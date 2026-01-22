package com.livescore.backend.Service;

import com.livescore.backend.DTO.*;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.CricketRules;
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

    @Cacheable(cacheNames = "tournamentPlayerStats", key = "T(String).valueOf(#tournamentId).concat(':').concat(T(String).valueOf(#playerId))")
    public TournamentPlayerStatsDTO getTournamentPlayerStatsDto(Long tournamentId, Long playerId) {
        TournamentPlayerStatsDTO dto = new TournamentPlayerStatsDTO();
        dto.tournamentId = tournamentId;
        dto.playerId = playerId;

        if (tournamentId == null || playerId == null) {
            return dto;
        }

        Player p = playerInterface.findActiveById(playerId).orElse(null);
        dto.playerName = p != null ? p.getName() : "Unknown";

        dto.matchesPlayed = cricketBallInterface.countMatchesPlayedInTournament(playerId, tournamentId);
        dto.manOfMatchCount = (int) matchRepo.countManOfMatchForPlayer(tournamentId, playerId);

        // Batting aggregates
        Object[] batting = cricketBallInterface.getBattingAggregate(playerId, tournamentId);
        if (batting != null && batting.length >= 4) {
            dto.totalRuns = batting[0] != null ? ((Number) batting[0]).intValue() : 0;
            dto.ballsFaced = batting[1] != null ? ((Number) batting[1]).intValue() : 0;
            dto.fours = batting[2] != null ? ((Number) batting[2]).intValue() : 0;
            dto.sixes = batting[3] != null ? ((Number) batting[3]).intValue() : 0;
        }

        dto.strikeRate = dto.ballsFaced == 0 ? 0.0 : roundTo2(dto.totalRuns * 100.0 / (double) dto.ballsFaced);

        Integer highest = cricketBallInterface
                .getRunsPerInningsDesc(playerId, tournamentId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(0);
        dto.highestScore = highest == null ? 0 : highest;

        Integer notOut = cricketBallInterface.countNotOutInnings(playerId, tournamentId);
        dto.notOuts = notOut == null ? 0 : notOut;

        int inningsBatted = cricketBallInterface.countDistinctInningsBatted(playerId, tournamentId);
        int outs = Math.max(0, inningsBatted - dto.notOuts);
        dto.battingAverage = outs == 0 ? (double) dto.totalRuns : roundTo2((double) dto.totalRuns / (double) outs);

        // Bowling aggregates
        Object[] bowling = cricketBallInterface.getBowlingAggregate(playerId, tournamentId);
        if (bowling != null && bowling.length >= 3) {
            dto.runsConceded = bowling[0] != null ? ((Number) bowling[0]).intValue() : 0;
            dto.ballsBowled = bowling[1] != null ? ((Number) bowling[1]).intValue() : 0;
            dto.totalWickets = bowling[2] != null ? ((Number) bowling[2]).intValue() : 0;
        }

        dto.economy = dto.ballsBowled == 0 ? 0.0 : roundTo2(dto.runsConceded * 6.0 / (double) dto.ballsBowled);
        dto.bowlingAverage = dto.totalWickets == 0 ? 0.0 : roundTo2((double) dto.runsConceded / (double) dto.totalWickets);

        // Best bowling figures: evaluate match-wise wickets first, then runs conceded
        Integer bestW = null;
        Integer bestR = null;
        Long bestMatchId = null;
        List<Match> matches = matchRepo.findByTournament_Id(tournamentId);
        if (matches != null) {
            for (Match m : matches) {
                if (m == null || m.getId() == null) continue;
                Integer w = cricketBallInterface.countWicketsByMatchAndBowler(m.getId(), playerId);
                Integer r = cricketBallInterface.sumRunsConcededByMatchAndBowler(m.getId(), playerId);
                int wv = (w == null ? 0 : w);
                int rv = (r == null ? 0 : r);

                if (bestW == null || wv > bestW || (wv == bestW && rv < (bestR == null ? Integer.MAX_VALUE : bestR))) {
                    bestW = wv;
                    bestR = rv;
                    bestMatchId = m.getId();
                }
            }
        }

        dto.bestFigureWickets = bestW;
        dto.bestFigureRuns = bestR;
        dto.bestFigureMatchId = bestMatchId;

        return dto;
    }

    public ResponseEntity<?> getTournamentPlayerStats(Long tournamentId, Long playerId) {
        return ResponseEntity.ok(getTournamentPlayerStatsDto(tournamentId, playerId));
    }

    public ResponseEntity<?> getAllStats() {
        List<Stats> stats = statsInterface.findAllActive();
        return ResponseEntity.ok(stats);
    }

    public ResponseEntity<?> getStatsById(Long id) {
        Stats stats = statsInterface.findById(id).orElse(null);
        if (stats == null || stats.getPlayer() == null || Boolean.TRUE.equals(stats.getPlayer().getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    public ResponseEntity<?> createStats(Long playerId, Long tournamentId) {
        if (playerId == null || tournamentId == null) {
            return ResponseEntity.badRequest().body("playerId and tournamentId are required");
        }
        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body("Player not found");
        }
        Stats stats = new Stats();
        stats.setPlayer(player);

        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }
        stats.setTournament(t);
        stats.setSportType(t.getSport());
        Stats savedStats = statsInterface.save(stats);
        return ResponseEntity.ok(savedStats);
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
            if (CricketRules.isBallFaced(ball)) {
                batStats.setBallsFaced(batStats.getBallsFaced() + 1);
            }

            // boundaries
            if (Boolean.TRUE.equals(ball.getIsFour())) batStats.setFours(batStats.getFours() + 1);
            if (Boolean.TRUE.equals(ball.getIsSix())) batStats.setSixes(batStats.getSixes() + 1);

            if (ball.getInnings() != null && ball.getInnings().getId() != null) {
                Integer inningsRuns = cricketBallInterface.sumBatsmanRunsByInnings(batsman.getId(), ball.getInnings().getId());
                int inns = inningsRuns == null ? 0 : inningsRuns;
                if (inns > batStats.getHighest()) batStats.setHighest(inns);
            }

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

            bowlStats.setRunsConceded(bowlStats.getRunsConceded() + CricketRules.runsConcededThisBall(ball));

            // legal delivery -> counts as ball bowled
            if (Boolean.TRUE.equals(ball.getLegalDelivery())) {
                bowlStats.setBallsBowled(bowlStats.getBallsBowled() + 1);
            }

            // wicket credited to bowler? exclude runout as bowler wicket unless rules say otherwise
            String dismissal = ball.getDismissalType();
            if (dismissal != null) {
                if (CricketRules.isBowlerCreditedWicket(dismissal)) {
                    bowlStats.setWickets(bowlStats.getWickets() + 1);
                }
            }

            statsInterface.save(bowlStats);
        }
    }


    public ResponseEntity<MatchScorecardDTO> getMatchScorecard(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) {
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
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> {
                    Long playerId = entry.getKey();
                    List<CricketBall> pBalls = entry.getValue();
                    Player p = pBalls.get(0).getBatsman();

                    BattingScoreDTO bs = new BattingScoreDTO();
                    bs.playerId = playerId;
                    bs.playerName = p == null ? null : p.getName();

                    bs.runs = pBalls.stream().mapToInt(b -> b.getRuns() == null ? 0 : b.getRuns()).sum();
                    bs.balls = (int) pBalls.stream().filter(CricketRules::isBallFaced).count();
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
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
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
                        return CricketRules.runsConcededThisBall(b);
                    }).sum();

                    bw.wickets = (int) bowlerBalls.stream()
                            .filter(b -> b.getDismissalType() != null && !b.getDismissalType().trim().isEmpty())
                            .filter(b -> CricketRules.isBowlerCreditedWicket(b.getDismissalType()))
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
        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.playerId = playerId;

        if (playerId == null) {
            dto.playerName = "Unknown";
            return dto;
        }
        Player p = playerInterface.findActiveById(playerId).orElse(null);
        if (p == null) {
            dto.playerName = "Unknown";
            return dto;
        }
        dto.playerName = p.getName();


        List<CricketBall> batsmanBalls = (matchId != null)
                ? cricketBallInterface.findByBatsmanIdAndMatchId(playerId, matchId)
                : cricketBallInterface.findBatsmanBallsByTournamentAndPlayer(tournamentId, playerId);

        int runs = batsmanBalls.stream().mapToInt(b -> b.getRuns() == null ? 0 : b.getRuns()).sum();
        // balls faced: wide is not faced; no-ball is faced even if illegal
        int ballsFaced = (int) batsmanBalls.stream().filter(CricketRules::isBallFaced).count();

        int fours = (int) batsmanBalls.stream().filter(b -> Boolean.TRUE.equals(b.getIsFour())).count();
        int sixes = (int) batsmanBalls.stream().filter(b -> Boolean.TRUE.equals(b.getIsSix())).count();

        Map<Long, Integer> runsPerInnings = new HashMap<>();
        for (CricketBall b : batsmanBalls) {
            if (b.getInnings() != null) {
                Long id = b.getInnings().getId();
                int run = (b.getRuns() == null) ? 0 : b.getRuns();
                runsPerInnings.put(id, runsPerInnings.getOrDefault(id, 0) + run);
            }
        }
        int highest = runsPerInnings.values().stream().mapToInt(Integer::intValue).max().orElse(0);


        // notOut (count innings where player batted and has no dismissal recorded)
        Set<Long> inningsWithBalls = batsmanBalls.stream().filter(b -> b.getInnings() != null).map(b -> b.getInnings().getId()).collect(Collectors.toSet());
        Set<Long> inningsWhereOut = batsmanBalls.stream().filter(b -> b.getDismissalType() != null && b.getInnings() != null).map(b -> b.getInnings().getId()).collect(Collectors.toSet());

        int notOut = (int) inningsWithBalls.stream().filter(id -> !inningsWhereOut.contains(id)).count();

        int dismissals = inningsWhereOut.size();
        int outs = Math.max(0, dismissals); // retired handling can be added if you store those types
        double battingAvg = (outs == 0) ? runs : roundTo2((double) runs / (double) outs);

        dto.matchesPlayed = cricketBallInterface.countMatchesPlayedInTournament(playerId, tournamentId);
        dto.runs = runs;
        dto.ballsFaced = ballsFaced;
        dto.fours = fours;
        dto.sixes = sixes;
        dto.highest = highest;
        dto.notOut = notOut;

        dto.strikeRate = (ballsFaced == 0) ? 0.0 : roundTo2((double) runs * 100.0 / (double) ballsFaced);
        dto.battingAvg = battingAvg;

        // BOWLING
        List<CricketBall> bowlerBalls = (matchId != null)
                ? cricketBallInterface.findByBowlerIdAndMatchId(playerId, matchId)
                : cricketBallInterface.findBowlerBallsByTournamentAndPlayer(tournamentId, playerId);

        int runsConceded = bowlerBalls.stream().mapToInt(b -> {
            return CricketRules.runsConcededThisBall(b);
        }).sum();

        int ballsBowled = (int) bowlerBalls.stream().filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count();
        int wickets = (int) bowlerBalls.stream()
                .filter(b -> b.getDismissalType() != null)
                .filter(b -> CricketRules.isBowlerCreditedWicket(b.getDismissalType()))
                .count();

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


    public PlayerStatsDTO getPlayerCricketTournamentStats(Long playerId, Long tournamentId, Long matchId) {
        return getPlayerTournamentStats(playerId, tournamentId, matchId);
    }


    public ResponseEntity<?> optimizePlayerStats(Long playerId, Long tournamentId) {
        PlayerStatsDTO dto = getOptimizedPlayerStatsDto(playerId, tournamentId);
        return ResponseEntity.ok(dto);

    }

    @Cacheable(cacheNames = "playerStats", key = "T(String).valueOf(#tournamentId).concat(':').concat(T(String).valueOf(#playerId))")
    public PlayerStatsDTO getOptimizedPlayerStatsDto(Long playerId, Long tournamentId) {
        Object[] batting = cricketBallInterface.getBattingAggregate(playerId, tournamentId);
        Object[] bowling = cricketBallInterface.getBowlingAggregate(playerId, tournamentId);

        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.playerId = playerId;
        dto.playerName = playerInterface.findActiveById(playerId).map(Player::getName).orElse("Unknown");

        // Batting
        if (batting != null && batting.length >= 4) {
            dto.runs = batting[0] != null ? ((Number) batting[0]).intValue() : 0;
            dto.ballsFaced = batting[1] != null ? ((Number) batting[1]).intValue() : 0;
            dto.fours = batting[2] != null ? ((Number) batting[2]).intValue() : 0;
            dto.sixes = batting[3] != null ? ((Number) batting[3]).intValue() : 0;
        }

        // Bowling
        if (bowling != null && bowling.length >= 3) {
            dto.runsConceded = bowling[0] != null ? ((Number) bowling[0]).intValue() : 0;
            dto.ballsBowled = bowling[1] != null ? ((Number) bowling[1]).intValue() : 0;
            dto.wickets = bowling[2] != null ? ((Number) bowling[2]).intValue() : 0;
        }

        dto.strikeRate = dto.ballsFaced == 0 ? 0.0 : roundTo2(dto.runs * 100.0 / (double) dto.ballsFaced);
        dto.economy = dto.ballsBowled == 0 ? 0.0 : roundTo2(dto.runsConceded * 6.0 / (double) dto.ballsBowled);
        dto.bowlingAverage = dto.wickets == 0 ? 0.0 : roundTo2((double) dto.runsConceded / (double) dto.wickets);

        Integer highest = cricketBallInterface
                .getRunsPerInningsDesc(playerId, tournamentId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(0);
        dto.highest = highest == null ? 0 : highest;

        Integer notOut = cricketBallInterface.countNotOutInnings(playerId, tournamentId);
        dto.notOut = notOut == null ? 0 : notOut;

        dto.matchesPlayed = cricketBallInterface.countMatchesPlayedInTournament(playerId, tournamentId);

        int inningsBatted = cricketBallInterface.countDistinctInningsBatted(playerId, tournamentId);
        int outs = Math.max(0, inningsBatted - dto.notOut);
        dto.battingAvg = outs == 0 ? (double) dto.runs : roundTo2((double) dto.runs / (double) outs);

        return dto;
    }

}
