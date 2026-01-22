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
import com.livescore.backend.Util.CricketRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
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
    private StatsService statsService;

    // ----------------- MATCH-LEVEL AWARDS -----------------
    @Transactional
    public void computeMatchAwards(Long matchId) {
        if (matchId == null) {
            return;
        }
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) {
            return;
        }
        List<CricketBall> balls = cricketBallInterface.findByMatch_Id(matchId);
        if (balls == null || balls.isEmpty()) {
            // nothing to compute
            return;
        }

        // Agg per player for this match
        class Agg {
            long playerId;
            int runs = 0;
            int ballsFaced = 0;
            int fours = 0;
            int sixes = 0;
            int wickets = 0;
            int runsConceded = 0;
            int ballsBowled = 0;
            Map<Long, Integer> inningsRuns = new HashMap<>();
            int pomCount = 0; // not used per match, but keep for uniformity
        }

        Map<Long, Agg> map = new HashMap<>();

        Function<Player, Agg> getAgg = (player) -> {
            if (player == null) return null;
            return map.computeIfAbsent(player.getId(), id -> {
                Agg a = new Agg();
                a.playerId = id;
                return a;
            });
        };

        for (CricketBall b : balls) {
            Long inningsId = (b.getInnings() == null) ? null : b.getInnings().getId();

            // Batsman contributions
            if (b.getBatsman() != null) {
                Agg a = getAgg.apply(b.getBatsman());
                if (a != null) {
                    int r = (b.getRuns() == null ? 0 : b.getRuns());
                    a.runs += r;
                    if (CricketRules.isBallFaced(b)) a.ballsFaced += 1;
                    if (Boolean.TRUE.equals(b.getIsFour())) a.fours += 1;
                    if (Boolean.TRUE.equals(b.getIsSix())) a.sixes += 1;
                    if (inningsId != null) {
                        a.inningsRuns.put(inningsId, a.inningsRuns.getOrDefault(inningsId, 0) + r);
                    }
                }
            }

            // Bowler contributions
            if (b.getBowler() != null) {
                Agg a = getAgg.apply(b.getBowler());
                if (a != null) {
                    a.runsConceded += CricketRules.runsConcededThisBall(b);
                    if (Boolean.TRUE.equals(b.getLegalDelivery())) a.ballsBowled += 1;

                    String d = b.getDismissalType();
                    if (d != null) {
                        if (CricketRules.isBowlerCreditedWicket(d)) a.wickets += 1;
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
            int pomCount; // match-level pom (not used here)
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

            if (pm.ballsFaced > 0) {
                pm.strikeRate = ((double) pm.runs * 100.0) / (double) pm.ballsFaced;
            } else pm.strikeRate = 0.0;

            if (pm.ballsBowled > 0) {
                double overs = pm.ballsBowled / 6.0;
                pm.economy = overs > 0 ? ((double) pm.runsConceded) / overs : Double.POSITIVE_INFINITY;
            } else pm.economy = Double.POSITIVE_INFINITY;

            if (pm.wickets > 0) {
                pm.bowlingAverage = ((double) pm.runsConceded) / pm.wickets;
            } else pm.bowlingAverage = Double.POSITIVE_INFINITY;

            metrics.add(pm);
        }

        // Best Batsman (deterministic): runs DESC, highestInnings DESC, strikeRate DESC
        Comparator<PlayerMetrics> batComparator = Comparator
                .comparingInt((PlayerMetrics m) -> m.runs)
                .thenComparingInt(m -> m.highestInnings)
                .thenComparingDouble(m -> m.strikeRate)
                .reversed(); // reverse all criteria to get descending order for each

        PlayerMetrics bestBat = metrics.stream()
                .filter(m -> m.ballsFaced > 0)
                .max(batComparator)
                .orElse(null);

        // Best Bowler: wickets DESC, economy ASC, bowlingAverage ASC
        Comparator<PlayerMetrics> bowlComparator = Comparator
                .comparingInt((PlayerMetrics m) -> m.wickets).reversed()
                .thenComparingDouble(m -> m.economy)
                .thenComparingDouble(m -> m.bowlingAverage);

        PlayerMetrics bestBowl = metrics.stream()
                .filter(m -> m.ballsBowled > 0)
                .max(bowlComparator)
                .orElse(null);

        // Man of Match - composite score (sane weights)
        class MoMMetrics {
            PlayerMetrics pm;
            double score;
        }
        List<MoMMetrics> momList = new ArrayList<>();
        for (PlayerMetrics pm : metrics) {
            double score = 0.0;
            // weights (tunable)
            score += pm.runs * 1.0;
            score += pm.wickets * 25.0;          // wicket valued highly
            score += pm.sixes * 2.0;
            score += pm.fours * 1.0;
            score += pm.pomCount * 15.0;         // match-level pom not typical here but kept

            // Strike rate: only consider if reasonable sample size
            if (pm.ballsFaced >= 10) {
                double cappedSr = Math.min(pm.strikeRate, 200.0);
                score += cappedSr / 20.0;        // SR impact limited to <= 10 points
            }

            // economy penalty: punish expensive bowling (only when bowled)
            if (pm.ballsBowled > 0 && Double.isFinite(pm.economy)) {
                double econPenalty = Math.max(0.0, pm.economy - 8.0) * 5.0;
                score -= econPenalty;
            }

            MoMMetrics m = new MoMMetrics();
            m.pm = pm;
            m.score = score;
            momList.add(m);
        }

        MoMMetrics top = momList.stream().max(Comparator.comparingDouble(x -> x.score)).orElse(null);

        if (top != null) {
            double bestScore = top.score;
            List<MoMMetrics> tied = momList.stream()
                    .filter(m -> Math.abs(m.score - bestScore) < 1e-6)
                    .collect(Collectors.toList());

            Player chosen = null;
            if (tied.size() == 1) {
                chosen = playerRepo.findActiveById(top.pm.playerId).orElse(null);
            } else {
                // try pick from winning team (if match winner known)
                if (match.getWinnerTeam() != null) {
                    for (MoMMetrics m : tied) {
                        Player p = playerRepo.findActiveById(m.pm.playerId).orElse(null);
                        if (p != null && p.getTeam() != null && match.getWinnerTeam().getId() != null
                                && p.getTeam().getId() != null
                                && p.getTeam().getId().equals(match.getWinnerTeam().getId())) {
                            chosen = p;
                            break;
                        }
                    }
                }

                // fallback deterministic tiebreaker: more wickets then more runs
                if (chosen == null) {
                    tied.sort((a, b) -> {
                        if (a.pm.wickets != b.pm.wickets) return Integer.compare(b.pm.wickets, a.pm.wickets);
                        if (a.pm.runs != b.pm.runs) return Integer.compare(b.pm.runs, a.pm.runs);
                        return Long.compare(a.pm.playerId, b.pm.playerId); // deterministic fallback
                    });
                    chosen = playerRepo.findActiveById(tied.get(0).pm.playerId).orElse(null);
                }
            }

            if (bestBat != null) {
                Player bestBatPlayer = playerRepo.findActiveById(bestBat.playerId).orElse(null);
                if (bestBatPlayer != null) match.setBestBatsman(bestBatPlayer);
            }
            if (bestBowl != null) {
                Player bestBowlPlayer = playerRepo.findActiveById(bestBowl.playerId).orElse(null);
                if (bestBowlPlayer != null) match.setBestBowler(bestBowlPlayer);
            }
            if (chosen != null) {
                match.setManOfMatch(chosen);
            }
            match.setStatus("FINISHED");
            matchRepo.save(match);
        }
    }

    // ----------------- TOURNAMENT-LEVEL AWARDS -----------------
    @Transactional(readOnly = true)
    public TournamentAwardsDTO ensureAndGetTournamentAwards(Long tournamentId) {
        return computeTournamentAwards(tournamentId);
    }

    @Transactional
    public TournamentAwardsDTO computeTournamentAwards(Long tournamentId) {
        // gather all matches for the tournament
        List<Match> matches = matchRepo.findByTournament_Id(tournamentId);
        TournamentAwardsDTO empty = new TournamentAwardsDTO();
        empty.tournamentId = tournamentId;
        empty.topBatsmen = Collections.emptyList();
        empty.topBowlers = Collections.emptyList();

        if (matches == null || matches.isEmpty()) {
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
            Map<Long, Integer> inningsRuns = new HashMap<>();
            int pomCount = 0;
        }

        Map<Long, Agg> map = new HashMap<>();

        Function<Player, Agg> getAgg = (player) -> {
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
                Long inningsId = (b.getInnings() == null) ? null : b.getInnings().getId();

                if (b.getBatsman() != null) {
                    Agg a = getAgg.apply(b.getBatsman());
                    if (a != null) {
                        int r = (b.getRuns() == null ? 0 : b.getRuns());
                        a.runs += r;
                        if (CricketRules.isBallFaced(b)) a.ballsFaced += 1;
                        if (Boolean.TRUE.equals(b.getIsFour())) a.fours += 1;
                        if (Boolean.TRUE.equals(b.getIsSix())) a.sixes += 1;
                        if (inningsId != null) {
                            a.inningsRuns.put(inningsId, a.inningsRuns.getOrDefault(inningsId, 0) + r);
                        }
                    }
                }

                if (b.getBowler() != null) {
                    Agg a = getAgg.apply(b.getBowler());
                    if (a != null) {
                        a.runsConceded += CricketRules.runsConcededThisBall(b);
                        if (Boolean.TRUE.equals(b.getLegalDelivery())) a.ballsBowled += 1;

                        String d = b.getDismissalType();
                        if (d != null) {
                            if (CricketRules.isBowlerCreditedWicket(d)) a.wickets += 1;
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
                pm.economy = overs > 0 ? (double) pm.runsConceded / overs : Double.POSITIVE_INFINITY;
            } else pm.economy = Double.POSITIVE_INFINITY;

            if (pm.wickets > 0) pm.bowlingAverage = (double) pm.runsConceded / pm.wickets;
            else pm.bowlingAverage = Double.POSITIVE_INFINITY;

            metrics.add(pm);
        }

        TournamentAwardsDTO dto = new TournamentAwardsDTO();
        dto.tournamentId = tournamentId;


        Comparator<PlayerMetrics> batComp = Comparator
                .comparingInt((PlayerMetrics m) -> m.runs).reversed()
                .thenComparing(Comparator.comparingInt((PlayerMetrics m) -> m.highestInnings).reversed())
                .thenComparing(Comparator.comparingDouble((PlayerMetrics m) -> m.strikeRate).reversed())

                .thenComparingLong(m -> m.playerId);

        List<PlayerStatDTO> topBatsmen = metrics.stream()
                .filter(m -> m.ballsFaced > 0) // or >=10 if you want to ignore very short innings
                .sorted(batComp)               // use the single, well-defined comparator
                .limit(5)
                .map(m -> {
                    Player p = playerRepo.findActiveById(m.playerId).orElse(null);
                    PlayerStatDTO ps = new PlayerStatDTO();
                    ps.playerId = m.playerId;
                    ps.playerName = p != null ? p.getName() : "Unknown";

                    ps.runs = m.runs;
                    ps.ballsFaced = m.ballsFaced;
                    ps.fours = m.fours;
                    ps.sixes = m.sixes;

                    ps.wickets = m.wickets;
                    ps.runsConceded = m.runsConceded;
                    ps.ballsBowled = m.ballsBowled;
                    if (m.ballsBowled > 0) {
                        double overs = ((double) m.ballsBowled) / 6.0;
                        ps.economy = overs > 0 ? ((double) m.runsConceded) / overs : null;
                    } else {
                        ps.economy = null;
                    }
                    ps.bowlingAverage = m.wickets > 0 ? (double) m.runsConceded / m.wickets : null;

                    ps.pomCount = m.pomCount;
                    ps.compositeScore = 0.0;
                    return ps;
                }).collect(Collectors.toList());

        dto.topBatsmen = topBatsmen;

        // highest scorer: purely maximum total runs
        PlayerMetrics highestScorer = metrics.stream()
                .max(Comparator.comparingInt((PlayerMetrics m) -> m.runs)
                        .thenComparingInt(m -> m.highestInnings)
                        .thenComparingLong(m -> m.playerId))
                .orElse(null);
        if (highestScorer != null) {
            Player p = playerRepo.findActiveById(highestScorer.playerId).orElse(null);
            dto.highestScorerId = highestScorer.playerId;
            dto.highestScorerName = p != null ? p.getName() : "Unknown";
            dto.highestRuns = highestScorer.runs;
        }

        if (!topBatsmen.isEmpty()) {
            dto.bestBatsmanId = topBatsmen.get(0).playerId;
            dto.bestBatsmanName = topBatsmen.get(0).playerName;
            dto.bestBatsmanRuns = topBatsmen.get(0).runs;
        }


        // Top bowlers (top 5) - wickets DESC, economy ASC, bowlingAverage ASC
        Comparator<PlayerMetrics> bowlComp = Comparator
                .comparingInt((PlayerMetrics m) -> m.wickets).reversed()
                .thenComparingDouble(m -> m.economy)
                .thenComparingDouble(m -> m.bowlingAverage);

        List<PlayerStatDTO> topBowlers = metrics.stream()
                .filter(m -> m.ballsBowled > 0)
                .sorted(bowlComp)
                .limit(5)
                .map(m -> {
                    Player p = playerRepo.findActiveById(m.playerId).orElse(null);
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

        // Compute Man of Tournament using composite score (same weights as match-level)
        Map<Long, Double> composite = new HashMap<>();
        for (PlayerMetrics m : metrics) {
            double score = 0;
            score += m.runs * 1.0;
            score += m.wickets * 25.0;
            score += m.sixes * 2.0;
            score += m.fours * 1.0;
            score += m.pomCount * 15.0;

            if (m.ballsFaced >= 10) {
                score += Math.min(m.strikeRate, 200.0) / 20.0;
            }

            if (m.ballsBowled > 0 && Double.isFinite(m.economy)) {
                score -= Math.max(0.0, m.economy - 8.0) * 5.0;
            }

            composite.put(m.playerId, score);
        }

        // choose man of tournament
        Optional<Map.Entry<Long, Double>> best = composite.entrySet().stream().max(Map.Entry.comparingByValue());
        if (best.isPresent()) {
            Long bestPlayerId = best.get().getKey();
            Player p = playerRepo.findActiveById(bestPlayerId).orElse(null);
            dto.manOfTournamentId = bestPlayerId;
            dto.manOfTournamentName = p != null ? p.getName() : "Unknown";
        }

        // attach composite scores to top lists for visibility
        for (PlayerStatDTO ps : dto.topBatsmen) {
            ps.compositeScore = composite.getOrDefault(ps.playerId, 0.0);
        }
        for (PlayerStatDTO ps : dto.topBowlers) {
            ps.compositeScore = composite.getOrDefault(ps.playerId, 0.0);
        }

        return dto;
    }

    // ----------------- MATCH AWARDS DTO HELPER -----------------
    public AwardsDTO ensureAndGetAwards(Long matchId) {
        AwardsDTO dto = new AwardsDTO();
        dto.matchId = matchId;
        if (matchId == null) {
            return dto;
        }

        Match m = matchRepo.findById(matchId).orElse(null);
        if (m == null) {
            return dto;
        }
        // If awards not set on match, compute
        if (m.getManOfMatch() == null || m.getBestBatsman() == null || m.getBestBowler() == null) {
            computeMatchAwards(matchId); // this will set match fields
            m = matchRepo.findById(matchId).orElse(null);
            if (m == null) {
                return dto;
            }
        }
        if (m.getManOfMatch() != null) {
            dto.manOfMatchId = m.getManOfMatch().getId();
            dto.manOfMatchName = m.getManOfMatch().getName();
        }
        if (m.getBestBatsman() != null) {
            dto.bestBatsmanId = m.getBestBatsman().getId();
            dto.bestBatsmanName = m.getBestBatsman().getName();
        }
        if (m.getBestBowler() != null) {
            dto.bestBowlerId = m.getBestBowler().getId();
            dto.bestBowlerName = m.getBestBowler().getName();
        }
        return dto;
    }
}
