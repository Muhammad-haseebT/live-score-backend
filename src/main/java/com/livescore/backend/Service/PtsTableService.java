package com.livescore.backend.Service;

import com.livescore.backend.DTO.PtsTableDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class PtsTableService {

    private final PtsTableInterface ptsTableInterface;


    private final MatchInterface matchInterface;


    private final CricketInningsInterface inningsInterface;

    public ResponseEntity<?> createPtsTable(PtsTable ptsTable) {


        if (ptsTable.getId() != null && ptsTableInterface.existsById(ptsTable.getId())) {
            return ResponseEntity.badRequest().body("PtsTable with id " + ptsTable.getId() + " already exists");
        }


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

    @Transactional
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

        Long tournamentId = match.getTournament() != null ? match.getTournament().getId() : null;
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
        ptsWinner.setPlayed(safeInc(ptsWinner.getPlayed()));
        ptsLoser.setPlayed(safeInc(ptsLoser.getPlayed()));

        ptsWinner.setWins(safeInc(ptsWinner.getWins()));
        ptsLoser.setLosses(safeInc(ptsLoser.getLosses()));


        ptsWinner.setPoints(safeAdd(ptsWinner.getPoints(), 2));
        ptsLoser.setPoints(safeAdd(ptsLoser.getPoints(), 0));


        // NRR calculations (match-wise)
        double winnerNrr = calculateTeamNrr(winner.getId(), tournamentId);
        double loserNrr = calculateTeamNrr(loser.getId(), tournamentId);

        ptsWinner.setNrr(winnerNrr);
        ptsLoser.setNrr(loserNrr);

        ptsTableInterface.save(ptsWinner);
        ptsTableInterface.save(ptsLoser);
        System.out.println(ptsWinner);
        System.out.println(ptsLoser);

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



    public double calculateTeamNrr(Long teamId, Long tournamentId) {

        if (teamId == null || tournamentId == null) return 0.0;

        List<Match> matches =
                matchInterface.findCompletedMatchesByTeam(teamId, tournamentId);

        if (matches == null || matches.isEmpty()) return 0.0;

        long runsScored = 0L;
        double oversFaced = 0.0;
        long runsConceded = 0L;
        double oversBowled = 0.0;

        for (Match match : matches) {
            if (match == null) continue;

            CricketInnings myInnings =
                    inningsInterface.findByMatchAndTeam(match.getId(), teamId);

            CricketInnings oppInnings =
                    inningsInterface.findOpponentInnings(match.getId(), teamId);

            if (myInnings == null || oppInnings == null) {
                // skip incomplete records
                continue;
            }

            InningsData self = calculateInningsStats(myInnings);
            InningsData opp = calculateInningsStats(oppInnings);

            runsScored += self.runs;
            oversFaced += self.overs;

            runsConceded += opp.runs;
            oversBowled += opp.overs;
        }

        if (oversFaced <= 0.0 || oversBowled <= 0.0) return 0.0;

        double nrr = ((double) runsScored / oversFaced) - ((double) runsConceded / oversBowled);

        return roundToThreeDecimals(nrr);
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

    public ResponseEntity<?> getPtsTablesByTournament(Long tournamentId) {
        List<PtsTable> p=ptsTableInterface.findByTournamentId(tournamentId);
        List<PtsTableDTO> ptDtoList=p.stream().map(pt->{
            PtsTableDTO dto=new PtsTableDTO();
            dto.setTeamName(pt.getTeam().getName());
            dto.setId(pt.getId());
            dto.setTournamentId(pt.getTournament().getId());
            dto.setTeamId(pt.getTeam().getId());
            dto.setPlayed(pt.getPlayed());
            dto.setWins(pt.getWins());
            dto.setLosses(pt.getLosses());
            dto.setPoints(pt.getPoints());
            dto.setNrr(pt.getNrr());
            return dto;
        }).toList();
        return ResponseEntity.ok(ptDtoList);
    }


    // changed InningsData to use long runs for safety
    private static class InningsData {
        long runs;
        double overs;
        public InningsData() {}
        public InningsData(long runs, double overs) {
            this.runs = runs;
            this.overs = overs;
        }
    }


    private InningsData calculateInningsStats(CricketInnings innings) {
        if (innings == null) return new InningsData(0L, 0.0);

        List<CricketBall> balls = innings.getBalls();
        if (balls == null || balls.isEmpty()) return new InningsData(0L, 0.0);

        long totalRuns = 0L;
        long legalBallCount = 0L;

        for (CricketBall ball : balls) {
            if (ball == null) continue;


            int ballRuns = safeInt(ball.getRuns());
            int extra = safeInt(ball.getExtra());
            totalRuns += (ballRuns + extra);


            String extraType = ball.getExtraType();
            if (isLegalDelivery(extraType)) {
                legalBallCount++;
            }
        }

        double overs = convertBallsToOvers(legalBallCount);
        return new InningsData(totalRuns, overs);
    }



    private boolean isLegalDelivery(String extraType) {
        if (extraType == null) return true;
        String t = extraType.trim().toUpperCase();
        return !(t.equals("WIDE") || t.equals("NO_BALL"));
    }

    // Convert number of legal balls to overs decimal (e.g., 88 -> 14.6666667)
    private double convertBallsToOvers(long legalBalls) {
        long completeOvers = legalBalls / 6;
        long remainingBalls = legalBalls % 6;
        return completeOvers + (remainingBalls / 6.0);
    }

    private double roundToThreeDecimals(double v) {

        return  Math.round(v*1000.0)/1000.0;
    }


    // small safe helpers
    private int safeInc(Integer x) {
        return (x == null) ? 1 : x + 1;
    }
    private int safeAdd(Integer x, int add) {
        return (x == null) ? add : x + add;
    }
    private int safeInt(Integer x) {
        return x == null ? 0 : x;
    }
}
