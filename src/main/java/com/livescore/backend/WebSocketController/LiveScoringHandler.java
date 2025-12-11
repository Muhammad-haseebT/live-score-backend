package com.livescore.backend.WebSocketController;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Service.LiveSCoringService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
@Component
public class LiveScoringHandler extends TextWebSocketHandler {

    private final LiveSCoringService liveScoringService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    public LiveScoringHandler(LiveSCoringService liveScoringService) {
        this.liveScoringService = liveScoringService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ScoreDTO score = mapper.readValue(message.getPayload(), ScoreDTO.class);
        ScoreDTO updatedScore = liveScoringService.scoring(score);

        String json = mapper.writeValueAsString(updatedScore);
        synchronized (sessions) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
}
