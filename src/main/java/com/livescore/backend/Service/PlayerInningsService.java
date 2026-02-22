package com.livescore.backend.Service;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.MatchState;
import com.livescore.backend.Entity.PlayerInnings;
import com.livescore.backend.Interface.Cricket.MatchStateInterface;
import com.livescore.backend.Interface.Cricket.PlayerInningsInterface;
import com.livescore.backend.Interface.MatchInterface;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlayerInningsService {
    private final PlayerInningsInterface playerInningsInterface;
    private final MatchInterface matchInterface;
    private final MatchStateInterface matchStateInterface;


    public ResponseEntity<?> getScorecard(Long matchId, Long Team1Id) {
        Match m = matchInterface.findById(matchId).orElse(null);

        if (m == null) {
            return ResponseEntity.notFound().build();
        }
        MatchState matchState = matchStateInterface.findByTeam_Id(Team1Id, matchId);
        List<PlayerInnings> playerInningsList1 = playerInningsInterface.findByMatchId(matchId, Team1Id);
        Scorecard scorecard = new Scorecard();
        List<batsmanScore> batsmanScores = new ArrayList<>();
        List<bowlerScore> bowlerScores = new ArrayList<>();
        for (PlayerInnings playerInnings : playerInningsList1) {

            if(Objects.equals(playerInnings.getRole(), "Batsman")){
                batsmanScore bs = new batsmanScore();
                bs.setName(playerInnings.getPlayer().getName());
                bs.setRuns(playerInnings.getRuns());
                bs.setBalls(playerInnings.getBallsFaced());
                bs.setFours(playerInnings.getFour());
                bs.setSixes(playerInnings.getSixes());
                bs.setStrikeRate((double) playerInnings.getRuns() / (playerInnings.getBallsFaced() == 0 ? 1 : playerInnings.getBallsFaced()) * 100);
                batsmanScores.add(bs);
            }


            if(Objects.equals(playerInnings.getRole(), "BOWLER")){
                System.out.println("Bowler: " + playerInnings.getPlayer().getName() + ", Runs: " + playerInnings.getRuns() + ", Balls: " + playerInnings.getBallsFaced() + ", Wickets: " + playerInnings.getWickets());
                 bowlerScore bl = new bowlerScore();
                bl.setName(playerInnings.getPlayer().getName());
                bl.setOvers(playerInnings.getOvers() );
                bl.setEconomy((double) playerInnings.getRunsConceded() / (playerInnings.getBallsBowled() == 0 ? 1 : playerInnings.getBallsBowled()) * 6);
                bl.setRunsConceded(playerInnings.getRunsConceded());
                bl.setWickets(playerInnings.getWickets());
                bl.setBallsBowled(playerInnings.getBallsBowled());
                bowlerScores.add(bl);

            }

        }
        scorecard.setBatsmanScores(batsmanScores);
        scorecard.setBowlerScores(bowlerScores);
        if (matchState != null) {
            scorecard.setExtras(matchState.getExtras());
            scorecard.setTotalRuns(matchState.getRuns());
            scorecard.setOvers(matchState.getOvers());
            scorecard.setBalls(matchState.getBalls());
        } else {
            scorecard.setExtras(0);
            scorecard.setTotalRuns(0);
            scorecard.setOvers(0);
            scorecard.setBalls(0);
        }
        return ResponseEntity.ok(scorecard);

    }


    @Data
    static class Scorecard {

        private List<batsmanScore> batsmanScores = null;
        private List<bowlerScore> bowlerScores = null;
        private int Extras = 0;
        private int TotalRuns = 0;
        private int overs = 0;
        private int balls = 0;


    }

    @Data
    static class batsmanScore {
        private String name = "";
        private int runs = 0;
        private int balls = 0;
        private int fours = 0;
        private int sixes = 0;
        private double strikeRate = 0.0;
    }

    @Data
    static class bowlerScore {
        private String name = "";
        private int overs = 0;
        private double economy = 0.0;
        private int runsConceded = 0;
        private int wickets = 0;
        private int ballsBowled = 0;
    }

}
