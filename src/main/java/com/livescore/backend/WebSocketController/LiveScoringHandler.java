package com.livescore.backend.WebSocketController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.Cricket.CricketScoringService;
import com.livescore.backend.DTO.ScoreDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Component
public class LiveScoringHandler extends TextWebSocketHandler {

    private final CricketScoringService liveScoringService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(LiveScoringHandler.class);

    // Map to track which sessions are subscribed to which matches
    private final Map<Long, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();

    // Map to track which matches each session is subscribed to
    private final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();

    // Locks for thread-safe session operations
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    // Thread pool for async operations
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // ✅ Constructor updated - removed MatchStateCache dependency
    public LiveScoringHandler(CricketScoringService s) {
        this.liveScoringService = s;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return;

        for (String q : uri.getQuery().split("&")) {
            if (q.startsWith("matchId=")) {
                try {
                    Long matchId = Long.parseLong(q.split("=")[1]);
                    subscribe(session, matchId);
                } catch (Exception ignored) {
                    log.warn("Invalid matchId parameter in WebSocket connection");
                }
            }
        }
    }

    private void subscribe(WebSocketSession session, Long matchId) {
        if (matchId == null) return;

        subscriptions
                .computeIfAbsent(matchId,
                        k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);

        sessionSubscriptions
                .computeIfAbsent(session.getId(),
                        k -> ConcurrentHashMap.newKeySet())
                .add(matchId);

        sessionLocks.computeIfAbsent(session.getId(), k -> new Object());

        // Send current match state to newly connected client
        executor.execute(() -> sendCurrentMatchState(session, matchId));
    }

    // ✅ Simplified - Spring Cache automatically handles caching
    private void sendCurrentMatchState(WebSocketSession session, Long matchId) {
        try {
            // Direct service call - @Cacheable will handle cache lookup
            ScoreDTO currentState = liveScoringService.getCurrentMatchState(matchId);

            String json = mapper.writeValueAsString(currentState);
            safeSend(session, json);
        } catch (Exception e) {
            log.warn("Failed to send current match state to client", e);
        }
    }

    private void unsubscribeAll(WebSocketSession session) {
        Set<Long> matches = sessionSubscriptions.remove(session.getId());

        if (matches != null) {
            for (Long m : matches) {
                Set<WebSocketSession> set = subscriptions.get(m);
                if (set != null) {
                    set.remove(session);
                    if (set.isEmpty()) {
                        subscriptions.remove(m);
                    }
                }
            }
        }

        sessionLocks.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());

            // SUBSCRIBE MESSAGE
            if (node.has("action")
                    && "subscribe".equalsIgnoreCase(node.get("action").asText())
                    && node.has("matchId")) {

                Long matchId = node.get("matchId").asLong();
                subscribe(session, matchId);

                safeSend(session,
                        "{\"status\":\"subscribed\",\"matchId\":" + matchId + "}");
                return;
            }

            ScoreDTO score = mapper.treeToValue(node, ScoreDTO.class);

            if (score.getMatchId() == null) {
                safeSend(session,
                        "{\"error\":\"matchId is required\"}");
                return;
            }

            // Check if this is an UNDO request
            if (score.isUndo()) {
                if (score.getInningsId() == null) {
                    safeSend(session,
                            "{\"error\":\"inningsId is required for undo\"}");
                    return;
                }
                executor.execute(() -> processUndo(score.getMatchId(), score.getInningsId()));
                return;
            }

            // ASYNC PROCESSING
            executor.execute(() -> processScore(score));

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            safeSend(session,
                    "{\"error\":\"Invalid JSON payload\"}");
        }
    }

    // ✅ Simplified - @CacheEvict automatically clears cache
    private void processUndo(Long matchId, Long inningsId) {
        try {
            // Service method has @CacheEvict, so cache is automatically cleared
            ScoreDTO updated = liveScoringService.undoLastBall(matchId, inningsId);

            broadcast(updated);
        } catch (Exception e) {
            log.warn("Failed to process undo", e);
        }
    }

    // ✅ Simplified - @CachePut automatically updates cache
    private void processScore(ScoreDTO score) {
        try {
            // Service method has @CachePut, so cache is automatically updated
            ScoreDTO updated = liveScoringService.scoring(score);

            broadcast(updated);
        } catch (Exception e) {
            log.warn("Failed to process score", e);
        }
    }

    private void broadcast(ScoreDTO updated) {
        try {
            String json = mapper.writeValueAsString(updated);
            Set<WebSocketSession> subs = subscriptions.get(updated.getMatchId());

            if (subs == null) return;

            for (WebSocketSession s : subs) {
                try {
                    safeSend(s, json);
                } catch (Exception ex) {
                    unsubscribeAll(s);
                    try {
                        s.close();
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast score update", e);
        }
    }

    private void safeSend(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) return;

        Object lock = sessionLocks.computeIfAbsent(
                session.getId(), k -> new Object());

        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception e) {
                log.warn("Error sending message to WebSocket session", e);
                unsubscribeAll(session);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.debug("WebSocket connection closed: {}", status);
        unsubscribeAll(session);
    }
}

