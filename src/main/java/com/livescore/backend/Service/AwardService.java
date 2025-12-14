package com.livescore.backend.Service;

import com.livescore.backend.DTO.AwardsDTO;
import com.livescore.backend.DTO.PlayerStatDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AwardService {
    @Autowired
    private CricketBallInterface cricketBallInterface;
    @Autowired
    private PlayerInterface playerRepo;
    @Autowired
    private MatchInterface matchRepo;
    @Autowired
    private StatsService statsService; // optional, to update Stats.highest from innings

    @Transactional
    public void computeMatchAwards(Long matchId) {
        // --- YOUR EXISTING METHOD (unchanged) ---
        Match match = matchRepo.findById(matchId).orElseThrow();
        List<CricketBall> balls = cricketBallInterface.findByMatch_Id(matchId);
        if (balls == null || balls.isEmpty()) return;

        // per-player aggregation holder
        class Agg {
            long playerId;
            int runs = 0;
            int ballsFaced = 0; // legal deliveries faced as batsman
            int fours = 0;
            int sixes = 0;
            int wickets = 0;
            int runsConceded = 0;
            int ballsBowled = 0; // legal deliveries bowled
            Map<Long, Integer> inningsRuns = new HashMap<>();
        }

        Map<Long, Agg> map = new HashMap<>();

        java.util.function.Function<Player, Agg> getAgg = (player) -> {
            if (player == null) return null;
            return map.computeIfAbsent(player.getId(), id -> {
                Agg a = new Agg();
                a.playerId = id;
                return a;
            });
        };

        for (CricketBall b : balls) {
            Long inningsId = b.getInnings() == null ? null : b.getInnings().getId();

            // batsman
            if (b.getBatsman() != null) {
                Agg a = getAgg.apply(b.getBatsman());
                int r = (b.getRuns() == null ? 0 : b.getRuns());
                a.runs += r;
                if (Boolean.TRUE.equals(b.getLegalDelivery())) a.ballsFaced += 1;
                if (Boolean.TRUE.equals(b.getIsFour())) a.fours += 1;
                if (Boolean.TRUE.equals(b.getIsSix())) a.sixes += 1;
                if (inningsId != null) {
                    a.inningsRuns.put(inningsId, a.inningsRuns.getOrDefault(inningsId, 0) + r);
                }
            }

            // bowler
            if (b.getBowler() != null) {
                Agg a = getAgg.apply(b.getBowler());
                int runsConcededThisBall = (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra());
                a.runsConceded += runsConcededThisBall;
                if (Boolean.TRUE.equals(b.getLegalDelivery())) a.ballsBowled += 1;

                // wickets credit (exclude runout)
                String d = b.getDismissalType();
                if (d != null) {
                    d = d.toLowerCase();
                    if (d.equals("bowled") || d.equals("caught") || d.equals("lbw") || d.equals("stumped") || d.equals("hit-wicket")) {
                        a.wickets += 1;
                    }
                }
            }
        }

        // compute derived metrics per player
        class PlayerMetrics {
            Long playerId;
            int runs;
            int ballsFaced;
            double strikeRate;
            int highestInnings;
            int fours;
            int sixes;
            int wickets;
            int runsConceded;
            int ballsBowled;
            double economy; // runs per over
            double bowlingAverage;
        }

        List<PlayerMetrics> metrics = new ArrayList<>();
        for (Agg a : map.values()) {
            PlayerMetrics pm = new PlayerMetrics();
            pm.playerId = a.playerId;
            pm.runs = a.runs;
            pm.ballsFaced = a.ballsFaced;
            pm.fours = a.fours;
            pm.sixes = a.sixes;
            pm.wickets = a.wickets;
            pm.runsConceded = a.runsConceded;
            pm.ballsBowled = a.ballsBowled;
            pm.highestInnings = a.inningsRuns.values().stream().mapToInt(Integer::intValue).max().orElse(0);

            if (pm.ballsFaced > 0) pm.strikeRate = (double) pm.runs * 100.0 / pm.ballsFaced;
            else pm.strikeRate = 0.0;

            if (pm.ballsBowled > 0) {
                double overs = pm.ballsBowled / 6.0;
                pm.economy = pm.runsConceded / overs;
            } else pm.economy = Double.POSITIVE_INFINITY;

            if (pm.wickets > 0) pm.bowlingAverage = (double) pm.runsConceded / pm.wickets;
            else pm.bowlingAverage = Double.POSITIVE_INFINITY;

            metrics.add(pm);
        }

        // Best Batsman
        PlayerMetrics bestBat = metrics.stream()
                .filter(m -> m.ballsFaced > 0)
                .max(Comparator.comparingInt((PlayerMetrics m) -> m.runs)
                        .thenComparingInt(m -> m.highestInnings)
                        .thenComparingDouble(m -> m.strikeRate))
                .orElse(null);

        // Best Bowler - manual comparator to ensure deterministic tiebreakers
        PlayerMetrics bestBowl = null;
        for (PlayerMetrics pm : metrics) {
            if (pm.ballsBowled == 0) continue;
            if (bestBowl == null) { bestBowl = pm; continue; }
            if (pm.wickets > bestBowl.wickets) bestBowl = pm;
            else if (pm.wickets == bestBowl.wickets) {
                if (Double.compare(pm.economy, bestBowl.economy) < 0) bestBowl = pm; // lower economy wins
                else if (Double.compare(pm.economy, bestBowl.economy) == 0) {
                    if (Double.compare(pm.bowlingAverage, bestBowl.bowlingAverage) < 0) bestBowl = pm;
                }
            }
        }

        // Man of the Match - composite score
        class MoMMetrics {
            PlayerMetrics pm;
            double score;
        }
        List<MoMMetrics> momList = new ArrayList<>();
        for (PlayerMetrics pm : metrics) {
            double score = 0.0;
            score += pm.runs * 1.0;
            score += pm.wickets * 30.0;
            score += pm.sixes * 3.0;
            score += pm.fours * 1.0;
            if (pm.ballsFaced > 0) score += pm.strikeRate / 10.0;
            if (pm.ballsBowled > 0) {
                double economyBonus = Math.max(0.0, 6.0 - pm.economy) * 5.0;
                score += economyBonus;
            }
            MoMMetrics m = new MoMMetrics();
            m.pm = pm;
            m.score = score;
            momList.add(m);
        }

        MoMMetrics top = momList.stream().max(Comparator.comparingDouble(x -> x.score)).orElse(null);

        if (top != null) {
            double bestScore = top.score;
            List<MoMMetrics> tied = momList.stream().filter(m -> Math.abs(m.score - bestScore) < 1e-6).collect(Collectors.toList());
            Player chosen = null;
            if (tied.size() == 1) {
                chosen = playerRepo.findById(top.pm.playerId).orElse(null);
            } else {
                // prefer player from winning team
                if (match.getWinnerTeam() != null) {
                    for (MoMMetrics m : tied) {
                        Player p = playerRepo.findById(m.pm.playerId).orElse(null);
                        if (p != null && p.getTeam() != null && p.getTeam().getId().equals(match.getWinnerTeam().getId())) {
                            chosen = p;
                            break;
                        }
                    }
                }
                if (chosen == null) {
                    tied.sort((a,b) -> {
                        if (a.pm.wickets != b.pm.wickets) return Integer.compare(b.pm.wickets, a.pm.wickets);
                        return Integer.compare(b.pm.runs, a.pm.runs);
                    });
                    chosen = playerRepo.findById(tied.get(0).pm.playerId).orElse(null);
                }
            }

            if (bestBat != null) match.setBestBatsman(playerRepo.findById(bestBat.playerId).orElse(null));
            if (bestBowl != null) match.setBestBowler(playerRepo.findById(bestBowl.playerId).orElse(null));
            match.setManOfMatch(chosen);
            match.setStatus("FINISHED");
            matchRepo.save(match);
        }
    }

    // ----------------- NEW: Tournament-level awards -----------------

    /**
     * Ensure tournament awards are computed and return DTO.
     * This is on-demand and idempotent.
     */
    @Transactional(readOnly = true)
    public TournamentAwardsDTO ensureAndGetTournamentAwards(Long tournamentId) {
        // compute on-demand
        return computeTournamentAwards(tournamentId);
    }

    /**
     * Compute tournament awards by aggregating data across all matches in the tournament.
     * This is a full-scan approach (reads all matches and all balls). Swap to incremental
     * if performance becomes an issue.
     */
    @Transactional
    public TournamentAwardsDTO computeTournamentAwards(Long tournamentId) {
        // gather all matches for the tournament
        List<Match> matches = matchRepo.findByTournament_Id(tournamentId);
        if (matches == null || matches.isEmpty()) {
            TournamentAwardsDTO empty = new TournamentAwardsDTO();
            empty.tournamentId = tournamentId;
            empty.topBatsmen = Collections.emptyList();
            empty.topBowlers = Collections.emptyList();
            return empty;
        }

        // aggregate per player across all matches
        class Agg {
            long playerId;
            int runs = 0;
            int ballsFaced = 0;
            int fours = 0;
            int sixes = 0;
            int wickets = 0;
            int runsConceded = 0;
            int ballsBowled = 0;
            Map<Long, Integer> inningsRuns = new HashMap<>(); // key: inningsId (global unique)
            int pomCount = 0; // player-of-match count across matches
        }

        Map<Long, Agg> map = new HashMap<>();

        java.util.function.Function<Player, Agg> getAgg = (player) -> {
            if (player == null) return null;
            return map.computeIfAbsent(player.getId(), id -> {
                Agg a = new Agg();
                a.playerId = id;
                return a;
            });
        };

        // For each match, collect balls and optional man-of-match
        for (Match match : matches) {
            Long matchId = match.getId();
            // count man of match
            if (match.getManOfMatch() != null) {
                Agg pomAgg = getAgg.apply(match.getManOfMatch());
                if (pomAgg != null) pomAgg.pomCount += 1;
            }

            List<CricketBall> balls = cricketBallInterface.findByMatch_Id(matchId);
            if (balls == null || balls.isEmpty()) continue;

            for (CricketBall b : balls) {
                Long inningsId = b.getInnings() == null ? null : b.getInnings().getId();

                if (b.getBatsman() != null) {
                    Agg a = getAgg.apply(b.getBatsman());
                    int r = (b.getRuns() == null ? 0 : b.getRuns());
                    a.runs += r;
                    if (Boolean.TRUE.equals(b.getLegalDelivery())) a.ballsFaced += 1;
                    if (Boolean.TRUE.equals(b.getIsFour())) a.fours += 1;
                    if (Boolean.TRUE.equals(b.getIsSix())) a.sixes += 1;
                    if (inningsId != null) {
                        a.inningsRuns.put(inningsId, a.inningsRuns.getOrDefault(inningsId, 0) + r);
                    }
                }

                if (b.getBowler() != null) {
                    Agg a = getAgg.apply(b.getBowler());
                    int runsConcededThisBall = (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra());
                    a.runsConceded += runsConcededThisBall;
                    if (Boolean.TRUE.equals(b.getLegalDelivery())) a.ballsBowled += 1;

                    String d = b.getDismissalType();
                    if (d != null) {
                        d = d.toLowerCase();
                        if (d.equals("bowled") || d.equals("caught") || d.equals("lbw") || d.equals("stumped") || d.equals("hit-wicket")) {
                            a.wickets += 1;
                        }
                    }
                }
            }
        }

        // compute PlayerMetrics across tournament
        class PlayerMetrics {
            Long playerId;
            int runs;
            int ballsFaced;
            double strikeRate;
            int highestInnings;
            int fours;
            int sixes;
            int wickets;
            int runsConceded;
            int ballsBowled;
            double economy;
            double bowlingAverage;
            int pomCount;
        }

        List<PlayerMetrics> metrics = new ArrayList<>();
        for (Agg a : map.values()) {
            PlayerMetrics pm = new PlayerMetrics();
            pm.playerId = a.playerId;
            pm.runs = a.runs;
            pm.ballsFaced = a.ballsFaced;
            pm.fours = a.fours;
            pm.sixes = a.sixes;
            pm.wickets = a.wickets;
            pm.runsConceded = a.runsConceded;
            pm.ballsBowled = a.ballsBowled;
            pm.highestInnings = a.inningsRuns.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            pm.pomCount = a.pomCount;

            if (pm.ballsFaced > 0) pm.strikeRate = (double) pm.runs * 100.0 / pm.ballsFaced;
            else pm.strikeRate = 0.0;

            if (pm.ballsBowled > 0) {
                double overs = pm.ballsBowled / 6.0;
                pm.economy = pm.runsConceded / overs;
            } else pm.economy = Double.POSITIVE_INFINITY;

            if (pm.wickets > 0) pm.bowlingAverage = (double) pm.runsConceded / pm.wickets;
            else pm.bowlingAverage = Double.POSITIVE_INFINITY;

            metrics.add(pm);
        }

        TournamentAwardsDTO dto = new TournamentAwardsDTO();
        dto.tournamentId = tournamentId;

        // Top batsmen (top 3)
        // Top batsmen mapping (keep sorting as you have)
        List<PlayerStatDTO> topBatsmen = metrics.stream()
                .filter(m -> m.ballsFaced > 0)
                .sorted(Comparator.comparingInt((PlayerMetrics m) -> m.runs).reversed()
                        .thenComparingInt(m -> m.highestInnings).reversed()
                        .thenComparingDouble(m -> m.strikeRate).reversed())
                .limit(3)
                .map(m -> {
                    Player p = playerRepo.findById(m.playerId).orElse(null);
                    PlayerStatDTO ps = new PlayerStatDTO();
                    ps.playerId = m.playerId;
                    ps.playerName = p != null ? p.getName() : "Unknown";

                    // batting fields
                    ps.runs = m.runs;
                    ps.ballsFaced = m.ballsFaced;
                    ps.fours = m.fours;
                    ps.sixes = m.sixes;

                    // bowling fields (may be zero)
                    ps.wickets = m.wickets;
                    ps.runsConceded = m.runsConceded;
                    ps.ballsBowled = m.ballsBowled;
                    if (m.ballsBowled > 0) {
                        double overs = m.ballsBowled / 6.0;
                        ps.economy = overs > 0 ? (double) m.runsConceded / overs : null;
                    } else {
                        ps.economy = null;
                    }
                    ps.bowlingAverage = m.wickets > 0 ? (double) m.runsConceded / m.wickets : null;

                    ps.pomCount = m.pomCount;
                    ps.compositeScore = 0.0; // will be attached later (as in your original flow)
                    return ps;
                }).collect(Collectors.toList());

        dto.topBatsmen = topBatsmen;
        if (!topBatsmen.isEmpty()) {
            dto.bestBatsmanId = topBatsmen.get(0).playerId;
            dto.bestBatsmanName = topBatsmen.get(0).playerName;
            dto.bestBatsmanRuns = topBatsmen.get(0).runs;
        }
        List<PlayerStatDTO> topBowlers = metrics.stream()
                .filter(m -> m.ballsBowled > 0)
                .sorted(Comparator.comparingInt((PlayerMetrics m) -> m.wickets).reversed()
                        .thenComparingDouble(m -> m.economy)
                        .thenComparingDouble(m -> m.bowlingAverage))
                .limit(3)
                .map(m -> {
                    Player p = playerRepo.findById(m.playerId).orElse(null);
                    PlayerStatDTO ps = new PlayerStatDTO();
                    ps.playerId = m.playerId;
                    ps.playerName = p != null ? p.getName() : "Unknown";

                    // batting
                    ps.runs = m.runs;
                    ps.ballsFaced = m.ballsFaced;
                    ps.fours = m.fours;
                    ps.sixes = m.sixes;

                    // bowling
                    ps.wickets = m.wickets;
                    ps.runsConceded = m.runsConceded;
                    ps.ballsBowled = m.ballsBowled;
                    if (m.ballsBowled > 0) {
                        double overs = m.ballsBowled / 6.0;
                        ps.economy = overs > 0 ? (double) m.runsConceded / overs : null;
                    } else {
                        ps.economy = null;
                    }
                    ps.bowlingAverage = m.wickets > 0 ? (double) m.runsConceded / m.wickets : null;

                    ps.pomCount = m.pomCount;
                    ps.compositeScore = 0.0; // to be filled later
                    return ps;
                }).collect(Collectors.toList());

        dto.topBowlers = topBowlers;
        if (!topBowlers.isEmpty()) {
            dto.bestBowlerId = topBowlers.get(0).playerId;
            dto.bestBowlerName = topBowlers.get(0).playerName;
            dto.bestBowlerWickets = topBowlers.get(0).wickets;
        }

        // Compute Man of Tournament using a composite score
        // weights are configurable - change as you wish
        Map<Long, Double> composite = new HashMap<>();
        for (PlayerMetrics m : metrics) {
            double score = 0.0;
            score += m.runs * 1.0;
            score += m.wickets * 20.0;
            score += m.sixes * 3.0;
            score += m.fours * 1.0;
            score += m.pomCount * 50.0;
            if (m.ballsFaced > 0) score += m.strikeRate / 10.0;
            if (m.ballsBowled > 0) {
                double econBonus = Math.max(0.0, 6.0 - m.economy) * 5.0;
                score += econBonus;
            }
            composite.put(m.playerId, score);
        }

        Optional<Map.Entry<Long, Double>> best = composite.entrySet().stream().max(Map.Entry.comparingByValue());
        if (best.isPresent()) {
            Long bestPlayerId = best.get().getKey();
            Player p = playerRepo.findById(bestPlayerId).orElse(null);
            dto.manOfTournamentId = bestPlayerId;
            dto.manOfTournamentName = p != null ? p.getName() : "Unknown";
        }

        for (PlayerStatDTO ps : dto.topBatsmen) {
            ps.compositeScore = composite.getOrDefault(ps.playerId, 0.0);
        }
        for (PlayerStatDTO ps : dto.topBowlers) {
            ps.compositeScore = composite.getOrDefault(ps.playerId, 0.0);
        }


        return dto;
    }

    public AwardsDTO ensureAndGetAwards(Long matchId) {
        Match m = matchRepo.findById(matchId).orElseThrow();
        // If awards not set on match, compute
        if (m.getManOfMatch() == null || m.getBestBatsman() == null || m.getBestBowler() == null) {
            computeMatchAwards(matchId); // your existing method; will set match fields
            m = matchRepo.findById(matchId).orElseThrow();
        }
        AwardsDTO dto = new AwardsDTO();
        dto.matchId = matchId;
        if (m.getManOfMatch()!=null) { dto.manOfMatchId = m.getManOfMatch().getId(); dto.manOfMatchName = m.getManOfMatch().getName(); }
        if (m.getBestBatsman()!=null) { dto.bestBatsmanId = m.getBestBatsman().getId(); dto.bestBatsmanName = m.getBestBatsman().getName(); }
        if (m.getBestBowler()!=null) { dto.bestBowlerId = m.getBestBowler().getId(); dto.bestBowlerName = m.getBestBowler().getName(); }
        return dto;
    }
}
