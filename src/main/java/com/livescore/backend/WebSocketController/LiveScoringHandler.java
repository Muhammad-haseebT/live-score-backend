package com.livescore.backend.WebSocketController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Service.LiveSCoringService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveScoringHandler extends TextWebSocketHandler {

    private final LiveSCoringService liveScoringService;
    private final ObjectMapper mapper = new ObjectMapper();

    // matchId -> sessions subscribed to that match
    private final Map<Long, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();

    // map session -> set of subscribed matchIds (for cleanup)
    private final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();

    public LiveScoringHandler(LiveSCoringService liveScoringService) {
        this.liveScoringService = liveScoringService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // try to read matchId from query param: ws://host/ws?matchId=123
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            String[] qs = uri.getQuery().split("&");
            for (String q : qs) {
                if (q.startsWith("matchId=")) {
                    try {
                        Long matchId = Long.parseLong(q.split("=")[1]);
                        subscribe(session, matchId);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private void subscribe(WebSocketSession session, Long matchId) {
        subscriptions.computeIfAbsent(matchId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);
        sessionSubscriptions.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet()).add(matchId);
    }

    private void unsubscribeAll(WebSocketSession session) {
        Set<Long> matches = sessionSubscriptions.remove(session.getId());
        if (matches != null) {
            for (Long m : matches) {
                Set<WebSocketSession> set = subscriptions.get(m);
                if (set != null) {
                    set.remove(session);
                    if (set.isEmpty()) subscriptions.remove(m);
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = mapper.readTree(message.getPayload());
        // allow a subscribe message
        if (node.has("action") && node.get("action").asText().equalsIgnoreCase("subscribe") && node.has("matchId")) {
            Long matchId = node.get("matchId").asLong();
            subscribe(session, matchId);
            // ack
            session.sendMessage(new TextMessage("{\"status\":\"subscribed\",\"matchId\":" + matchId + "}"));
            return;
        }

        // otherwise treat message as scoring update
        ScoreDTO score = mapper.treeToValue(node, ScoreDTO.class);
        if (score.getMatchId() == null) {
            session.sendMessage(new TextMessage("{\"error\":\"matchId required in scoring message\"}"));
            return;
        }

        ScoreDTO updated = liveScoringService.scoring(score);
        String json = mapper.writeValueAsString(updated);

        // broadcast only to subscribers of this matchId
        Set<WebSocketSession> subs = subscriptions.get(score.getMatchId());
        if (subs != null) {
            for (WebSocketSession s : subs) {
                try {
                    if (s.isOpen()) s.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    // remove broken session
                    unsubscribeAll(s);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unsubscribeAll(session);
    }
}
