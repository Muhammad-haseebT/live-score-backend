package com.livescore.backend.Service;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
    private TournamentInterface tournamentInterface;

    public ResponseEntity<?> getAllStats() {
        List<Stats> stats = statsInterface.findAll();
        return ResponseEntity.ok(stats);
    }

    public ResponseEntity<?> getStatsById(Long id) {
        Stats stats = statsInterface.findById(id).orElse(null);
        return ResponseEntity.ok(stats);
    }

    public ResponseEntity<?> createStats(Long playerId, Long tournamentId) {
        Stats stats=new Stats();
        stats.setPlayer(playerInterface.findById(playerId).orElse(null));

        Tournament t=tournamentInterface.findById(tournamentId).orElse(null);
        stats.setTournament(t);
        stats.setSportType(t.getSport());
        Stats savedStats = statsInterface.save(stats);
        return ResponseEntity.ok(savedStats);
    }
    public ResponseEntity<?> updateStats(Long playerId, Long tournamentId, Long matchId) {
        Stats existingStats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId)
                .orElseThrow(() -> new RuntimeException("Stats not found"));

        String sportName = existingStats.getSportType() != null
                ? existingStats.getSportType().getName()
                : "";

        if (sportName.equalsIgnoreCase("CRICKET")) {
            List<CricketBall> batsmanBalls = cricketBallInterface.findBatsmanBalls(tournamentId, playerId);
            List<CricketBall> bowlerBalls  = cricketBallInterface.findBowlerBalls(tournamentId, playerId);
            List<CricketBall> fielderBalls = cricketBallInterface.findFielderBalls(tournamentId, playerId);

            // --- BATTING ---
            // Exclude wides from balls faced (wides do not count as ball faced).
            // Treat no-ball depending on extraType; commonly no-ball does NOT count as legal ball but batsman may face it.
            // Here we *exclude* wides and no-balls from balls counted as "legal" deliveries if you want strictly legal balls.
            // If you want to include no-ball as ball faced, remove the no-ball filter.
            Predicate<CricketBall> isWide = cb -> {
                String et = cb.getExtra() == null ? null : cb.getExtraType();
                return et != null && et.toLowerCase().contains("wide");
            };
            Predicate<CricketBall> isNoBall = cb -> {
                String et = cb.getExtra() == null ? null : cb.getExtraType();
                return et != null && (et.toLowerCase().contains("no") || et.toLowerCase().contains("noball"));
            };

            // runs by batsman (includes runs field only; extras not added here)
            int totalRuns = batsmanBalls.stream()
                    .mapToInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)
                    .sum();

            // balls faced: exclude wides (and optionally no-balls if you prefer)
            long ballsFaced = batsmanBalls.stream()
                    .filter(cb -> !isWide.test(cb))   // exclude wides
                    // .filter(cb -> !isNoBall.test(cb)) // optionally exclude no-balls if you want
                    .count();

            // highest per-innings: group by innings id and sum runs per innings, take max
            Map<Long, Integer> runsPerInnings = batsmanBalls.stream()
                    .filter(cb -> cb.getInnings() != null && cb.getInnings().getId() != null)
                    .collect(Collectors.groupingBy(cb -> cb.getInnings().getId(),
                            Collectors.summingInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)));
            int highest = runsPerInnings.values().stream().mapToInt(Integer::intValue).max().orElse(0);

            // notOut: count innings where player faced at least one ball and never had a dismissal recorded in that innings
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

            // strikeRate: integer percent
            int strikeRate = (ballsFaced == 0) ? 0 : (int) Math.round(((double) totalRuns * 100.0) / (double) ballsFaced);

            // --- BOWLING ---
            // runs conceded: include runs + extras that count against bowler (wides, no-balls).
            int runsConceded = bowlerBalls.stream()
                    .mapToInt(cb -> {
                        int r = cb.getRuns() != null ? cb.getRuns() : 0;
                        String et = cb.getExtraType();
                        int extra = cb.getExtra() != null ? cb.getExtra() : 0;
                        if (et != null) {
                            String e = et.toLowerCase();
                            if (e.contains("wide") || e.contains("no") || e.contains("noball") || e.contains("nb")) {
                                return r + extra; // charged to bowler
                            } else {
                                return r; // byes/legbyes not charged to bowler
                            }
                        }
                        return r;
                    })
                    .sum();

            // wickets: only count dismissal types that are credited to bowler (not runouts)
            Set<String> bowlerWicketTypes = Set.of("bowled","lbw","caught","stumped","hitwicket","hit wicket","caught and bowled","caught&bowled");
            int wickets = (int) bowlerBalls.stream()
                    .filter(cb -> cb.getDismissalType() != null)
                    .filter(cb -> {
                        String dt = cb.getDismissalType().toLowerCase();
                        return bowlerWicketTypes.stream().anyMatch(dt::contains);
                    })
                    .count();

            // balls bowled: count legal deliveries (exclude wides and no-balls)
            int ballsBowled = (int) bowlerBalls.stream()
                    .filter(cb -> !isWide.test(cb) && !isNoBall.test(cb))
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

    public ResponseEntity<?> deleteStats(Long id) {
        Stats stats = statsInterface.findById(id).orElse(null);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        statsInterface.delete(stats);
        return ResponseEntity.ok().build();
    }

}
