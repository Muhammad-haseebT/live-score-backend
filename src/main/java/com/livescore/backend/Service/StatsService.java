package com.livescore.backend.Service;

import com.livescore.backend.DTO.*;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private MatchInterface matchRepo;
    @Autowired
    private CricketInningsInterface cricketInningsRepo;

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


            Predicate<CricketBall> isWide = cb -> {
                String et = cb.getExtra() == null ? null : cb.getExtraType();
                return et != null && et.toLowerCase().contains("wide");
            };

            Predicate<CricketBall> isNoBall = cb -> {
                String et = cb.getExtra() == null ? null : cb.getExtraType();
                return et != null && (et.toLowerCase().contains("no") || et.toLowerCase().contains("noball"));
            };


            int totalRuns = batsmanBalls.stream()
                    .mapToInt(cb -> cb.getRuns() != null ? cb.getRuns() : 0)
                    .sum();

            // balls faced: exclude wides (and optionally no-balls if you prefer)
            long ballsFaced = batsmanBalls.stream()
                    .filter(cb -> !isWide.test(cb))   // exclude wides
                     .filter(cb -> !isNoBall.test(cb))
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
    @Transactional
    public void updateTournamentStats(CricketBall ball) {
        if (ball == null) return;
        Match match = ball.getMatch();
        if (match == null || match.getTournament() == null) return;
        Tournament tournament = match.getTournament();

        // --- Batsman updates ---
        Player batsman = ball.getBatsman();
        if (batsman != null) {
            Stats batStats = statsInterface.findByTournamentIdAndPlayerId(tournament.getId(), batsman.getId())
                    .orElseGet(() -> {
                        Stats s = new Stats();
                        s.setTournament(tournament);
                        s.setPlayer(batsman);
                        s.setSportType(match.getTournament().getSport()); // adjust getter
                        // Initialize all numeric fields to 0 to prevent NullPointerException
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
                    });
            // runs off bat
            int runs = (ball.getRuns() == null ? 0 : ball.getRuns());
            batStats.setRuns(batStats.getRuns() + runs);

            // balls faced only increment on legal delivery where this player was batsman
            if (Boolean.TRUE.equals(ball.getLegalDelivery())) {
                batStats.setBallsFaced(batStats.getBallsFaced() + 1);
            }

            // boundaries
            if (Boolean.TRUE.equals(ball.getIsFour())) batStats.setFours(batStats.getFours() + 1);
            if (Boolean.TRUE.equals(ball.getIsSix())) batStats.setSixes(batStats.getSixes() + 1);


            if (runs > batStats.getHighest()) batStats.setHighest(runs);

            statsInterface.save(batStats);
        }


        Player bowler = ball.getBowler();
        if (bowler != null) {
            Stats bowlStats = statsInterface.findByTournamentIdAndPlayerId(tournament.getId(), bowler.getId())
                    .orElseGet(() -> {
                        Stats s = new Stats();
                        s.setTournament(tournament);
                        s.setPlayer(bowler);
                        s.setSportType(match.getTournament().getSport());
                        // Initialize all numeric fields to 0 to prevent NullPointerException
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
                    });

            int runsConcededThisBall = (ball.getRuns() == null ? 0 : ball.getRuns()) + (ball.getExtra() == null ? 0 : ball.getExtra());
            bowlStats.setRunsConceded(bowlStats.getRunsConceded() + runsConcededThisBall);

            // legal delivery -> counts as ball bowled
            if (Boolean.TRUE.equals(ball.getLegalDelivery())) {
                bowlStats.setBallsBowled(bowlStats.getBallsBowled() + 1);
            }

            // wicket credited to bowler? exclude runout as bowler wicket unless rules say otherwise
            String dismissal = ball.getDismissalType();
            if (dismissal != null) {
                String d = dismissal.toLowerCase();
                // credit these to bowler
                if (d.equals("bowled") || d.equals("caught") || d.equals("lbw") || d.equals("stumped") || d.equals("hit-wicket")) {
                    bowlStats.setWickets(bowlStats.getWickets() + 1);
                }
            }

            statsInterface.save(bowlStats);
        }
    }


    public ResponseEntity<MatchScorecardDTO> getMatchScorecard(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if(match==null){
            return ResponseEntity.notFound().build();

        }
        MatchScorecardDTO dto = new MatchScorecardDTO();
        dto.matchId = matchId;
        dto.status = match.getStatus();

        // find innings 1 and 2
        CricketInnings inn1 = cricketInningsRepo.findByMatchIdAndNo(matchId, 1);
        if (inn1 != null) dto.firstInnings = buildInningsDTO(inn1);
        CricketInnings inn2 = cricketInningsRepo.findByMatchIdAndNo(matchId, 2);
        if (inn2 != null) dto.secondInnings = buildInningsDTO(inn2);

        return ResponseEntity.ok(dto);
    }
    private InningsDTO buildInningsDTO(CricketInnings innings) {
        InningsDTO d = new InningsDTO();
        if (innings == null) return d;

        d.inningsId = innings.getId();
        d.teamId = innings.getTeam() == null ? null : innings.getTeam().getId();
        d.teamName = innings.getTeam() == null ? null : innings.getTeam().getName();

        List<CricketBall> balls = cricketBallInterface.findByMatch_IdAndInnings_Id(
                innings.getMatch() == null ? null : innings.getMatch().getId(),
                innings.getId()
        );

        if (balls == null) balls = List.of();

        // ---------- TOTALS ----------
        int totalRuns = balls.stream()
                .mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra()))
                .sum();
        d.totalRuns = totalRuns;

        int extras = balls.stream().mapToInt(b -> b.getExtra() == null ? 0 : b.getExtra()).sum();
        d.extras = extras;

        int wickets = (int) balls.stream()
                .filter(b -> b.getDismissalType() != null && !b.getDismissalType().trim().isEmpty())
                .count();
        d.wickets = wickets;

        int legalBallsTotal = (int) balls.stream()
                .filter(b -> Boolean.TRUE.equals(b.getLegalDelivery()))
                .count();
        d.totalBalls = legalBallsTotal;
        d.oversString = formatOvers(legalBallsTotal);

        // ---------- balls list (ball-by-ball) ----------
        d.balls = balls.stream().map(b -> {
            BallDTO bd = new BallDTO();
            bd.id = b.getId();
            bd.overNumber = b.getOverNumber();
            bd.ballNumber = b.getBallNumber();
            bd.batsmanId = b.getBatsman() == null ? null : b.getBatsman().getId();
            bd.bowlerId = b.getBowler() == null ? null : b.getBowler().getId();
            bd.runs = b.getRuns();
            bd.extra = b.getExtra();
            bd.extraType = b.getExtraType();
            bd.dismissalType = b.getDismissalType();
            bd.fielderId = b.getFielder() == null ? null : b.getFielder().getId();
            bd.mediaId = b.getMedia() == null ? null : b.getMedia().getId();
            return bd;
        }).collect(Collectors.toList());

        // ---------- PLAYER-WISE BATTING ----------
        Map<Long, List<CricketBall>> ballsByBatsman = balls.stream()
                .filter(b -> b.getBatsman() != null && b.getBatsman().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getBatsman().getId()));

        d.battingScores = ballsByBatsman.entrySet().stream()
                .map(entry -> {
                    Long playerId = entry.getKey();
                    List<CricketBall> pBalls = entry.getValue();
                    Player p = pBalls.get(0).getBatsman();

                    BattingScoreDTO bs = new BattingScoreDTO();
                    bs.playerId = playerId;
                    bs.playerName = p == null ? null : p.getName();

                    bs.runs = pBalls.stream().mapToInt(b -> b.getRuns() == null ? 0 : b.getRuns()).sum();
                    bs.balls = (int) pBalls.stream().filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count();
                    bs.fours = (int) pBalls.stream().filter(b -> Boolean.TRUE.equals(b.getIsFour())).count();
                    bs.sixes = (int) pBalls.stream().filter(b -> Boolean.TRUE.equals(b.getIsSix())).count();
                    bs.strikeRate = bs.balls == 0 ? 0.0 : roundTo2((double) bs.runs * 100.0 / bs.balls);
                    boolean isOut = pBalls.stream().anyMatch(b -> b.getDismissalType() != null && !b.getDismissalType().trim().isEmpty());
                    bs.notOut = !isOut;
                    return bs;
                })
                // optionally sort by batting order if you have it; fallback by runs desc
                .sorted((a, b) -> Integer.compare(b.runs, a.runs))
                .collect(Collectors.toList());

        if (d.battingScores == null) d.battingScores = List.of();

        // ---------- BOWLING SCORECARD (OPPOSITION) ----------
        Map<Long, List<CricketBall>> ballsByBowler = balls.stream()
                .filter(b -> b.getBowler() != null && b.getBowler().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getBowler().getId()));

        d.bowlingScores = ballsByBowler.entrySet().stream()
                .map(entry -> {
                    Long bowlerId = entry.getKey();
                    List<CricketBall> bowlerBalls = entry.getValue();
                    Player bowler = bowlerBalls.get(0).getBowler();

                    BowlingScoreDTO bw = new BowlingScoreDTO();
                    bw.playerId = bowlerId;
                    bw.playerName = bowler == null ? null : bowler.getName();

                    long legalBallsByBowler = bowlerBalls.stream().filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count();
                    bw.balls = (int) legalBallsByBowler;
                    bw.overs = formatOvers((int) legalBallsByBowler);

                    bw.runsConceded = bowlerBalls.stream().mapToInt(b -> {
                        int r = b.getRuns() == null ? 0 : b.getRuns();
                        int e = b.getExtra() == null ? 0 : b.getExtra();
                        String et = b.getExtraType();
                        if (et != null) {
                            String t = et.toLowerCase();
                            if (t.contains("wide") || t.contains("no")) {
                                return r + e;
                            }
                        }
                        return r;
                    }).sum();

                    bw.wickets = (int) bowlerBalls.stream()
                            .filter(b -> b.getDismissalType() != null && !b.getDismissalType().trim().isEmpty())
                            .filter(b -> {
                                String dType = b.getDismissalType().toLowerCase();
                                return dType.contains("bowled") || dType.contains("caught") || dType.contains("lbw")
                                        || dType.contains("stumped") || dType.contains("hit wicket") || dType.contains("hitwicket");
                            })
                            .count();

                    bw.maidens = calculateMaidens(bowlerBalls);

                    bw.economy = legalBallsByBowler == 0 ? 0.0 : roundTo2((double) bw.runsConceded * 6.0 / (double) legalBallsByBowler);

                    return bw;
                })
                .sorted((a, b) -> {
                    if (a.playerName == null) return -1;
                    if (b.playerName == null) return 1;
                    return a.playerName.compareToIgnoreCase(b.playerName);
                })
                .collect(Collectors.toList());

        if (d.bowlingScores == null) d.bowlingScores = List.of();

        return d;
    }


    private int calculateMaidens(List<CricketBall> balls) {

        Map<Integer, List<CricketBall>> overs =
                balls.stream()
                        .filter(b -> b.getOverNumber() != null)
                        .collect(Collectors.groupingBy(CricketBall::getOverNumber));

        int maidens = 0;

        for (List<CricketBall> overBalls : overs.values()) {

            boolean hasRun = overBalls.stream().anyMatch(b -> {
                int r = b.getRuns() == null ? 0 : b.getRuns();
                int e = b.getExtra() == null ? 0 : b.getExtra();
                return (r + e) > 0;
            });

            boolean sixLegal = overBalls.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getLegalDelivery()))
                    .count() == 6;

            if (!hasRun && sixLegal) {
                maidens++;
            }
        }

        return maidens;
    }


    private String formatOvers(int legalBalls) {
        int overs = legalBalls / 6;
        int ballsRem = legalBalls % 6;
        return overs + "." + ballsRem;
    }


    public PlayerStatsDTO getPlayerTournamentStats(Long playerId, Long tournamentId, Long matchId) {
        Player p = playerInterface.findById(playerId).orElseThrow();
        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.playerId = playerId;
        dto.playerName = p.getName();

        // BATTING: aggregate batsman balls in tournament or optional match
        List<CricketBall> batsmanBalls = (matchId != null)
                ? cricketBallInterface.findByBatsmanIdAndMatchId(playerId, matchId)
                : cricketBallInterface.findBatsmanBallsByTournamentAndPlayer(tournamentId, playerId);

        int runs = batsmanBalls.stream().mapToInt(b -> b.getRuns()==null?0:b.getRuns()).sum();
        int ballsFaced = (int) batsmanBalls.stream().filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count(); // exclude wides/no-balls for balls faced

        int fours = (int) batsmanBalls.stream().filter(b -> Boolean.TRUE.equals(b.getIsFour())).count();
        int sixes = (int) batsmanBalls.stream().filter(b -> Boolean.TRUE.equals(b.getIsSix())).count();
        Map<Long,Integer> runsPerInnings = batsmanBalls.stream()
                .filter(b -> b.getInnings() != null)
                .collect(Collectors.groupingBy(b -> b.getInnings().getId(), Collectors.summingInt(b -> b.getRuns()==null?0:b.getRuns())));
        int highest = runsPerInnings.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // notOut (count innings where player batted and has no dismissal recorded)
        Set<Long> inningsWithBalls = batsmanBalls.stream().filter(b -> b.getInnings()!=null).map(b -> b.getInnings().getId()).collect(Collectors.toSet());
        Set<Long> inningsWhereOut = batsmanBalls.stream().filter(b -> b.getDismissalType()!=null && b.getInnings()!=null).map(b -> b.getInnings().getId()).collect(Collectors.toSet());
        int notOut = (int) inningsWithBalls.stream().filter(id -> !inningsWhereOut.contains(id)).count();

        dto.runs = runs;
        dto.ballsFaced = ballsFaced;
        dto.fours = fours;
        dto.sixes = sixes;
        dto.highest = highest;
        dto.notOut = notOut;
        dto.strikeRate = (ballsFaced == 0) ? 0.0 : roundTo2((double) runs * 100.0 / (double) ballsFaced);

        // BOWLING
        List<CricketBall> bowlerBalls = (matchId != null)
                ? cricketBallInterface.findByBowlerIdAndMatchId(playerId, matchId)
                : cricketBallInterface.findBowlerBallsByTournamentAndPlayer(tournamentId, playerId);

        int runsConceded = bowlerBalls.stream().mapToInt(b -> {
            int r = b.getRuns()==null?0:b.getRuns();
            int ex = b.getExtra()==null?0:b.getExtra();
            String et = b.getExtraType();
            if (et != null) {
                String e = et.toLowerCase();
                if (e.contains("wide") || e.contains("no") || e.contains("noball") || e.contains("nb")) return r + ex;
            }
            return r;
        }).sum();

        int ballsBowled = (int) bowlerBalls.stream().filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count();
        int wickets = (int) bowlerBalls.stream()
                .filter(b -> b.getDismissalType()!=null)
                .filter(b -> {
                    String dt = b.getDismissalType().toLowerCase();
                    return dt.contains("bowled") || dt.contains("lbw") || dt.contains("stumped") || dt.contains("caught") || dt.contains("hit wicket") || dt.contains("hitwicket");
                }).count();

        dto.wickets = wickets;
        dto.ballsBowled = ballsBowled;
        dto.runsConceded = runsConceded;
        dto.economy = (ballsBowled == 0) ? 0 : roundTo2((double) runsConceded * 6.0 / (double) ballsBowled);
        dto.bowlingAverage = (wickets == 0) ? 0 : roundTo2((double) runsConceded / (double) wickets);



        return dto;
    }












    private double roundTo2(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) return val;
        BigDecimal bd = BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
