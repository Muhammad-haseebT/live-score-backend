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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // ✅ FIXED: Innings-level tracking (NOT instance variables)
    private final Map<Long, Long[]> inningsOriginalBatsmen = new ConcurrentHashMap<>();

    /**
     * Main method: Calculate current match state
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
            handleSecondInnings(s, match, innings);
        } else {
            s.setTarget(s.getRuns());
        }

        // ✅ ALWAYS populate player stats LAST (after all logic)
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

        // ✅ Calculate CRR using CURRENT ball count
        int totalBalls = s.getOvers() * Constants.BALLS_PER_OVER + s.getBalls();
        double crr = totalBalls == 0 ? 0.0 :
                ((double) runs * Constants.BALLS_PER_OVER) / (double) totalBalls;
        s.setCrr(round2(crr));
    }

    /**
     * Handles second innings logic: target, RRR, win conditions
     */
    private void handleSecondInnings(ScoreDTO s, Match match, CricketInnings innings) {
        CricketInnings firstInnings = inningsRepo.findByMatchIdAndNo(match.getId(), Constants.FIRST_INNINGS);
        int firstRuns = ballRepo.sumRunsAndExtrasByInningsId(firstInnings.getId());
        int target = firstRuns + 1;
        int currentRuns = s.getRuns();
        int remainingRuns = target - currentRuns;

        // Target achieved
        if (currentRuns >= target) {
            endMatchWithWinner(s, match, innings);
            return;
        }

        // Calculate RRR
        int maxBalls = match.getOvers() * Constants.BALLS_PER_OVER;
        int legalBalls = s.getOvers() * Constants.BALLS_PER_OVER + s.getBalls();
        int remainingBalls = Math.max(0, maxBalls - legalBalls);

        double rrr = remainingBalls > 0 ?
                ((double) remainingRuns * Constants.BALLS_PER_OVER) / (double) remainingBalls :
                Double.POSITIVE_INFINITY;

        s.setTarget(remainingRuns);
        s.setRrr(round2(rrr));
    }

    private void endMatchWithWinner(ScoreDTO s, Match match, CricketInnings innings) {
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
    }

    /**
     * ✅ FIXED: ALWAYS populate player stats with error handling
     */
    private void populatePlayerStats(ScoreDTO s, CricketInnings innings) {
        Long inningsId = innings.getId();
        Long currentStrikerId = s.getBatsmanId();
        Long currentNonStrikerId = s.getNonStrikerId();
        Long bowlerId = s.getBowlerId();

        System.out.println("=== POPULATE STATS DEBUG ===");
        System.out.println("Innings ID: " + inningsId);
        System.out.println("Striker: " + currentStrikerId + ", Non-Striker: " + currentNonStrikerId);

        // ✅ ENSURE original batting order exists
        if (!inningsOriginalBatsmen.containsKey(inningsId) && currentStrikerId != null && currentNonStrikerId != null) {
            inningsOriginalBatsmen.put(inningsId, new Long[]{currentStrikerId, currentNonStrikerId});
            System.out.println("✅ SET original order: Bat1=" + currentStrikerId + ", Bat2=" + currentNonStrikerId);
        }

        Long[] originalOrder = inningsOriginalBatsmen.get(inningsId);
        if (originalOrder == null) {
            System.err.println("❌ No original order found for innings: " + inningsId);
            return;
        }

        Long originalBatsman1Id = originalOrder[0];
        Long originalBatsman2Id = originalOrder[1];

        System.out.println("Original Bat1 ID: " + originalBatsman1Id);
        System.out.println("Original Bat2 ID: " + originalBatsman2Id);

        // ✅ TOP POSITION - Always populate
        try {
            PlayerStatDTO batsman1Stats = calculateBatsmanStats(originalBatsman1Id, inningsId);
            s.setBatsman1Stats(batsman1Stats);
            System.out.println("✅ Batsman1Stats: " + batsman1Stats.getPlayerName() + " - " + batsman1Stats.getRuns() + " runs");
        } catch (Exception e) {
            System.err.println("❌ Batsman1Stats failed: " + e.getMessage());
            s.setBatsman1Stats(createEmptyStats(originalBatsman1Id, "Batsman 1"));
        }

        // ✅ BOTTOM POSITION - Always populate
        try {
            PlayerStatDTO batsman2Stats = calculateBatsmanStats(originalBatsman2Id, inningsId);
            s.setBatsman2Stats(batsman2Stats);
            System.out.println("✅ Batsman2Stats: " + batsman2Stats.getPlayerName() + " - " + batsman2Stats.getRuns() + " runs");
        } catch (Exception e) {
            System.err.println("❌ Batsman2Stats failed: " + e.getMessage());
            s.setBatsman2Stats(createEmptyStats(originalBatsman2Id, "Batsman 2"));
        }

        // ✅ Bowler stats
        if (bowlerId != null) {
            try {
                PlayerStatDTO bowlerStats = calculateBowlerStats(bowlerId, inningsId);
                s.setBowlerStats(bowlerStats);
            } catch (Exception e) {
                System.err.println("❌ BowlerStats failed: " + e.getMessage());
            }
        }

        System.out.println("=== END POPULATE STATS ===");
    }

    /**
     * ✅ FIXED: Robust batsman stats calculation
     */
    private PlayerStatDTO calculateBatsmanStats(Long playerId, Long inningsId) {
        if (playerId == null) {
            return createEmptyStats(null, "Unknown Batsman");
        }

        PlayerStatDTO stats = new PlayerStatDTO();
        stats.setPlayerId(playerId);

        // ✅ Get player name with fallback
        try {
            Player player = playerRepo.findById(playerId).orElse(null);
            stats.setPlayerName(player != null ? player.getName() : "Player ID: " + playerId);
        } catch (Exception e) {
            stats.setPlayerName("Player ID: " + playerId);
        }

        // ✅ Calculate stats from balls faced
        try {
            List<CricketBall> balls = ballRepo.findByBatsman_IdAndInnings_Id(playerId, inningsId);
            int runs = 0, ballsFaced = 0, fours = 0, sixes = 0;

            for (CricketBall ball : balls) {
                // Legal deliveries = balls faced
                if (Boolean.TRUE.equals(ball.getLegalDelivery())) {
                    ballsFaced++;
                }

                // Batsman runs
                if (ball.getRuns() != null) {
                    int ballRuns = ball.getRuns();
                    runs += ballRuns;
                    if (ballRuns == 4) fours++;
                    if (ballRuns == 6) sixes++;
                }
            }

            stats.setRuns(runs);
            stats.setBallsFaced(ballsFaced);
            stats.setFours(fours);
            stats.setSixes(sixes);
        } catch (Exception e) {
            System.err.println("Stats calculation failed for batsman " + playerId + ": " + e.getMessage());
            // Keep defaults (0s)
        }

        return stats;
    }

    /**
     * ✅ FIXED: Bowling stats calculation
     */
    private PlayerStatDTO calculateBowlerStats(Long playerId, Long inningsId) {
        PlayerStatDTO stats = new PlayerStatDTO();
        stats.setPlayerId(playerId);

        try {
            Player player = playerRepo.findById(playerId).orElse(null);
            stats.setPlayerName(player != null ? player.getName() : "Bowler ID: " + playerId);
        } catch (Exception e) {
            stats.setPlayerName("Bowler ID: " + playerId);
        }

        try {
            List<CricketBall> balls = ballRepo.findByBowler_IdAndInnings_Id(playerId, inningsId);
            int wickets = 0, runsConceded = 0, ballsBowled = 0;

            for (CricketBall ball : balls) {
                if (Boolean.TRUE.equals(ball.getLegalDelivery())) {
                    ballsBowled++;
                }

                if (ball.getRuns() != null) {
                    runsConceded += ball.getRuns();
                }
                if (ball.getExtra() != null) {
                    runsConceded += ball.getExtra();
                }

                if (ball.getDismissalType() != null && !ball.getDismissalType().isEmpty() &&
                        !ball.getDismissalType().toLowerCase().contains("runout")) {
                    wickets++;
                }
            }

            stats.setWickets(wickets);
            stats.setRunsConceded(runsConceded);
            stats.setBallsBowled(ballsBowled);

            if (ballsBowled > 0) {
                double overs = (double) ballsBowled / Constants.BALLS_PER_OVER;
                stats.setEconomy(round2(runsConceded / overs));
            }

        } catch (Exception e) {
            System.err.println("Bowler stats failed for " + playerId + ": " + e.getMessage());
        }

        return stats;
    }

    private PlayerStatDTO createEmptyStats(Long playerId, String name) {
        PlayerStatDTO stats = new PlayerStatDTO();
        stats.setPlayerId(playerId);
        stats.setPlayerName(name);
        stats.setRuns(0);
        stats.setBallsFaced(0);
        stats.setFours(0);
        stats.setSixes(0);
        return stats;
    }

    // ✅ Clear cache when new innings starts
    public void clearInningsBatsmenCache(Long inningsId) {
        inningsOriginalBatsmen.remove(inningsId);
        System.out.println("✅ Cleared cache for innings: " + inningsId);
    }

    // Existing helper methods (unchanged)...
    private Team getOpposingTeam(Match match, Team team) {
        if (team == null || match.getTeam1() == null || match.getTeam2() == null) return null;
        return match.getTeam1().getId().equals(team.getId()) ? match.getTeam2() : match.getTeam1();
    }

    private boolean isMatchFinal(Match m) {
        if (m == null || m.getStatus() == null) return false;
        String st = m.getStatus().trim().toUpperCase();
        return st.equals("COMPLETED") || st.equals("FINISHED") || st.equals("ABANDONED") || st.equals("TIED");
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
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


}
