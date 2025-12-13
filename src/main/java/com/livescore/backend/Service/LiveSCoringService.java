package com.livescore.backend.Service;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LiveSCoringService {
    @Autowired
    private CricketBallInterface cricketBallInterface;
    @Autowired
    private PlayerInterface playerRepo;
    @Autowired
    private CricketInningsInterface cricketInningsRepo;
    @Autowired
    private MatchInterface matchRepo;
    @Autowired
    private TeamInterface teamRepo;
    @Autowired
    private StatsService statsService;
    @Autowired
    private AwardService awardService;
    @Autowired
    private MatchService matchService;

    @Transactional
    public ScoreDTO scoring(ScoreDTO s) {

        // basic validations
        if (s == null) throw new RuntimeException("ScoreDTO required");
        Match m = matchRepo.findById(s.getMatchId()).orElseThrow(() -> new RuntimeException("Match not found"));
        CricketInnings currentInnings = cricketInningsRepo.findById(s.getInningsId()).orElseThrow(() -> new RuntimeException("Innings not found"));

        // caps
        int maxBallsPerInnings = (m.getOvers() == 0 ? 0 : m.getOvers() * 6);

        // count current legal balls and wickets before processing this new ball
        long legalBallsSoFar = cricketBallInterface.countLegalBallsByInningsId(currentInnings.getId());
        long wicketsSoFar = cricketBallInterface.countWicketsByInningsId(currentInnings.getId()); // add repo method or fallback below

        // (If your repo doesn't have countWicketsByInningsId, we compute below after fetching list.)

        boolean proceed = true;

        // If innings already reached max overs, end or finalize
        if (legalBallsSoFar >= maxBallsPerInnings) {
            // innings finished by overs
            s.setStatus("end");
            return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
        }

        // Build ball entity
        CricketBall ball = new CricketBall();
        ball.setInnings(currentInnings);
        ball.setBatsman(s.getBatsmanId() == null ? null : playerRepo.findById(s.getBatsmanId()).orElse(null));
        ball.setBowler(s.getBowlerId() == null ? null : playerRepo.findById(s.getBowlerId()).orElse(null));
        ball.setFielder(s.getFielderId() == null ? null : playerRepo.findById(s.getFielderId()).orElse(null));
        ball.setMatch(m);
        ball.setOverNumber(s.getOvers());
        ball.setBallNumber(s.getBalls());

        if (s.getEventType() == null) throw new RuntimeException("eventType required");

        // default init
        ball.setRuns(0);
        ball.setExtra(0);
        ball.setExtraType(null);
        ball.setLegalDelivery(Boolean.FALSE);
        ball.setIsFour(Boolean.FALSE);
        ball.setIsSix(Boolean.FALSE);

        switch (s.getEventType().toLowerCase()) {
            case "run": {
                int r = parseIntSafe(s.getEvent());
                ball.setRuns(r);
                ball.setExtra(0);
                ball.setExtraType(null);
                ball.setLegalDelivery(true);
                break;
            }
            case "boundary": {
                int b = parseIntSafe(s.getEvent()); // expect 4 or 6
                ball.setRuns(b);
                ball.setExtra(0);
                ball.setExtraType(null);
                ball.setLegalDelivery(true);
                if (b == 4) ball.setIsFour(true);
                if (b == 6) ball.setIsSix(true);
                break;
            }
            case "wide": {
                int w = parseIntSafe(s.getEvent()); // usually 0 or extras
                ball.setRuns(0);
                ball.setExtra(w + 1); // 1 + any overthrow/run off wide
                ball.setExtraType("WIDE");
                ball.setLegalDelivery(false);
                break;
            }
            case "noball": {
                int nb = parseIntSafe(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(nb + 1); // 1 + any runs
                ball.setExtraType("NO_BALL");
                ball.setLegalDelivery(false);
                break;
            }
            case "bye": {
                int by = parseIntSafe(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(by);
                ball.setExtraType("BYE");
                ball.setLegalDelivery(true);
                break;
            }
            case "legbye": {
                int lb = parseIntSafe(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(lb);
                ball.setExtraType("LEGBYE");
                ball.setLegalDelivery(true);
                break;
            }
            case "wicket": {
                String dismissal = s.getEvent();
                ball.setDismissalType(dismissal);
                Player outPlayer = null;
                if (s.getOutPlayerId() != null) outPlayer = playerRepo.findById(s.getOutPlayerId()).orElse(null);
                if (outPlayer == null) {
                    outPlayer = ball.getBatsman();
                }
                ball.setOutPlayer(outPlayer);
                // runs on this ball (could be 0 or some runs)
                ball.setRuns(s.getRunsOnThisBall());
                ball.setExtra(s.getExtrasThisBall());
                ball.setLegalDelivery(true);
                // fielder if applicable
                if (dismissal != null && (dismissal.equalsIgnoreCase("caught") || dismissal.equalsIgnoreCase("runout") || dismissal.equalsIgnoreCase("stumped"))) {
                    ball.setFielder(s.getFielderId() == null ? null : playerRepo.findById(s.getFielderId()).orElse(null));
                }
                break;
            }
            default:
                throw new RuntimeException("Invalid event type: " + s.getEventType());
        }

        // Save the ball
        cricketBallInterface.save(ball);

        // Update tournament / player stats
        statsService.updateTournamentStats(ball);

        // Recalculate authoritative innings totals after saving
        List<CricketBall> inningsBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), currentInnings.getId());
        int inningsRuns = inningsBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
        int legalBallsNow = (int) inningsBalls.stream().filter(b -> Boolean.TRUE.equals(b.getLegalDelivery())).count();
        int wicketsNow = (int) inningsBalls.stream().filter(b -> b.getDismissalType() != null && !b.getDismissalType().trim().isEmpty()).count();

        // Update DTO live values
        s.setRuns(inningsRuns);
        s.setBalls(legalBallsNow % 6); // current ball in over
        s.setOvers(legalBallsNow / 6);
        s.setWickets(wicketsNow);

        // compute CRR (current run rate) for this innings: runs per over (runs * 6 / legalBalls)
        double crr = legalBallsNow == 0 ? 0.0 : ((double) inningsRuns * 6.0) / (double) legalBallsNow;
        s.setCrr(round2(crr));

        // If second innings, compute RRR
        if (!s.isFirstInnings()) {
            // compute first innings total (target = firstRuns + 1)
            CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 1);
            int firstRuns = 0;
            if (firstInnings != null) {
                List<CricketBall> firstBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), firstInnings.getId());
                firstRuns = firstBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
            }
            int target = firstRuns + 1;
            int remainingRuns = target - inningsRuns;

            int remainingBalls = Math.max(0, maxBallsPerInnings - legalBallsNow);

            if (remainingRuns <= 0) {
                // chasing team has reached target -> match over, set winner
                Team chasingTeam = currentInnings.getTeam();
                m.setWinnerTeam(chasingTeam);
                m.setStatus("COMPLETED");
                matchRepo.save(m);
                awardService.computeMatchAwards(m.getId());
                matchService.endMatch(m.getId());

                s.setStatus("end Match");
                s.setTarget(0);
                s.setRrr(0.0);
                return s;
            } else {
                // not yet won
                if (remainingBalls == 0) {
                    // overs finished and target not reached -> match over, determine winner by runs
                    Team otherTeam = m.getTeam1().getId().equals(currentInnings.getTeam().getId()) ? m.getTeam2() : m.getTeam1();
                    // winner is team with higher runs
                    if (inningsRuns > firstRuns) {
                        m.setWinnerTeam(currentInnings.getTeam());
                    } else if (inningsRuns < firstRuns) {
                        m.setWinnerTeam(otherTeam);
                    } else {
                        // tie -> handle as per rules; mark draw or tie
                        m.setWinnerTeam(null);
                        m.setStatus("TIED");
                    }
                    m.setStatus("COMPLETED");
                    matchRepo.save(m);
                    awardService.computeMatchAwards(m.getId());
                    matchService.endMatch(m.getId());

                    s.setStatus("end Match");
                    s.setTarget(remainingRuns);
                    s.setRrr(Double.POSITIVE_INFINITY);
                    return s;
                } else {
                    // compute required run rate
                    double rrr = ((double) remainingRuns * 6.0) / (double) remainingBalls;
                    s.setRrr(round2(rrr));
                    s.setTarget(remainingRuns);
                }
            }
        } else {
            // first innings: set target in DTO (runs scored)
            s.setTarget(inningsRuns);
        }

        // ALL-OUT check: determine team players count (fallback 11)
        int teamPlayers = 11;
        try {
            Team t = currentInnings.getTeam();
            if (t != null) {
                Optional<Team> teamOpt = teamRepo.findById(t.getId());
                if (teamOpt.isPresent()) {
                    Team teamEntity = teamOpt.get();
                    // try to infer players collection size; adjust getter if different name
                    if (teamEntity.getPlayers() != null) {
                        teamPlayers = teamEntity.getPlayers().size();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        int maxWickets = Math.max(0, teamPlayers - 1); // e.g., 11 players -> 10 wickets

        if (wicketsNow >= maxWickets) {
            // innings ended by all-out
            if (s.isFirstInnings()) {
                // create second innings
                return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
            } else {
                // second innings all out -> determine winner
                CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 1);
                int firstRuns = 0;
                if (firstInnings != null) {
                    List<CricketBall> firstBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), firstInnings.getId());
                    firstRuns = firstBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
                }

                if (inningsRuns > firstRuns) {
                    m.setWinnerTeam(currentInnings.getTeam());
                } else if (inningsRuns < firstRuns) {
                    Team other = m.getTeam1().getId().equals(currentInnings.getTeam().getId()) ? m.getTeam2() : m.getTeam1();
                    m.setWinnerTeam(other);
                } else {
                    // tie
                    m.setWinnerTeam(null);
                    m.setStatus("TIED");
                }

                m.setStatus("COMPLETED");
                matchRepo.save(m);
                awardService.computeMatchAwards(m.getId());
                matchService.endMatch(m.getId());

                s.setStatus("end Match");
                s.setTarget(Math.max(0, (firstRuns + 1) - inningsRuns));
                s.setRrr(Double.POSITIVE_INFINITY);
                return s;
            }
        }

        // If overs limit reached AFTER saving this ball
        if (legalBallsNow >= maxBallsPerInnings) {
            // if first innings -> create second innings
            if (s.isFirstInnings()) {
                return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
            } else {
                // second innings -> match finished on overs
                CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 1);
                int firstRuns = 0;
                if (firstInnings != null) {
                    List<CricketBall> firstBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), firstInnings.getId());
                    firstRuns = firstBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
                }
                if (inningsRuns > firstRuns) {
                    m.setWinnerTeam(currentInnings.getTeam());
                } else if (inningsRuns < firstRuns) {
                    Team other = m.getTeam1().getId().equals(currentInnings.getTeam().getId()) ? m.getTeam2() : m.getTeam1();
                    m.setWinnerTeam(other);
                } else {
                    m.setWinnerTeam(null);
                    m.setStatus("TIED");
                }
                m.setStatus("COMPLETED");
                matchRepo.save(m);
                awardService.computeMatchAwards(m.getId());
                matchService.endMatch(m.getId());

                s.setStatus("end Match");
                return s;
            }
        }

        // normal ongoing ball: just return updated DTO with live CRR/RRR and target
        return s;
    }

    // helper: create second innings and return DTO
    private ScoreDTO handleEndOfInningsAndMaybeCreateNext(ScoreDTO s, Match m, CricketInnings currentInnings) {
        // compute first innings total
        CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 1);
        int firstRuns = 0;
        if (firstInnings != null) {
            List<CricketBall> firstBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), firstInnings.getId());
            firstRuns = firstBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
        }

        // if we're already in first innings, create second innings and return its id + status
        if (s.isFirstInnings()) {
            CricketInnings innings = new CricketInnings();
            innings.setMatch(m);
            innings.setNo(2);
            // set chasing team: the other team
            Team t1 = m.getTeam1();
            Team t2 = m.getTeam2();
            if (t1 == null || t2 == null) throw new RuntimeException("Match teams missing");

            Team chasing = (currentInnings.getTeam() != null && currentInnings.getTeam().getId().equals(t1.getId())) ? t2 : t1;
            innings.setTeam(chasing);
            cricketInningsRepo.save(innings);

            // set DTO to represent second innings start
            s.setInningsId(innings.getId());
            s.setStatus("endFirst");
            s.setRuns(0);
            s.setOvers(0);
            s.setWickets(0);
            s.setBalls(0);
            s.setEvent("");
            s.setEventType("");
            s.setDismissalType("");
            s.setIsLegal(false);
            s.setFour(false);
            s.setSix(false);
            s.setComment("");
            s.setOutPlayerId(null);
            s.setMediaId(null);
            s.setFirstInnings(false);

            s.setTarget(firstRuns + 1); // runs required to win
            return s;
        } else {
            // already second innings -> end match (overs exhausted)
            // determine winner
            CricketInnings second = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 2);
            int secondRuns = 0;
            if (second != null) {
                List<CricketBall> secondBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), second.getId());
                secondRuns = secondBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
            }

            if (secondRuns > firstRuns) m.setWinnerTeam(second.getTeam());
            else if (secondRuns < firstRuns) m.setWinnerTeam(firstInnings.getTeam());
            else {
                m.setWinnerTeam(null);
                m.setStatus("TIED");
            }

            m.setStatus("COMPLETED");
            matchRepo.save(m);
            awardService.computeMatchAwards(m.getId());
            matchService.endMatch(m.getId());

            s.setStatus("end Match");
            s.setTarget(Math.max(0, (firstRuns + 1) - secondRuns));
            return s;
        }
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
