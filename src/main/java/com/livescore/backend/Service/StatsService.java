package com.livescore.backend.Service;

import com.livescore.backend.Cricket.CricketRules;
import com.livescore.backend.DTO.PlayerFullStatsDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {


    private final StatsInterface statsInterface;
    private final PlayerInterface playerInterface;
    private final CricketBallInterface cricketBallInterface;
    private final MatchInterface matchRepo;
    private final TournamentInterface tournamentInterface;
    private final AwardInterface awardInterface;

    // ==================== STATS CREATION ====================

    private Stats createInitializedStats(Tournament tournament, Player player, Sports sport) {
        Stats s = new Stats();
        s.setTournament(tournament);
        s.setPlayer(player);
        s.setSportType(sport);
        s.setRuns(0);
        s.setWickets(0);
        s.setHighest(0);
        s.setNotOut(0);
        s.setStrikeRate(0);
        s.setBattingAverage(0.0);
        s.setBallsFaced(0);
        s.setBallsBowled(0);
        s.setRunsConceded(0);
        s.setFours(0);
        s.setSixes(0);
        s.setFifties(0);
        s.setHundreds(0);
        s.setInningsPlayed(0);
        s.setMaidens(0);
        s.setThreeWicketHauls(0);
        s.setFiveWicketHauls(0);
        s.setDotBalls(0);
        s.setCatches(0);
        s.setRunouts(0);
        s.setStumpings(0);
        s.setPlayerOfMatchCount(0);
        s.setPoints(0);
        s.setEconomy(0.0);
        s.setBowlingAverage(0.0);
        s.setBowlingStrikeRate(0.0);
        s.setGoals(0);
        s.setAssists(0);
        s.setFouls(0);
        s.setYellowCards(0);
        s.setRedCards(0);
        return s;
    }

    public void createStats(Long playerId, Long tournamentId) {
        if (playerId == null || tournamentId == null) return;
        if (statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).isPresent()) return;

        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (player == null) return;

        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) return;

        Stats stats = createInitializedStats(t, player, t.getSport());
        statsInterface.save(stats);
    }

    // ==================== END OF MATCH HOOK ====================

    @Transactional
    public void onMatchEnd(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;

        Long tournamentId = match.getTournament().getId();

        // 1. Gather all player IDs from this match
        Set<Long> playerIds = new HashSet<>();
        for (CricketInnings ci : match.getCricketInnings()) {
            List<CricketBall> balls = cricketBallInterface.findAllByInningsAndMatch(ci.getId(), matchId);
            for (CricketBall b : balls) {
                if (b.getBatsman() != null) playerIds.add(b.getBatsman().getId());
                if (b.getBowler() != null) playerIds.add(b.getBowler().getId());
                if (b.getFielder() != null) playerIds.add(b.getFielder().getId());
            }
        }

        // 2. Recalculate full stats for each player
        for (Long pid : playerIds) {
            recalculatePlayerStats(pid, tournamentId);
        }

        // 3. Player of the Match
        calculatePlayerOfMatch(matchId);

        // 4. Check if tournament is over — generate tournament awards
        checkAndHandleTournamentEnd(match.getTournament());
    }

    // ==================== FULL RECALCULATION ====================

    @Transactional
    public void recalculatePlayerStats(Long playerId, Long tournamentId) {
        if (playerId == null || tournamentId == null) return;

        Tournament tournament = tournamentInterface.findById(tournamentId).orElse(null);
        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (tournament == null || player == null) return;

        Stats stats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId)
                .orElseGet(() -> createInitializedStats(tournament, player, tournament.getSport()));

        List<CricketBall> batsmanBalls = cricketBallInterface.findBatsmanBalls(tournamentId, playerId);
        List<CricketBall> bowlerBalls = cricketBallInterface.findBowlerBalls(tournamentId, playerId);
        List<CricketBall> fielderBalls = cricketBallInterface.findFielderBalls(tournamentId, playerId);

        recalculateBatting(stats, batsmanBalls);
        recalculateBowling(stats, bowlerBalls);
        recalculateFielding(stats, fielderBalls);

        int pomCount = awardInterface.countPomByPlayerId(playerId);
        stats.setPlayerOfMatchCount(pomCount);

        recalculateDerivedFields(stats);
        statsInterface.save(stats);
    }

    private void recalculateBatting(Stats stats, List<CricketBall> batsmanBalls) {
        int totalRuns = batsmanBalls.stream()
                .filter(cb -> !isWide(cb) && !isByeOrLegBye(cb))
                .mapToInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)
                .sum();

        long ballsFaced = batsmanBalls.stream()
                .filter(CricketRules::isBallFaced)
                .count();

        Map<Long, Integer> runsPerInnings = batsmanBalls.stream()
                .filter(cb -> cb.getInnings() != null && cb.getInnings().getId() != null)
                .filter(cb -> !isWide(cb) && !isByeOrLegBye(cb))
                .collect(Collectors.groupingBy(
                        cb -> cb.getInnings().getId(),
                        Collectors.summingInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)));

        int highest = runsPerInnings.values().stream()
                .mapToInt(Integer::intValue).max().orElse(0);
        int fifties = (int) runsPerInnings.values().stream()
                .filter(r -> r >= 50 && r < 100).count();
        int hundreds = (int) runsPerInnings.values().stream()
                .filter(r -> r >= 100).count();

        Set<Long> allInnings = batsmanBalls.stream()
                .filter(cb -> cb.getInnings() != null && cb.getInnings().getId() != null)
                .map(cb -> cb.getInnings().getId())
                .collect(Collectors.toSet());

        Set<Long> dismissedInnings = batsmanBalls.stream()
                .filter(cb -> cb.getDismissalType() != null
                        && cb.getInnings() != null
                        && cb.getInnings().getId() != null)
                .map(cb -> cb.getInnings().getId())
                .collect(Collectors.toSet());

        int notOutCount = (int) allInnings.stream()
                .filter(id -> !dismissedInnings.contains(id))
                .count();

        int inningsPlayed = allInnings.size();

        int foursCount = (int) batsmanBalls.stream()
                .filter(cb -> Boolean.TRUE.equals(cb.getIsFour())).count();
        int sixesCount = (int) batsmanBalls.stream()
                .filter(cb -> Boolean.TRUE.equals(cb.getIsSix())).count();

        stats.setRuns(totalRuns);
        stats.setBallsFaced((int) ballsFaced);
        stats.setHighest(highest);
        stats.setFifties(fifties);
        stats.setHundreds(hundreds);
        stats.setNotOut(notOutCount);
        stats.setInningsPlayed(inningsPlayed);
        stats.setFours(foursCount);
        stats.setSixes(sixesCount);
    }

    private void recalculateBowling(Stats stats, List<CricketBall> bowlerBalls) {
        int runsConceded = bowlerBalls.stream()
                .mapToInt(CricketRules::runsConcededThisBall)
                .sum();

        int wickets = (int) bowlerBalls.stream()
                .filter(cb -> cb.getDismissalType() != null)
                .filter(cb -> CricketRules.isBowlerCreditedWicket(cb.getDismissalType()))
                .count();

        int ballsBowled = (int) bowlerBalls.stream()
                .filter(cb -> Boolean.TRUE.equals(cb.getLegalDelivery()))
                .count();

        int dotBalls = (int) bowlerBalls.stream()
                .filter(CricketRules::isDotBall)
                .count();

        Map<Long, Long> wicketsPerInnings = bowlerBalls.stream()
                .filter(cb -> cb.getDismissalType() != null
                        && CricketRules.isBowlerCreditedWicket(cb.getDismissalType())
                        && cb.getInnings() != null
                        && cb.getInnings().getId() != null)
                .collect(Collectors.groupingBy(
                        cb -> cb.getInnings().getId(), Collectors.counting()));

        int threeHauls = (int) wicketsPerInnings.values().stream()
                .filter(w -> w >= 3).count();
        int fiveHauls = (int) wicketsPerInnings.values().stream()
                .filter(w -> w >= 5).count();

        Map<String, List<CricketBall>> overGroups = bowlerBalls.stream()
                .filter(cb -> cb.getInnings() != null && cb.getOverNumber() != null)
                .collect(Collectors.groupingBy(
                        cb -> cb.getInnings().getId() + "_" + cb.getOverNumber()));

        int maidens = (int) overGroups.values().stream()
                .filter(balls -> {
                    long legalCount = balls.stream()
                            .filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count();
                    int totalConceded = balls.stream()
                            .mapToInt(CricketRules::runsConcededThisBall).sum();
                    return legalCount == 6 && totalConceded == 0;
                }).count();

        stats.setWickets(wickets);
        stats.setBallsBowled(ballsBowled);
        stats.setRunsConceded(runsConceded);
        stats.setDotBalls(dotBalls);
        stats.setMaidens(maidens);
        stats.setThreeWicketHauls(threeHauls);
        stats.setFiveWicketHauls(fiveHauls);
    }

    private void recalculateFielding(Stats stats, List<CricketBall> fielderBalls) {
        int catchCount = (int) fielderBalls.stream()
                .filter(cb -> cb.getDismissalType() != null)
                .filter(cb -> {
                    String dt = cb.getDismissalType().toLowerCase();
                    return dt.contains("catch") || dt.contains("caught");
                }).count();

        int runoutCount = (int) fielderBalls.stream()
                .filter(cb -> cb.getDismissalType() != null)
                .filter(cb -> cb.getDismissalType().toLowerCase().contains("runout"))
                .count();

        int stumpingCount = (int) fielderBalls.stream()
                .filter(cb -> cb.getDismissalType() != null)
                .filter(cb -> cb.getDismissalType().toLowerCase().contains("stumped"))
                .count();

        stats.setCatches(catchCount);
        stats.setRunouts(runoutCount);
        stats.setStumpings(stumpingCount);
    }

    // ==================== DERIVED FIELDS ====================

    private void recalculateDerivedFields(Stats stats) {
        stats.setStrikeRate(stats.getBallsFaced() > 0
                ? (int) Math.round((double) stats.getRuns() * 100.0 / stats.getBallsFaced())
                : 0);

        int dismissals = safeInt(stats.getInningsPlayed()) - safeInt(stats.getNotOut());
        stats.setBattingAverage(dismissals > 0
                ? roundTo2((double) stats.getRuns() / dismissals)
                : 0.0);

        stats.setEconomy(stats.getBallsBowled() > 0
                ? roundTo2((double) stats.getRunsConceded() * 6.0 / stats.getBallsBowled())
                : 0.0);

        stats.setBowlingAverage(stats.getWickets() > 0
                ? roundTo2((double) stats.getRunsConceded() / stats.getWickets())
                : 0.0);

        stats.setBowlingStrikeRate(stats.getWickets() > 0
                ? roundTo2((double) stats.getBallsBowled() / stats.getWickets())
                : 0.0);

        stats.setPoints(calculatePoints(stats));
    }

    private int calculatePoints(Stats stats) {
        return stats.getRuns()
                + (stats.getWickets() * 25)
                + (stats.getFours())
                + (stats.getSixes() * 2)
                + (stats.getCatches() * 10)
                + (stats.getRunouts() * 10)
                + (stats.getStumpings() * 10)
                + (stats.getFifties() * 20)
                + (stats.getHundreds() * 50)
                + (stats.getThreeWicketHauls() * 20)
                + (stats.getFiveWicketHauls() * 50)
                + (stats.getMaidens() * 15);
    }

    // ==================== TOURNAMENT END CHECK ====================

    private void checkAndHandleTournamentEnd(Tournament tournament) {
        List<Match> allMatches = matchRepo.findByTournamentId(tournament.getId());
        if (allMatches == null || allMatches.isEmpty()) return;

        boolean allCompleted = allMatches.stream()
                .allMatch(m -> m.getStatus() != null
                        && (m.getStatus().equalsIgnoreCase("COMPLETED")
                        || m.getStatus().equalsIgnoreCase("FINISHED")));

        if (allCompleted) {
            Optional<Award> existing = awardInterface.findByTournamentIdAndAwardType(
                    tournament.getId(), "MAN_OF_TOURNAMENT");
            if (existing.isEmpty()) {
                calculateEndOfTournamentAwards(tournament.getId());
            }
        }
    }

    // ==================== AWARDS ====================

    @Transactional
    public Award calculatePlayerOfMatch(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return null;

        Optional<Award> existing = awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH");
        if (existing.isPresent()) return existing.get();

        Long tournamentId = match.getTournament().getId();
        List<CricketInnings> inningsList = match.getCricketInnings();
        if (inningsList == null || inningsList.isEmpty()) return null;

        Map<Long, Integer> playerPoints = new HashMap<>();
        Map<Long, String> playerReasons = new HashMap<>();

        for (CricketInnings ci : inningsList) {
            List<CricketBall> balls = cricketBallInterface.findAllByInningsAndMatch(ci.getId(), matchId);

            Map<Long, List<CricketBall>> byBatsman = balls.stream()
                    .filter(b -> b.getBatsman() != null)
                    .collect(Collectors.groupingBy(b -> b.getBatsman().getId()));

            for (Map.Entry<Long, List<CricketBall>> entry : byBatsman.entrySet()) {
                Long pid = entry.getKey();
                List<CricketBall> pBalls = entry.getValue();

                int runs = pBalls.stream()
                        .filter(b -> !isWide(b) && !isByeOrLegBye(b))
                        .mapToInt(b -> b.getRuns() != null ? b.getRuns() : 0).sum();

                int fours = (int) pBalls.stream()
                        .filter(b -> Boolean.TRUE.equals(b.getIsFour())).count();
                int sixes = (int) pBalls.stream()
                        .filter(b -> Boolean.TRUE.equals(b.getIsSix())).count();

                int pts = runs + fours + (sixes * 2);
                if (runs >= 100) pts += 50;
                else if (runs >= 50) pts += 20;

                playerPoints.merge(pid, pts, Integer::sum);
                playerReasons.merge(pid, runs + " runs", (old, nw) -> old + ", " + nw);
            }

            Map<Long, List<CricketBall>> byBowler = balls.stream()
                    .filter(b -> b.getBowler() != null)
                    .collect(Collectors.groupingBy(b -> b.getBowler().getId()));

            for (Map.Entry<Long, List<CricketBall>> entry : byBowler.entrySet()) {
                Long pid = entry.getKey();
                List<CricketBall> pBalls = entry.getValue();

                int wkt = (int) pBalls.stream()
                        .filter(b -> b.getDismissalType() != null)
                        .filter(b -> CricketRules.isBowlerCreditedWicket(b.getDismissalType()))
                        .count();

                int dots = (int) pBalls.stream()
                        .filter(CricketRules::isDotBall)
                        .count();

                int pts = (wkt * 25) + (dots * 2);
                if (wkt >= 5) pts += 50;
                else if (wkt >= 3) pts += 20;

                playerPoints.merge(pid, pts, Integer::sum);
                playerReasons.merge(pid, wkt + " wickets", (old, nw) -> old + ", " + nw);
            }

            balls.stream()
                    .filter(b -> b.getFielder() != null && b.getDismissalType() != null)
                    .forEach(b -> playerPoints.merge(b.getFielder().getId(), 10, Integer::sum));
        }

        if (playerPoints.isEmpty()) return null;

        Map.Entry<Long, Integer> best = playerPoints.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (best == null) return null;

        Player pomPlayer = playerInterface.findActiveById(best.getKey()).orElse(null);
        if (pomPlayer == null) return null;

        Award award = new Award();
        award.setMatch(match);
        award.setTournament(match.getTournament());
        award.setPlayer(pomPlayer);
        award.setAwardType("PLAYER_OF_MATCH");
        award.setPointsEarned(best.getValue());
        award.setReason(playerReasons.getOrDefault(best.getKey(), ""));

        Award saved = awardInterface.save(award);

        Stats pomStats = statsInterface.findByTournamentIdAndPlayerId(
                        tournamentId, pomPlayer.getId())
                .orElse(null);
        if (pomStats != null) {
            pomStats.setPlayerOfMatchCount(pomStats.getPlayerOfMatchCount() + 1);
            statsInterface.save(pomStats);
        }

        return saved;
    }

    @Transactional
    public List<Award> calculateEndOfTournamentAwards(Long tournamentId) {
        Tournament tournament = tournamentInterface.findById(tournamentId).orElse(null);
        if (tournament == null) return Collections.emptyList();

        List<Award> awards = new ArrayList<>();
        List<Stats> allStats = statsInterface.findByTournamentId(tournamentId);

        allStats.stream()
                .max(Comparator.comparingInt(Stats::getPoints))
                .ifPresent(s -> awards.add(createAward(tournament, s.getPlayer(),
                        "MAN_OF_TOURNAMENT", s.getPoints(),
                        "Highest overall points: " + s.getPoints())));

        allStats.stream()
                .max(Comparator.comparingInt(Stats::getRuns))
                .ifPresent(s -> awards.add(createAward(tournament, s.getPlayer(),
                        "BEST_BATSMAN", s.getRuns(),
                        "Most runs: " + s.getRuns())));

        allStats.stream()
                .filter(s -> s.getWickets() > 0)
                .max(Comparator.comparingInt(Stats::getWickets))
                .ifPresent(s -> awards.add(createAward(tournament, s.getPlayer(),
                        "BEST_BOWLER", s.getWickets(),
                        "Most wickets: " + s.getWickets())));

        allStats.stream()
                .max(Comparator.comparingInt(s ->
                        s.getCatches() + s.getRunouts() + s.getStumpings()))
                .filter(s -> (s.getCatches() + s.getRunouts() + s.getStumpings()) > 0)
                .ifPresent(s -> {
                    int total = s.getCatches() + s.getRunouts() + s.getStumpings();
                    awards.add(createAward(tournament, s.getPlayer(),
                            "BEST_FIELDER", total,
                            "Fielding dismissals: " + total));
                });

        allStats.stream()
                .max(Comparator.comparingInt(Stats::getSixes))
                .filter(s -> s.getSixes() > 0)
                .ifPresent(s -> awards.add(createAward(tournament, s.getPlayer(),
                        "MOST_SIXES", s.getSixes(),
                        "Most sixes: " + s.getSixes())));

        return awardInterface.saveAll(awards);
    }

    private Award createAward(Tournament tournament, Player player,
                              String type, int points, String reason) {
        Award a = new Award();
        a.setTournament(tournament);
        a.setPlayer(player);
        a.setAwardType(type);
        a.setPointsEarned(points);
        a.setReason(reason);
        return a;
    }

    // ==================== PLAYER FULL STATS DTO ====================

    public PlayerFullStatsDTO getPlayerFullStats(Long playerId, Long tournamentId) {
        PlayerFullStatsDTO dto = new PlayerFullStatsDTO();
        Stats s;
        if (tournamentId == null) {
            s = statsInterface.findByPlayerId(playerId);
        } else {
            s = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).orElse(null);
        }
        if (s == null) return dto;

        dto.setPlayerId(playerId);
        dto.setPlayerName(s.getPlayer().getName());
        dto.setTotalRuns(s.getRuns());
        dto.setBallsFaced(s.getBallsFaced());
        dto.setFours(s.getFours());
        dto.setSixes(s.getSixes());
        dto.setHighest(s.getHighest());
        dto.setNotOuts(s.getNotOut());
        dto.setStrikeRate((double) s.getStrikeRate());
        dto.setWickets(s.getWickets());
        dto.setRunsConceded(s.getRunsConceded());
        dto.setBattingAvg(s.getBattingAverage() != null ? s.getBattingAverage() : 0);
        dto.setBowlingAverage(s.getBowlingAverage() != null ? s.getBowlingAverage() : 0);
        dto.setEconomy(s.getEconomy() != null ? s.getEconomy() : 0);

        int matches = matchRepo.findMatchesByTeam(playerId);
        dto.setMatchesPlayed(matches);

        int pomCount = awardInterface.countPomByPlayerId(playerId);
        dto.setPomCount(pomCount);

        return dto;
    }

    // ==================== HELPERS ====================

    private boolean isWide(CricketBall cb) {
        return cb.getExtraType() != null && cb.getExtraType().equalsIgnoreCase("wide");
    }

    private boolean isByeOrLegBye(CricketBall cb) {
        if (cb.getExtraType() == null) return false;
        String et = cb.getExtraType().toLowerCase();
        return et.equals("bye") || et.equals("legbye");
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private double roundTo2(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) return 0.0;
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}