package com.livescore.backend.WebSocketController;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LiveScoring {
    @MessageMapping("/live-score")
    @SendTo("/topic/live-score")
    public String liveScore(String message) {
        return message;
    }
}
