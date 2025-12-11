package com.livescore.backend.WebSocketController;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
@Controller
public class LiveScoring {

    @Autowired
    private CricketBallInterface cricketBallInterface;
    @Autowired
    private PlayerInterface playerRepo;
    @Autowired
    private CricketInningsInterface cricketInningsRepo;

    @MessageMapping("/send")
    @SendTo("/topic/live")
    public ScoreDTO scoring(ScoreDTO s) {

        CricketBall ball = new CricketBall();
        ball.setInnings(cricketInningsRepo.findById(s.getInningsId()).orElseThrow());
        ball.setBatsman(playerRepo.findById(s.getBatsmanId()).orElse(null));
        ball.setBowler(playerRepo.findById(s.getBowlerId()).orElse(null));
        ball.setFielder(playerRepo.findById(s.getFielderId()).orElse(null));
        ball.setOverNumber(s.getOvers().intValue());
        ball.setBallNumber(s.getBalls().intValue());

        switch (s.getEventType().toLowerCase()) {

            case "run":
                int r = Integer.parseInt(s.getEvent());
                ball.setRuns(r);
                ball.setExtra(0);
                ball.setExtraType(null);
                ball.setLegalDelivery(true);
                s.setRuns(s.getRuns() + r);
                incrementBall(s);
                break;

            case "boundary":
                int b = Integer.parseInt(s.getEvent()); // 4 or 6
                ball.setRuns(b);
                ball.setExtra(0);
                ball.setExtraType(null);
                ball.setLegalDelivery(true);
                s.setRuns(s.getRuns() + b);
                if(b==4){
                    ball.setIsFour(true);
                }
                if(b==6){
                    ball.setIsSix(true);
                }
                incrementBall(s);
                break;

            case "wide":
                int w = Integer.parseInt(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(w + 1);  // 1 wide + runs if any
                ball.setExtraType("wide");
                ball.setLegalDelivery(false); // does NOT increment ball/over
                ball.setExtra(s.getExtra() + w + 1);
                break;

            case "noball":
                int nb = Integer.parseInt(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(nb + 1);
                ball.setExtraType("noball");
                ball.setLegalDelivery(false);
                ball.setExtra(s.getExtra() + nb + 1);
                break;

            case "bye":
                int by = Integer.parseInt(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(by);
                ball.setExtraType("bye");
                ball.setLegalDelivery(true);
                incrementBall(s);
                s.setExtra(s.getExtra() + by);
                break;

            case "legbye":
                int lb = Integer.parseInt(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(lb);
                ball.setExtraType("legbye");
                ball.setLegalDelivery(true);
                incrementBall(s);
                s.setExtra(s.getExtra() + lb);
                break;

            case "wicket":

                String dismissal = s.getEvent();
                ball.setDismissalType(dismissal);


                ball.setOutPlayer(playerRepo.findById(s.getOutPlayerId()).orElse(ball.getBatsman()));


                int runsBeforeOut = s.getRunsOnThisBall();
                ball.setRuns(runsBeforeOut);
                ball.setExtra(s.getExtrasThisBall());


                ball.setLegalDelivery(true);

                // Fielder if applicable
                if (dismissal.equalsIgnoreCase("caught") || dismissal.equalsIgnoreCase("runout") || dismissal.equalsIgnoreCase("stumped")) {
                    ball.setFielder(playerRepo.findById(s.getFielderId()).orElse(null));
                }

                incrementBall(s);  // count the ball
                s.setRuns(s.getRuns() + runsBeforeOut);  // add runs
                s.setWickets(s.getWickets() + 1);
                break;


            default:
                throw new RuntimeException("Invalid event type");
        }

        // Update target for 2nd innings
        if (s.getInningsId() != 1 && ball.getLegalDelivery()) {
            s.setTarget(s.getTarget() - (ball.getRuns() + ball.getExtra()));
        }

        cricketBallInterface.save(ball);

        return s;
    }

    private void incrementBall(ScoreDTO s) {
        s.setBalls(s.getBalls() + 1);

        if (s.getBalls() == 6) {
            s.setOvers(s.getOvers() + 1);
            s.setBalls(0L);
        }
    }
}


