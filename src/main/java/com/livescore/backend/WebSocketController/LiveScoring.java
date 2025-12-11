package com.livescore.backend.WebSocketController;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Service.LiveSCoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class LiveScoring {
    @Autowired
    private LiveSCoringService liveScoringService;



    @MessageMapping("/send")
    @SendTo("/topic/live")
    public ScoreDTO scoring(ScoreDTO s) {
            return liveScoringService.scoring(s);
    }


}


