package com.livescore.backend.Service;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiveSCoringService {
    @Autowired
    private CricketBallInterface cricketBallInterface;
    @Autowired
    private PlayerInterface playerRepo;
    @Autowired
    private CricketInningsInterface cricketInningsRepo;
    @Autowired
    private MatchInterface matchRepo;
    @Autowired
    private TeamInterface teamRepo;


    public ScoreDTO scoring(ScoreDTO s){


        Match m=matchRepo.findById(s.getMatchId()).orElseThrow();
        if(s.getOvers()==m.getOvers()&&s.getBalls()==6&&s.isFirstInnings()){
            s.setStatus("end");

        }
        else if(s.getOvers()==m.getOvers()&&s.getBalls()==6&&!s.isFirstInnings()){
            if(s.getTarget()==0){
                s.setStatus("draw");
            }
            else if(s.getTarget()<0){
                m.setWinnerTeam(teamRepo.findById(s.getTeamId()).orElseThrow());
            }else{
                m.setWinnerTeam(teamRepo.findById(m.getTeam1().getId()==s.getTeamId()?m.getTeam2().getId():m.getTeam1().getId()).orElse(null));
            }
            matchRepo.save(m);

            CricketInnings i=cricketInningsRepo.findByMatchIdAndNo(s.getMatchId(),1);
            List<CricketBall> balls = cricketBallInterface.findByMatch_IdAndInnings_Id(s.getMatchId(),i.getId());
            int runs = balls.stream().mapToInt(CricketBall::getRuns).sum();
            s.setTarget(runs);
        }

        if(s.getStatus().equalsIgnoreCase("end")){

            CricketInnings c=cricketInningsRepo.findByMatchIdAndNo(s.getMatchId(),1);
            List<CricketBall> balls = cricketBallInterface.findByMatch_IdAndInnings_Id(s.getMatchId(),c.getId());
            int runs = balls.stream().mapToInt(CricketBall::getRuns).sum();
            s.setTarget(runs);
            //empty innings create with same match id but innings n=2;
            CricketInnings innings = new CricketInnings();
            innings.setMatch(matchRepo.findById(s.getMatchId()).orElseThrow());
            innings.setNo(2);
            innings.setTeam(c.getTeam()==c.getMatch().getTeam1()?c.getMatch().getTeam2():c.getMatch().getTeam1());
            cricketInningsRepo.save(innings);
            s.setInningsId(innings.getId());
            s.setStatus("endFirst");
            s.setRuns(0);
            s.setOvers(0);
            s.setWickets(0);
            s.setBalls(0);
            s.setEvent("");
            s.setEventType("");
            s.setDismissalType("");
            s.setIsLegal(false);
            s.setFour(false);
            s.setSix(false);
            s.setComment("");
            s.setOutPlayerId(null);
            s.setMediaId(null);
            s.setFirstInnings(false);

            return s;
        }



        CricketBall ball = new CricketBall();
        ball.setInnings(cricketInningsRepo.findById(s.getInningsId()).orElseThrow());
        ball.setBatsman(playerRepo.findById(s.getBatsmanId()).orElse(null));
        ball.setBowler(playerRepo.findById(s.getBowlerId()).orElse(null));
        ball.setFielder(playerRepo.findById(s.getFielderId()).orElse(null));
        ball.setOverNumber(s.getOvers());
        ball.setBallNumber(s.getBalls());

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
        if (!s.isFirstInnings() && ball.getLegalDelivery()) {
            s.setTarget(s.getTarget() - (ball.getRuns() + ball.getExtra()));
        }

        cricketBallInterface.save(ball);

        return s;
    }
    private void incrementBall(ScoreDTO s) {
        s.setBalls(s.getBalls() + 1);

        if (s.getBalls() == 6) {
            s.setOvers(s.getOvers() + 1);
            s.setBalls(0);
        }
    }
}
