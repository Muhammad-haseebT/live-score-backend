package com.livescore.backend.Cricket;

import com.livescore.backend.DTO.PlayerStatDTO;
import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.CricketInningsInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Service.AwardService;
import com.livescore.backend.Service.MatchService;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CricketStateCalculator {
    @Autowired
    private CricketBallInterface ballRepo;
    @Autowired
    private CricketInningsInterface inningsRepo;
    @Autowired
    private MatchInterface matchRepo;
    @Autowired
    private CricketInningsManager inningsManager;
    @Autowired
    private AwardService awardService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private PlayerInterface playerRepo;

    /**
     * Main method: Calculate current match state
     * Checks for innings end, all-out, target reached, etc.
     */
    public ScoreDTO calculateState(ScoreDTO s, Match match, CricketInnings innings) {
        // ✅ Store original ball number (jo autoIncrement ne set kiya)
        int requestedOver = s.getOvers();
        int requestedBall = s.getBalls();

        // Update current innings stats (runs, wickets, CRR)
        updateInningsStats(s, innings);

        // ✅ Restore requested ball number (don't overwrite with DB calculation)
        s.setOvers(requestedOver);
        s.setBalls(requestedBall);

        // Check for all-out
        if (inningsManager.shouldEndInnings(s, innings)) {
            return inningsManager.handleInningsEnd(s, match, innings);
        }

        // Handle second innings logic
        if (!s.isFirstInnings()) {
            return handleSecondInnings(s, match, innings);
        } else {
            s.setTarget(s.getRuns());
        }
        populatePlayerStats(s, innings);

        return s;
    }


    /**
     * Updates innings statistics: runs, overs, wickets, CRR
     */
    private void updateInningsStats(ScoreDTO s, CricketInnings innings) {
        int runs = ballRepo.sumRunsAndExtrasByInningsId(innings.getId());
        int wickets = (int) ballRepo.countWicketsByInningsId(innings.getId());

        s.setRuns(runs);
        s.setWickets(wickets);

        // ✅ Calculate CRR using CURRENT ball count (not recalculated from DB)
        int totalBalls = s.getOvers() * Constants.BALLS_PER_OVER + s.getBalls();
        double crr = totalBalls == 0 ? 0.0 :
                ((double) runs * Constants.BALLS_PER_OVER) / (double) totalBalls;
        s.setCrr(round2(crr));
    }

    /**
     * Handles second innings logic: target, RRR, win conditions
     */
    private ScoreDTO handleSecondInnings(ScoreDTO s, Match match, CricketInnings currentInnings) {
        // Get first innings runs
        CricketInnings firstInnings = inningsRepo.findByMatchIdAndNo(
                match.getId(), Constants.FIRST_INNINGS);

        int firstRuns = ballRepo.sumRunsAndExtrasByInningsId(firstInnings.getId());
        int target = firstRuns + 1;
        int currentRuns = s.getRuns();
        int remainingRuns = target - currentRuns;

        // CHECK 1: Target achieved? (chasing team jeeti!)
        if (currentRuns >= target) {
            return endMatchWithWinner(s, match, currentInnings);
        }

        // CHECK 2: Calculate remaining balls
        int maxBalls = match.getOvers() * Constants.BALLS_PER_OVER;
        int legalBalls = s.getOvers() * Constants.BALLS_PER_OVER + s.getBalls();
        int remainingBalls = Math.max(0, maxBalls - legalBalls);

        // CHECK 3: Overs khatam? Determine winner by runs
        if (remainingBalls == 0) {
            return endMatchByOvers(s, match, currentInnings, currentRuns, firstRuns);
        }

        // Calculate Required Run Rate (RRR)
        double rrr = remainingBalls > 0 ?
                ((double) remainingRuns * Constants.BALLS_PER_OVER) / (double) remainingBalls :
                Double.POSITIVE_INFINITY;

        s.setTarget(remainingRuns);
        s.setRrr(round2(rrr));

        return s;
    }

    /**
     * Ends match when target is reached
     */
    private ScoreDTO endMatchWithWinner(ScoreDTO s, Match match, CricketInnings innings) {
        if (!isMatchFinal(match)) {
            match.setWinnerTeam(innings.getTeam());
            match.setStatus(Constants.STATUS_COMPLETED);
            matchRepo.save(match);
            awardService.computeMatchAwards(match.getId());
            matchService.endMatch(match.getId());
        }

        s.setStatus(Constants.STATUS_END_MATCH);
        s.setTarget(0);
        s.setRrr(0.0);
        return s;
    }

    /**
     * Ends match when overs are complete
     */
    private ScoreDTO endMatchByOvers(ScoreDTO s, Match match, CricketInnings currentInnings,
                                     int currentRuns, int firstRuns) {
        Team otherTeam = getOpposingTeam(match, currentInnings.getTeam());

        // Determine winner
        if (currentRuns > firstRuns) {
            match.setWinnerTeam(currentInnings.getTeam());
        } else if (currentRuns < firstRuns) {
            match.setWinnerTeam(otherTeam);
        } else {
            match.setWinnerTeam(null);
            match.setStatus(Constants.STATUS_TIED);
        }

        if (!isMatchFinal(match)) {
            if (!Constants.STATUS_TIED.equalsIgnoreCase(match.getStatus())) {
                match.setStatus(Constants.STATUS_COMPLETED);
            }
            matchRepo.save(match);
            awardService.computeMatchAwards(match.getId());
            matchService.endMatch(match.getId());
        }

        s.setStatus(Constants.STATUS_END_MATCH);
        s.setTarget(Math.max(0, (firstRuns + 1) - currentRuns));
        return s;
    }

    /**
     * Gets current match state (for WebSocket/API calls)
     */
    public ScoreDTO getCurrentState(Long matchId) {
        ScoreDTO state = new ScoreDTO();
        Match match = matchRepo.findById(matchId).orElse(null);

        if (match == null) {
            state.setStatus(Constants.STATUS_ERROR);
            state.setComment(Constants.ERROR_MATCH_NOT_FOUND);
            return state;
        }

        state.setMatchId(matchId);

        // Determine current innings (second ya first?)
        CricketInnings secondInnings = inningsRepo.findByMatchIdAndNo(matchId, Constants.SECOND_INNINGS);
        CricketInnings currentInnings;
        boolean isFirstInnings;

        if (secondInnings != null && secondInnings.getId() != null) {
            currentInnings = secondInnings;
            isFirstInnings = false;
        } else {
            currentInnings = inningsRepo.findByMatchIdAndNo(matchId, Constants.FIRST_INNINGS);
            isFirstInnings = true;
        }

        if (currentInnings == null) {
            state.setStatus(Constants.STATUS_ERROR);
            state.setComment(Constants.ERROR_INNINGS_NOT_FOUND);
            return state;
        }

        state.setInningsId(currentInnings.getId());
        state.setFirstInnings(isFirstInnings);
        state.setTeamId(currentInnings.getTeam() != null ? currentInnings.getTeam().getId() : null);

        // ✅ Get last ball for batsman/bowler/fielder info
        CricketBall lastBall = ballRepo.findFirstByInnings_IdOrderByIdDesc(currentInnings.getId());

        if (lastBall != null) {
            // ✅ Set current batsman/bowler/fielder
            state.setBatsmanId(lastBall.getBatsman() != null ? lastBall.getBatsman().getId() : null);
            state.setNonStrikerId(lastBall.getNonStriker() != null ? lastBall.getNonStriker().getId() : null);  // ✅ NEW
            state.setBowlerId(lastBall.getBowler() != null ? lastBall.getBowler().getId() : null);
            state.setFielderId(lastBall.getFielder() != null ? lastBall.getFielder().getId() : null);

            // ✅ Set last ball details for frontend context
            state.setEvent(String.valueOf(lastBall.getRuns() != null ? lastBall.getRuns() : 0));
            state.setEventType(lastBall.getExtraType() != null ? lastBall.getExtraType() : "run");
            state.setIsLegal(lastBall.getLegalDelivery());
            state.setFour(lastBall.getIsFour() != null && lastBall.getIsFour());
            state.setSix(lastBall.getIsSix() != null && lastBall.getIsSix());
            state.setDismissalType(lastBall.getDismissalType());
            state.setOutPlayerId(lastBall.getOutPlayer() != null ? lastBall.getOutPlayer().getId() : null);
            state.setComment(lastBall.getComment());
            state.setMediaId(lastBall.getMedia() != null ? lastBall.getMedia().getId() : null);
            state.setBalls(lastBall.getBallNumber());
            state.setOvers(lastBall.getOverNumber());
            state.setWickets((int) ballRepo.countWicketsByInningsId(currentInnings.getId()));
        } else {
            // No balls yet - set defaults
            state.setBatsmanId(null);
            state.setNonStrikerId(null);  // ✅ NEW
            state.setBowlerId(null);
            state.setFielderId(null);
            state.setEvent(null);
            state.setEventType(null);
            state.setIsLegal(null);
            state.setFour(false);
            state.setSix(false);
        }

        // Calculate current stats (runs, overs, wickets, CRR)
        updateInningsStats(state, currentInnings);

        // Calculate target and RRR for second innings
        if (!isFirstInnings) {
            CricketInnings firstInnings = inningsRepo.findByMatchIdAndNo(matchId, Constants.FIRST_INNINGS);
            int firstRuns = ballRepo.sumRunsAndExtrasByInningsId(firstInnings.getId());
            int target = firstRuns + 1;
            int remainingRuns = target - state.getRuns();

            int maxBalls = match.getOvers() * Constants.BALLS_PER_OVER;
            int legalBalls = state.getOvers() * Constants.BALLS_PER_OVER + state.getBalls();
            int remainingBalls = Math.max(0, maxBalls - legalBalls);

            state.setTarget(remainingRuns);
            if (remainingBalls > 0) {
                double rrr = ((double) remainingRuns * Constants.BALLS_PER_OVER) / (double) remainingBalls;
                state.setRrr(round2(rrr));
            } else {
                state.setRrr(0.0);
            }
        } else {
            state.setTarget(state.getRuns());
            state.setRrr(0.0);
        }

        // Set match status
        state.setStatus(isMatchFinal(match) ? Constants.STATUS_END_MATCH : "LIVE");

        // ✅ Populate player statistics (NEW - uses batsmanId, nonStrikerId, bowlerId from above)
        populatePlayerStats(state, currentInnings);

        // ✅ Set undo flag to false (this is current state, not undo)
        state.setUndo(false);

        return state;
    }

    // Helper methods
    private Team getOpposingTeam(Match match, Team team) {
        if (team == null || match.getTeam1() == null || match.getTeam2() == null) {
            return null;
        }
        return match.getTeam1().getId().equals(team.getId()) ?
                match.getTeam2() : match.getTeam1();
    }

    private boolean isMatchFinal(Match m) {
        if (m == null || m.getStatus() == null) return false;
        String st = m.getStatus().trim().toUpperCase();
        return st.equals("COMPLETED") || st.equals("FINISHED") ||
                st.equals("ABANDONED") || st.equals("TIED");
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }


    private void populatePlayerStats(ScoreDTO s, CricketInnings innings) {
        // ✅ Use frontend-provided IDs directly
        Long strikerId = s.getBatsmanId();
        Long nonStrikerId = s.getNonStrikerId();
        Long bowlerId = s.getBowlerId();

        // Populate striker stats (batsman1)
        if (strikerId != null) {
            PlayerStatDTO batsman1 = calculateBatsmanStats(strikerId, innings.getId());
            s.setBatsman1Stats(batsman1);
        }

        // Populate non-striker stats (batsman2)
        if (nonStrikerId != null) {
            PlayerStatDTO batsman2 = calculateBatsmanStats(nonStrikerId, innings.getId());
            s.setBatsman2Stats(batsman2);
        }

        // Populate bowler stats
        if (bowlerId != null) {
            PlayerStatDTO bowler = calculateBowlerStats(bowlerId, innings.getId());
            s.setBowlerStats(bowler);
        }
    }

    private PlayerStatDTO calculateBatsmanStats(Long playerId, Long inningsId) {
        PlayerStatDTO stats = new PlayerStatDTO();

        // Set player ID and name
        stats.setPlayerId(playerId);
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player != null) {
            stats.setPlayerName(player.getName());
        }

        // Query balls faced by this batsman
        List<CricketBall> balls = ballRepo.findByBatsman_IdAndInnings_Id(playerId, inningsId);

        int runs = 0;
        int ballsFaced = 0;
        int fours = 0;
        int sixes = 0;

        for (CricketBall ball : balls) {
            // Count legal deliveries as balls faced
            if (ball.getLegalDelivery() != null && ball.getLegalDelivery()) {
                ballsFaced++;
            }

            // Sum runs (not extras)
            if (ball.getRuns() != null) {
                runs += ball.getRuns();
            }

            // Count boundaries
            if (ball.getIsFour() != null && ball.getIsFour()) {
                fours++;
            }

            if (ball.getIsSix() != null && ball.getIsSix()) {
                sixes++;
            }
        }

        stats.setRuns(runs);
        stats.setBallsFaced(ballsFaced);
        stats.setFours(fours);
        stats.setSixes(sixes);

        return stats;
    }

    /**
     * ✅ NEW METHOD: Calculate bowling statistics for a player
     */
    private PlayerStatDTO calculateBowlerStats(Long playerId, Long inningsId) {
        PlayerStatDTO stats = new PlayerStatDTO();

        // Set player ID and name
        stats.setPlayerId(playerId);
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player != null) {
            stats.setPlayerName(player.getName());
        }

        // Query balls bowled by this bowler
        List<CricketBall> balls = ballRepo.findByBowler_IdAndInnings_Id(playerId, inningsId);

        int wickets = 0;
        int runsConceded = 0;
        int ballsBowled = 0;

        for (CricketBall ball : balls) {
            // Count legal deliveries as balls bowled
            if (ball.getLegalDelivery() != null && ball.getLegalDelivery()) {
                ballsBowled++;
            }

            // Sum runs conceded (batsman runs)
            if (ball.getRuns() != null) {
                runsConceded += ball.getRuns();
            }

            // Sum extras conceded (wides, no-balls, byes, leg-byes)
            if (ball.getExtra() != null) {
                runsConceded += ball.getExtra();
            }

            // Count wickets (exclude run-outs - those don't count for bowler)
            if (ball.getDismissalType() != null && !ball.getDismissalType().isEmpty()) {
                String dismissal = ball.getDismissalType().toLowerCase();
                // Run-outs don't count as bowler's wicket
                if (!dismissal.contains("runout") && !dismissal.contains("run out")) {
                    wickets++;
                }
            }
        }

        stats.setWickets(wickets);
        stats.setRunsConceded(runsConceded);
        stats.setBallsBowled(ballsBowled);

        // Calculate economy rate (runs per over)
        if (ballsBowled > 0) {
            double overs = (double) ballsBowled / Constants.BALLS_PER_OVER;
            stats.setEconomy(round2(runsConceded / overs));
        } else {
            stats.setEconomy(null);  // No balls bowled yet
        }

        // Calculate bowling average (runs conceded per wicket)
        if (wickets > 0) {
            stats.setBowlingAverage(round2((double) runsConceded / wickets));
        } else {
            stats.setBowlingAverage(null);  // No wickets yet
        }

        return stats;
    }
}
