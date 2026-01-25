package com.livescore.backend.Service;

import com.livescore.backend.DTO.*;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.CricketRules;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatsService {
    @Autowired
    private StatsInterface statsInterface;
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private CricketBallInterface cricketBallInterface;
    @Autowired
    private GoalsTypeInterface goalsTypeInterface;
    @Autowired
    private MatchInterface matchRepo;
    @Autowired
    private CricketInningsInterface cricketInningsRepo;
    @Autowired
    private TournamentInterface tournamentInterface;

    /**
     * Creates a new Stats entity with all fields initialized to zero/default values.
     * Prevents NullPointerException when accessing numeric fields.
     *
     * @param tournament Tournament entity
     * @param player     Player entity
     * @param sport      Sport type
     * @return Initialized Stats entity
     */
    private Stats createInitializedStats(Tournament tournament, Player player, Sports sport) {
        Stats s = new Stats();
        s.setTournament(tournament);
        s.setPlayer(player);
        s.setSportType(sport);
        
        // Initialize all numeric fields to prevent NullPointerException
        s.setRuns(0);
        s.setWickets(0);
        s.setHighest(0);
        s.setNotOut(0);
        s.setStrikeRate(0);
        s.setBallsFaced(0);
        s.setBallsBowled(0);
        s.setRunsConceded(0);
        s.setFours(0);
        s.setSixes(0);
        s.setPoints(0);
        s.setGoals(0);
        s.setAssists(0);
        s.setFouls(0);
        s.setYellowCards(0);
        s.setRedCards(0);
        
        return s;
    }


    public PlayerFullStatsDTO getPlayerFullStats(Long playerId, Long tournamentId) {
        PlayerFullStatsDTO dto = new PlayerFullStatsDTO();
        dto.playerId = playerId;

        if (playerId == null) {
            dto.playerName = "Unknown";
            return dto;
        }

        Player p = playerInterface.findActiveById(playerId).orElse(null);
        dto.playerName = p != null ? p.getName() : "Unknown";

        boolean byTournament = (tournamentId != null);

        Object[] batting = byTournament
                ? cricketBallInterface.getBattingAggregate(playerId, tournamentId)
                : cricketBallInterface.getBattingAggregateOverall(playerId);
        if (batting != null && batting.length >= 4) {
            dto.totalRuns = batting[0] != null ? ((Number) batting[0]).intValue() : 0;
            dto.ballsFaced = batting[1] != null ? ((Number) batting[1]).intValue() : 0;
            dto.fours = batting[2] != null ? ((Number) batting[2]).intValue() : 0;
            dto.sixes = batting[3] != null ? ((Number) batting[3]).intValue() : 0;
        } else {
            dto.totalRuns = 0;
            dto.ballsFaced = 0;
            dto.fours = 0;
            dto.sixes = 0;
        }

        Object[] bowling = byTournament
                ? cricketBallInterface.getBowlingAggregate(playerId, tournamentId)
                : cricketBallInterface.getBowlingAggregateOverall(playerId);
        if (bowling != null && bowling.length >= 3) {
            dto.runsConceded = bowling[0] != null ? ((Number) bowling[0]).intValue() : 0;
            dto.ballsBowled = bowling[1] != null ? ((Number) bowling[1]).intValue() : 0;
            dto.wickets = bowling[2] != null ? ((Number) bowling[2]).intValue() : 0;
        } else {
            dto.runsConceded = 0;
            dto.ballsBowled = 0;
            dto.wickets = 0;
        }

        dto.strikeRate = dto.ballsFaced == null || dto.ballsFaced == 0
                ? 0.0
                : roundTo2(((double) dto.totalRuns) * 100.0 / (double) dto.ballsFaced);

        dto.economy = dto.ballsBowled == null || dto.ballsBowled == 0
                ? 0.0
                : roundTo2(((double) dto.runsConceded) * 6.0 / (double) dto.ballsBowled);

        dto.bowlingAverage = dto.wickets == null || dto.wickets == 0
                ? 0.0
                : roundTo2(((double) dto.runsConceded) / (double) dto.wickets);

        Integer highest = byTournament
                ? cricketBallInterface.getRunsPerInningsDesc(playerId, tournamentId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(0)
                : cricketBallInterface.getRunsPerInningsDescOverall(playerId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(0);
        dto.highest = highest == null ? 0 : highest;

        Integer notOut = byTournament
                ? cricketBallInterface.countNotOutInnings(playerId, tournamentId)
                : cricketBallInterface.countNotOutInningsOverall(playerId);
        dto.notOuts = notOut == null ? 0 : notOut;

        dto.matchesPlayed = byTournament
                ? cricketBallInterface.countMatchesPlayedInTournament(playerId, tournamentId)
                : cricketBallInterface.countMatchesPlayedOverall(playerId);

        int inningsBatted = byTournament
                ? cricketBallInterface.countDistinctInningsBatted(playerId, tournamentId)
                : cricketBallInterface.countDistinctInningsBattedOverall(playerId);
        int outs = Math.max(0, inningsBatted - dto.notOuts);
        dto.battingAvg = outs == 0 ? (double) dto.totalRuns : roundTo2((double) dto.totalRuns / (double) outs);

        dto.pomCount = byTournament
                ? (int) matchRepo.countManOfMatchForPlayer(tournamentId, playerId)
                : (int) matchRepo.countManOfMatchForPlayerOverall(playerId);

        return dto;
    }


    public void createStats(Long playerId, Long tournamentId) {
        if (playerId == null || tournamentId == null) {
            return;
        }
        if (statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).isPresent()) {
            return;
        }

        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (player == null) {
            return;
        }
        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) {
            return;
        }

        Stats stats = new Stats();
        stats.setPlayer(player);
        stats.setTournament(t);
        stats.setSportType(t.getSport());
        statsInterface.save(stats);
    }

    public ResponseEntity<?> updateStats(Long playerId, Long tournamentId, Long matchId) {
        if (playerId == null || tournamentId == null) {
            return ResponseEntity.badRequest().body("playerId and tournamentId are required");
        }
        Stats existingStats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId)
                .orElse(null);
        if (existingStats == null) {
            return ResponseEntity.notFound().build();
        }

        String sportName = existingStats.getSportType() != null
                ? existingStats.getSportType().getName()
                : "";

        if (sportName.equalsIgnoreCase("CRICKET")) {
            List<CricketBall> batsmanBalls = cricketBallInterface.findBatsmanBalls(tournamentId, playerId);
            List<CricketBall> bowlerBalls = cricketBallInterface.findBowlerBalls(tournamentId, playerId);
            List<CricketBall> fielderBalls = cricketBallInterface.findFielderBalls(tournamentId, playerId);


            int totalRuns = batsmanBalls.stream()
                    .mapToInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)
                    .sum();

            // balls faced: exclude wides (and optionally no-balls if you prefer)
            long ballsFaced = batsmanBalls.stream()
                    .filter(CricketRules::isBallFaced)
                    .count();

            // highest per-innings: group by innings id and sum runs per innings, take max
            Map<Long, Integer> runsPerInnings = batsmanBalls.stream()
                    .filter(cb -> cb.getInnings() != null && cb.getInnings().getId() != null)
                    .collect(Collectors.groupingBy(cb -> cb.getInnings().getId(),
                            Collectors.summingInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)));
            int highest = runsPerInnings.values().stream().mapToInt(Integer::intValue).max().orElse(0);


            Set<Long> inningsWithAnyBall = batsmanBalls.stream()
                    .filter(cb -> cb.getInnings() != null && cb.getInnings().getId() != null)
                    .map(cb -> cb.getInnings().getId())
                    .collect(Collectors.toSet());

            Set<Long> inningsWhereOut = batsmanBalls.stream()
                    .filter(cb -> cb.getDismissalType() != null && cb.getInnings() != null && cb.getInnings().getId() != null)
                    .map(cb -> cb.getInnings().getId())
                    .collect(Collectors.toSet());

            int notOutCount = (int) inningsWithAnyBall.stream()
                    .filter(innId -> !inningsWhereOut.contains(innId))
                    .count();


            int strikeRate = (ballsFaced == 0) ? 0 : (int) Math.round(((double) totalRuns * 100.0) / (double) ballsFaced);

            // --- BOWLING ---

            int runsConceded = bowlerBalls.stream()
                    .mapToInt(cb -> {
                        return CricketRules.runsConcededThisBall(cb);
                    })
                    .sum();

            // wickets: only count dismissal types that are credited to bowler (not runouts)
            int wickets = (int) bowlerBalls.stream()
                    .filter(cb -> cb.getDismissalType() != null)
                    .filter(cb -> CricketRules.isBowlerCreditedWicket(cb.getDismissalType()))
                    .count();

            // balls bowled: count legal deliveries (exclude wides and no-balls)
            int ballsBowled = (int) bowlerBalls.stream()
                    .filter(cb -> Boolean.TRUE.equals(cb.getLegalDelivery()))
                    .count();

            // overs (optional) = ballsBowled / 6 and ballsBowled % 6 for remainder

            // --- FIELDING ---
            // catches/runouts: rely on dismissalType values — handle variations
            int catches = (int) fielderBalls.stream()
                    .filter(cb -> cb.getDismissalType() != null)
                    .filter(cb -> {
                        String dt = cb.getDismissalType().toLowerCase();
                        return dt.contains("catch") || dt.contains("caught");
                    })
                    .count();

            int runouts = (int) fielderBalls.stream()
                    .filter(cb -> cb.getDismissalType() != null)
                    .filter(cb -> cb.getDismissalType().toLowerCase().contains("runout"))
                    .count();

            // --- POINTS (simple example, tweak rules as needed) ---
            int points = (wickets * 10) + totalRuns + (catches * 5) + (runouts * 5);

            // --- SAVE ---
            existingStats.setRuns(totalRuns);
            existingStats.setStrikeRate(strikeRate);
            existingStats.setHighest(highest);
            existingStats.setNotOut(notOutCount);
            existingStats.setWickets(wickets);
            existingStats.setPoints(points);

        } else if (sportName.equalsIgnoreCase("FUTSAL") || sportName.equalsIgnoreCase("FOOTBALL")) {
            // Use GoalsTypeInterface to aggregate futsal/football stats
            // If you want match-scoped stats use findGoalsByMatchAndPlayer(matchId, playerId)
            List<GoalsType> goals = (matchId != null && matchId > 0)
                    ? goalsTypeInterface.findGoalsByMatchAndPlayer(matchId, playerId)
                    : goalsTypeInterface.findGoalsByTournamentAndPlayer(tournamentId, playerId);

            int totalGoals = goals.stream().mapToInt(gt -> gt.getGoal()).sum();
            int totalAssists = goals.stream().mapToInt(gt -> gt.getAssist()).sum();
            int totalFouls = goals.stream().mapToInt(gt -> gt.getFoul()).sum();
            int yellow = goals.stream().mapToInt(gt -> gt.getYellow()).sum();
            int red = goals.stream().mapToInt(gt -> gt.getRed()).sum();

            existingStats.setGoals(totalGoals);
            existingStats.setAssists(totalAssists);
            existingStats.setFouls(totalFouls);
            existingStats.setYellowCards(yellow);
            existingStats.setRedCards(red);

            // Optionally calculate points based on futsal rules:
            existingStats.setPoints(totalGoals * 5 + totalAssists * 3 - totalFouls);
        }

        Stats updatedStats = statsInterface.save(existingStats);
        return ResponseEntity.ok(updatedStats);
    }



    /**
     * Updates tournament statistics for a single cricket ball delivery.
     * Creates new Stats entities if they don't exist for the players involved.
     *
     * @param ball CricketBall entity to process
     */
    @Transactional
    public void updateTournamentStats(CricketBall ball) {
        if (ball == null) return;
        Match match = ball.getMatch();
        if (match == null || match.getTournament() == null) return;
        Tournament tournament = match.getTournament();

        // Update batsman statistics
        updateBatsmanStats(ball, tournament);

        // Update bowler statistics
        updateBowlerStats(ball, tournament);
    }

    /**
     * Updates batsman statistics for a single ball.
     */
    private void updateBatsmanStats(CricketBall ball, Tournament tournament) {
        Player batsman = ball.getBatsman();
        if (batsman == null) return;

        Stats batStats = statsInterface.findByTournamentIdAndPlayerId(tournament.getId(), batsman.getId())
                .orElseGet(() -> createInitializedStats(tournament, batsman, tournament.getSport()));

        // Update runs
        int runs = (ball.getRuns() == null ? 0 : ball.getRuns());
        batStats.setRuns(batStats.getRuns() + runs);

        // Update balls faced
        if (CricketRules.isBallFaced(ball)) {
            batStats.setBallsFaced(batStats.getBallsFaced() + 1);
        }

        // Update boundaries
        if (Boolean.TRUE.equals(ball.getIsFour())) {
            batStats.setFours(batStats.getFours() + 1);
        }
        if (Boolean.TRUE.equals(ball.getIsSix())) {
            batStats.setSixes(batStats.getSixes() + 1);
        }

        // Update highest score per innings
        if (ball.getInnings() != null && ball.getInnings().getId() != null) {
            Integer inningsRuns = cricketBallInterface.sumBatsmanRunsByInnings(
                    batsman.getId(), ball.getInnings().getId());
            int inns = inningsRuns == null ? 0 : inningsRuns;
            if (inns > batStats.getHighest()) {
                batStats.setHighest(inns);
            }
        }

        statsInterface.save(batStats);
    }

    /**
     * Updates bowler statistics for a single ball.
     */
    private void updateBowlerStats(CricketBall ball, Tournament tournament) {
        Player bowler = ball.getBowler();
        if (bowler == null) return;

        Stats bowlStats = statsInterface.findByTournamentIdAndPlayerId(tournament.getId(), bowler.getId())
                .orElseGet(() -> createInitializedStats(tournament, bowler, tournament.getSport()));

        // Update runs conceded
        bowlStats.setRunsConceded(bowlStats.getRunsConceded() + CricketRules.runsConcededThisBall(ball));

        // Update balls bowled (legal deliveries only)
        if (Boolean.TRUE.equals(ball.getLegalDelivery())) {
            bowlStats.setBallsBowled(bowlStats.getBallsBowled() + 1);
        }

        // Update wickets (only if credited to bowler)
        String dismissal = ball.getDismissalType();
        if (dismissal != null && CricketRules.isBowlerCreditedWicket(dismissal)) {
            bowlStats.setWickets(bowlStats.getWickets() + 1);
        }

        statsInterface.save(bowlStats);
    }



    private double roundTo2(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            return val;
        }
        BigDecimal bd = BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
