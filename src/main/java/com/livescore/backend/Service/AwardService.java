package com.livescore.backend.Service;

import com.livescore.backend.DTO.AwardsDTO;
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
            // track per-innings runs to compute highest innings
            Map<Long, Integer> inningsRuns = new HashMap<>();
        }

        Map<Long, Agg> map = new HashMap<>();

        // helper
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

            // fielder credits are stored in ball as fielder/outPlayer if needed
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
            double bowlingAverage; // runsConceded / wickets
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

            if (pm.ballsFaced > 0) {
                // strike rate = runs * 100 / ballsFaced
                pm.strikeRate = (double) pm.runs * 100.0 / pm.ballsFaced;
            } else pm.strikeRate = 0.0;

            if (pm.ballsBowled > 0) {
                double overs = pm.ballsBowled / 6.0;
                pm.economy = pm.runsConceded / overs;
            } else pm.economy = Double.POSITIVE_INFINITY;

            if (pm.wickets > 0) {
                pm.bowlingAverage = (double) pm.runsConceded / pm.wickets;
            } else pm.bowlingAverage = Double.POSITIVE_INFINITY;

            metrics.add(pm);
        }

        //  --- Best Batsman: runs desc, tie -> highestInnings desc -> strikeRate desc
        PlayerMetrics bestBat = metrics.stream()
                .filter(m -> m.ballsFaced > 0) // optional: ensure batted
                .max(Comparator.comparingInt((PlayerMetrics m) -> m.runs)
                        .thenComparingInt(m -> m.highestInnings)
                        .thenComparingDouble(m -> m.strikeRate))
                .orElse(null);

        // --- Best Bowler: wickets desc, tie -> economy asc, tie -> bowlingAverage asc
        PlayerMetrics bestBowl = metrics.stream()
                .filter(m -> m.ballsBowled > 0)
                .max(Comparator.comparingInt((PlayerMetrics m) -> m.wickets)
                        .thenComparingDouble(m -> -m.economy) // trick: we need min economy -> sort desc of -economy
                        .thenComparingDouble(m -> -m.bowlingAverage)) // min average -> desc of -average
                .orElse(null);

        // The above comparator uses max() and reverses for economy/average ties; simpler to use custom comparator:
        Comparator<PlayerMetrics> bowlComparator = (x,y) -> {
            if (x.wickets != y.wickets) return Integer.compare(x.wickets, y.wickets);
            if (Double.compare(x.economy, y.economy) != 0) return Double.compare(y.economy, x.economy) * -1; // ensure lower economy wins in max
            return Double.compare(y.bowlingAverage, x.bowlingAverage) * -1;
        };

        // Simpler safer approach: calculate manually
        bestBowl = null;
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

        // --- Man of the Match: composite metric
        // Weights (tune as needed):
        // runWeight = 1
        // wicketWeight = 30 (wickets are very valuable)
        // sixWeight = 3, fourWeight = 1
        // strikeRateBonus = strikeRate / 10
        // economyBonus = if bowled, (6.0 - economy) * 5 (lower economy yields positive)
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
                // economy smaller => bigger positive bonus
                double economyBonus = Math.max(0.0, 6.0 - pm.economy) * 5.0;
                score += economyBonus;
            }
            MoMMetrics m = new MoMMetrics();
            m.pm = pm;
            m.score = score;
            momList.add(m);
        }

        // sort by score desc
        MoMMetrics top = momList.stream().max(Comparator.comparingDouble(x -> x.score)).orElse(null);

        // tiebreakers: if tie choose player from winning team, else wickets then runs
        if (top != null) {
            // find ties within small epsilon
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
                    // next tiebreaker: highest wickets then runs
                    tied.sort((a,b) -> {
                        if (a.pm.wickets != b.pm.wickets) return Integer.compare(b.pm.wickets, a.pm.wickets);
                        return Integer.compare(b.pm.runs, a.pm.runs);
                    });
                    chosen = playerRepo.findById(tied.get(0).pm.playerId).orElse(null);
                }
            }

            // persist results on match
            if (bestBat != null) match.setBestBatsman(playerRepo.findById(bestBat.playerId).orElse(null));
            if (bestBowl != null) match.setBestBowler(playerRepo.findById(bestBowl.playerId).orElse(null));
            match.setManOfMatch(chosen);
            match.setStatus("FINISHED");
            matchRepo.save(match);
        }
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
