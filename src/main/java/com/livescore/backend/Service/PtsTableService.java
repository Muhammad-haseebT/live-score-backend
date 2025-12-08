package com.livescore.backend.Service;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PtsTableService {
    @Autowired
    private PtsTableInterface ptsTableInterface;

    @Autowired
    private MatchInterface matchInterface;

    @Autowired
    private CricketInningsInterface inningsInterface;
    public ResponseEntity<?> createPtsTable(PtsTable ptsTable) {

        // ID user se aani hi nahi chahiye. Agar aa rahi hai to error feko
        if (ptsTable.getId() != null && ptsTableInterface.existsById(ptsTable.getId())) {
            return ResponseEntity.badRequest().body("PtsTable with id " + ptsTable.getId() + " already exists");
        }

        // Check team + tournament unique
        if (ptsTableInterface.existsByTeamIdAndTournamentId(
                ptsTable.getTeam().getId(),
                ptsTable.getTournament().getId()
        )) {
            return ResponseEntity.badRequest().body(
                    "PtsTable with team id " + ptsTable.getTeam().getId() +
                            " and tournament id " + ptsTable.getTournament().getId() +
                            " already exists"
            );
        }

        // Save
        ptsTableInterface.save(ptsTable);

        return ResponseEntity.ok("PtsTable created successfully");
    }


    public ResponseEntity<?> updatePointsTableAfterMatch(Long matchId) {

        if (matchId == null || matchId <= 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid matchId: must be positive");
        }

        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Match not found with id: " + matchId);
        }

        if (match.getWinnerTeam() == null) {
            return ResponseEntity.badRequest()
                    .body("Cannot update points: winner not set for matchId " + matchId);
        }

        Team winner = match.getWinnerTeam();

        Team loser;
        if (match.getTeam1() == null || match.getTeam2() == null) {
            return ResponseEntity.badRequest()
                    .body("Match is missing Team1 or Team2 data");
        }

        if (match.getTeam1().getId().equals(winner.getId())) {
            loser = match.getTeam2();
        } else {
            loser = match.getTeam1();
        }

        Long tournamentId = match.getTournament().getId();
        if (tournamentId == null) {
            return ResponseEntity.badRequest()
                    .body("Tournament not found for this match");
        }

        PtsTable ptsWinner = ptsTableInterface
                .findByTournamentIdAndTeamId(tournamentId, winner.getId());

        PtsTable ptsLoser = ptsTableInterface
                .findByTournamentIdAndTeamId(tournamentId, loser.getId());

        if (ptsWinner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("PtsTable not found for Winner Team ID: " + winner.getId());
        }

        if (ptsLoser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("PtsTable not found for Loser Team ID: " + loser.getId());
        }


        // --- UPDATE POINTS TABLE ---
        ptsWinner.setPlayed(ptsWinner.getPlayed() + 1);
        ptsLoser.setPlayed(ptsLoser.getPlayed() + 1);

        ptsWinner.setWins(ptsWinner.getWins() + 1);
        ptsLoser.setLosses(ptsLoser.getLosses() + 1);

        ptsWinner.setPoints(ptsWinner.getPoints() + 2);
        ptsLoser.setPoints(ptsLoser.getPoints() - 2);

        // NRR calculations
        double winnerNrr = calculateTeamNrr(winner.getId(), tournamentId);
        double loserNrr = calculateTeamNrr(loser.getId(), tournamentId);

        ptsWinner.setNrr(winnerNrr);
        ptsLoser.setNrr(loserNrr);

        ptsTableInterface.save(ptsWinner);
        ptsTableInterface.save(ptsLoser);


        // --- SUCCESS RESPONSE ---
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Points table updated successfully");
        response.put("winnerTeamId", winner.getId());
        response.put("loserTeamId", loser.getId());
        response.put("winnerPoints", ptsWinner.getPoints());
        response.put("loserPoints", ptsLoser.getPoints());
        response.put("winnerNRR", winnerNrr);
        response.put("loserNRR", loserNrr);

        return ResponseEntity.ok(response);
    }



    // -------------------------------
    // 2. CALCULATE NRR FOR TEAM
    // -------------------------------
    public double calculateTeamNrr(Long teamId, Long tournamentId) {

        List<CricketInnings> teamInnings =
                inningsInterface.findByTeamAndTournament(teamId, tournamentId);



        double totalRunsScored = 0;
        double totalOversFaced = 0;
        double totalRunsConceded = 0;
        double totalOversBowled = 0;

        for (CricketInnings inn : teamInnings) {

            // Team innings
            InningsData self = calculateInningsStats(inn);
            totalRunsScored += self.runs;
            totalOversFaced += self.overs;

            CricketInnings opponent =
                    inningsInterface.findOpponentInnings(
                            inn.getMatch().getId(), teamId
                    );

            if (opponent != null) {
                InningsData opp = calculateInningsStats(opponent);
                totalRunsConceded += opp.runs;
                totalOversBowled += opp.overs;
            }
        }

        if (totalOversFaced == 0 || totalOversBowled == 0) return 0;

        return (totalRunsScored / totalOversFaced) -
                (totalRunsConceded / totalOversBowled);
    }

    public ResponseEntity<?> deletePtsTable(Long id) {
        if(ptsTableInterface.existsById(id)){
            ptsTableInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    public ResponseEntity<?> getAllPtsTables() {
        return ResponseEntity.ok(ptsTableInterface.findAll());
    }
    public ResponseEntity<?> getPtsTableById(Long id) {
        return ptsTableInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> getPointsTableByTournamentIdAndTeamId(Long tournamentId, Long teamId) {
        return ResponseEntity.ok(ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, teamId));
    }


    public ResponseEntity<?> getPointsTableByTournamentId(Long tournamentId) {
        return ResponseEntity.ok(ptsTableInterface.findByTournamentId(tournamentId));
    }
    private static class InningsData {
        int runs;
        double overs;
    }

    private InningsData calculateInningsStats(CricketInnings innings) {

        List<CricketBall> balls = innings.getBalls();

        int totalRuns = 0;

        Map<Integer, Integer> legalBalls = new HashMap<>();

        for (CricketBall ball : balls) {

            // Runs
            totalRuns += ball.getRuns();
            if(ball.getExtra() != null){
                totalRuns += ball.getExtra();
            }

            // Legal vs Illegal balls
            if (ball.getExtraType() == null || isLegalDelivery(ball.getExtraType())) {
                legalBalls.merge(ball.getOverNumber(), 1, Integer::sum);
            }
        }

        int completedOvers = 0;
        int ballsInCurrentOver = 0;

        for (int count : legalBalls.values()) {
            if (count == 6) completedOvers++;
            else ballsInCurrentOver = count;
        }

        double overs = completedOvers + (ballsInCurrentOver / 6.0);

        InningsData data = new InningsData();
        data.runs = totalRuns;
        data.overs = overs;

        return data;
    }


    // -------------------------------
    // 4. LEGAL DELIVERY CHECK
    // -------------------------------
    private boolean isLegalDelivery(String extraType) {
        return !extraType.equals("WIDE") && !extraType.equals("NO_BALL");
    }
}

