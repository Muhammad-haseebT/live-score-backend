package com.livescore.backend.WebSocketController;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LiveScoring {

    @MessageMapping("/send")                // client SENDS to: /app/send
    @SendTo("/topic/live")                 // client SUBSCRIBES to: /topic/live
    public String broadcast(String msg) {
        return msg;
    }
}

