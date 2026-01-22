package com.livescore.backend.WebSocketController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Service.LiveSCoringService;
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

    private final LiveSCoringService liveScoringService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(LiveScoringHandler.class);


    private final Map<Long, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();


    private final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();


    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public LiveScoringHandler(LiveSCoringService liveScoringService) {
        this.liveScoringService = liveScoringService;
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
                } catch (Exception ignored) {}
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

            // ASYNC PROCESSING
            executor.execute(() -> processScore(score));

        } catch (Exception e) {
            safeSend(session,
                    "{\"error\":\"Invalid JSON payload\"}");
        }
    }

    private void processScore(ScoreDTO score) {
        try {
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
                    try { s.close(); } catch (Exception ignore) {}
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
                unsubscribeAll(session);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unsubscribeAll(session);
    }
}
