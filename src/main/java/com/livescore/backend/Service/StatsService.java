package com.livescore.backend.Service;

import com.livescore.backend.DTO.*;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.CricketRules;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
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
        Stats s;
        if(tournamentId == null) {
            s=statsInterface.findByPlayerId(playerId);
        }
        else{
            s=statsInterface.findByPlayerIdAndTournamentId(playerId,tournamentId).get();
        }
        dto.setPlayerId(playerId);
        dto.setEconomy((double)s.getRunsConceded()/s.getBallsBowled());
        dto.setFours(s.getFours());
        dto.setSixes(s.getSixes());
        dto.setHighest(s.getHighest());
        dto.setBallsFaced(s.getBallsFaced());
        dto.setBattingAvg((double)s.getRuns()/s.getBallsFaced());
        dto.setWickets(s.getWickets());
        dto.setBowlingAverage((double)s.getRunsConceded()/s.getBallsBowled());
        dto.setRunsConceded(s.getRunsConceded());
        dto.setNotOuts(s.getNotOut());
        dto.setTotalRuns(s.getRuns());
        dto.setPlayerName(s.getPlayer().getName());
        dto.setStrikeRate((double)s.getStrikeRate());
        int matches=matchRepo.findMatchesByTeam(playerId);
        dto.setMatchesPlayed(matches);
        int pomMatches=matchRepo.findMatchesBPom(playerId);
        dto.setPomCount(pomMatches);
        dto.setHighest(s.getHighest());
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





    @Transactional
    @Async("statsExecutor")
    public void updateTournamentStats(Long ballId) {
        CricketBall ball = cricketBallInterface.findById(ballId).orElse(null);
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

        // Note: Highest score is now calculated in updateStats() method periodically
        // to avoid expensive query on every ball delivery

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
