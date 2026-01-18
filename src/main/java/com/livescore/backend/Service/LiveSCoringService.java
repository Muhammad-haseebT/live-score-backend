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
    @Autowired
    private MediaInterface mediaRepo;

    @Transactional
    public ScoreDTO scoring(ScoreDTO s) {

        if (s == null) {
            ScoreDTO err = new ScoreDTO();
            err.setStatus("ERROR");
            err.setComment("ScoreDTO required");
            return err;
        }
        if (s.getMatchId() == null) {
            s.setStatus("ERROR");
            s.setComment("matchId required");
            return s;
        }
        if (s.getInningsId() == null) {
            s.setStatus("ERROR");
            s.setComment("inningsId required");
            return s;
        }

        Match m = matchRepo.findById(s.getMatchId()).orElse(null);
        if (m == null) {
            s.setStatus("ERROR");
            s.setComment("Match not found");
            return s;
        }
        if (isMatchFinal(m)) {
            s.setStatus("ERROR");
            s.setComment("Match already ended");
            return s;
        }
        CricketInnings currentInnings = cricketInningsRepo.findById(s.getInningsId()).orElse(null);
        if (currentInnings == null) {
            s.setStatus("ERROR");
            s.setComment("Innings not found");
            return s;
        }
        if (currentInnings.getMatch() == null || currentInnings.getMatch().getId() == null
                || !currentInnings.getMatch().getId().equals(m.getId())) {
            s.setStatus("ERROR");
            s.setComment("Innings does not belong to match");
            return s;
        }

        int maxBallsPerInnings = (m.getOvers() == 0 ? 0 : m.getOvers() * 6);

        long legalBallsSoFar = cricketBallInterface.countLegalBallsByInningsId(currentInnings.getId());
        long wicketsSoFar = cricketBallInterface.countWicketsByInningsId(currentInnings.getId());





        if (legalBallsSoFar >= maxBallsPerInnings) {

            s.setStatus("END");

            return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
        }

        if (s.getOvers() < 0 || s.getBalls() < 0) {
            s.setStatus("ERROR");
            s.setComment("Invalid over/ball number");
            return s;
        }

        int inningsNo = currentInnings.getNo();
        List<CricketBall> existing = cricketBallInterface.findByOverNumberAndBallNumberAndMatch_Id(
                s.getOvers(),
                s.getBalls(),
                m.getId(),
                inningsNo
        );
        if (existing != null && !existing.isEmpty()) {
            s.setStatus("ERROR");
            s.setComment("Duplicate ball");
            return s;
        }

        CricketBall ball = new CricketBall();
        ball.setInnings(currentInnings);
        ball.setBatsman(s.getBatsmanId() == null ? null : playerRepo.findActiveById(s.getBatsmanId()).orElse(null));
        ball.setBowler(s.getBowlerId() == null ? null : playerRepo.findActiveById(s.getBowlerId()).orElse(null));
        ball.setFielder(s.getFielderId() == null ? null : playerRepo.findActiveById(s.getFielderId()).orElse(null));
        ball.setMatch(m);
        ball.setOverNumber(s.getOvers());
        ball.setBallNumber(s.getBalls());
        ball.setComment(s.getComment());

        if (s.getEventType() == null || s.getEventType().isBlank()) {
            s.setStatus("ERROR");
            s.setComment("eventType required");
            return s;
        }

        // basic sanity: wicket without a batsman/out-player is meaningless
        if ("wicket".equalsIgnoreCase(s.getEventType()) && ball.getBatsman() == null && s.getOutPlayerId() == null) {
            s.setStatus("ERROR");
            s.setComment("batsmanId or outPlayerId required for wicket");
            return s;
        }


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
                int w = parseIntSafe(s.getEvent());
                ball.setRuns(0);
                ball.setExtra(w + 1);
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
                if (s.getOutPlayerId() != null) outPlayer = playerRepo.findActiveById(s.getOutPlayerId()).orElse(null);
                if (outPlayer == null) {
                    outPlayer = ball.getBatsman();
                }
                ball.setOutPlayer(outPlayer);

                ball.setRuns(s.getRunsOnThisBall());
                ball.setExtra(s.getExtrasThisBall());
                ball.setLegalDelivery(true);

                if (dismissal != null && (dismissal.equalsIgnoreCase("caught") || dismissal.equalsIgnoreCase("runout") || dismissal.equalsIgnoreCase("stumped"))) {
                    ball.setFielder(s.getFielderId() == null ? null : playerRepo.findActiveById(s.getFielderId()).orElse(null));
                }
                break;
            }
            default:
                s.setStatus("ERROR");
                s.setComment("Invalid event type: " + s.getEventType());
                return s;
        }
        if(s.getMediaId()!=null){
            ball.setMedia(mediaRepo.findById(s.getMediaId()).orElse(null));
        }
        cricketBallInterface.save(ball);
        statsService.updateTournamentStats(ball);

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


        if (!s.isFirstInnings()) {

            CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 1);
            int firstRuns = 0;
            if (firstInnings != null) {
                List<CricketBall> firstBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), firstInnings.getId());
                firstRuns = firstBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
            }
            int target = firstRuns + 1;
            int remainingRuns = target - inningsRuns;

            int remainingBalls = Math.max(0, maxBallsPerInnings - legalBallsNow);

            if (remainingRuns < 0) {

                Team chasingTeam = currentInnings.getTeam();
                if (!isMatchFinal(m)) {
                    m.setWinnerTeam(chasingTeam);
                    m.setStatus("COMPLETED");
                    matchRepo.save(m);
                    awardService.computeMatchAwards(m.getId());
                    matchService.endMatch(m.getId());
                }

                s.setStatus("END_MATCH");
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
                    if (!isMatchFinal(m)) {
                        if (!"TIED".equalsIgnoreCase(m.getStatus())) {
                            m.setStatus("COMPLETED");
                        }
                        matchRepo.save(m);
                        awardService.computeMatchAwards(m.getId());
                        matchService.endMatch(m.getId());
                    }

                    s.setStatus("END_MATCH");
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
            s.setTarget(inningsRuns);
        }

        int teamPlayers = 11;
        try {
            Team t = currentInnings.getTeam();
            if (t != null) {
                Optional<Team> teamOpt = teamRepo.findById(t.getId());
                if (teamOpt.isPresent()) {
                    Team teamEntity = teamOpt.get();
                    if (teamEntity.getPlayers() != null) {
                        teamPlayers = teamEntity.getPlayers().size();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        int maxWickets = Math.max(0, teamPlayers - 1);

        if (wicketsNow >= maxWickets) {
            // innings ended by all-out
            if (s.isFirstInnings()) {

                return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
            } else {

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

                if (!isMatchFinal(m)) {
                    if (!"TIED".equalsIgnoreCase(m.getStatus())) {
                        m.setStatus("COMPLETED");
                    }
                    matchRepo.save(m);
                    awardService.computeMatchAwards(m.getId());
                    matchService.endMatch(m.getId());
                }

                s.setStatus("END_MATCH");
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
                if (!isMatchFinal(m)) {
                    if (!"TIED".equalsIgnoreCase(m.getStatus())) {
                        m.setStatus("COMPLETED");
                    }
                    matchRepo.save(m);
                    awardService.computeMatchAwards(m.getId());
                    matchService.endMatch(m.getId());
                }

                s.setStatus("END_MATCH");
                return s;
            }
        }


        return s;
    }






    private ScoreDTO handleEndOfInningsAndMaybeCreateNext(ScoreDTO s, Match m, CricketInnings currentInnings) {

        CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 1);
        int firstRuns = 0;
        if (firstInnings != null) {
            List<CricketBall> firstBalls = cricketBallInterface.findByMatch_IdAndInnings_Id(m.getId(), firstInnings.getId());
            firstRuns = firstBalls.stream().mapToInt(b -> (b.getRuns() == null ? 0 : b.getRuns()) + (b.getExtra() == null ? 0 : b.getExtra())).sum();
        }


        if (s.isFirstInnings()) {
            CricketInnings existingSecond = cricketInningsRepo.findByMatchIdAndNo(m.getId(), 2);
            if (existingSecond != null && existingSecond.getId() != null) {
                // idempotency: second innings already created
                s.setInningsId(existingSecond.getId());
                s.setStatus("END_FIRST");
                s.setFirstInnings(false);
                s.setTarget(firstRuns + 1);
                return s;
            }
            CricketInnings innings = new CricketInnings();
            innings.setMatch(m);
            innings.setNo(2);
            // set chasing team: the other team
            Team t1 = m.getTeam1();
            Team t2 = m.getTeam2();
            if (t1 == null || t2 == null) {
                s.setStatus("ERROR");
                s.setComment("Match teams missing");
                return s;
            }

            Team chasing = (currentInnings.getTeam() != null && currentInnings.getTeam().getId().equals(t1.getId())) ? t2 : t1;
            innings.setTeam(chasing);
            cricketInningsRepo.save(innings);

            // set DTO to represent second innings start
            s.setInningsId(innings.getId());
            s.setStatus("END_FIRST");

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
            if (isMatchFinal(m)) {
                s.setStatus("END_MATCH");
                return s;
            }

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

            if (!isMatchFinal(m)) {
                if (!"TIED".equalsIgnoreCase(m.getStatus())) {
                    m.setStatus("COMPLETED");
                }
                matchRepo.save(m);
                awardService.computeMatchAwards(m.getId());
                matchService.endMatch(m.getId());
            }

            s.setStatus("END_MATCH");
            s.setTarget(Math.max(0, (firstRuns + 1) - secondRuns));
            return s;
        }
    }

    private boolean isMatchFinal(Match m) {
        if (m == null || m.getStatus() == null) return false;
        String st = m.getStatus().trim().toUpperCase();
        return st.equals("COMPLETED") || st.equals("FINISHED") || st.equals("ABANDONED") || st.equals("TIED");
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
