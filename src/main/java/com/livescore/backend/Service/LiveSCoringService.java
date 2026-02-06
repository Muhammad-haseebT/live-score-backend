package com.livescore.backend.Service;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
    private CacheEvictionService cacheEvictionService;
    @Autowired
    private AwardService awardService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private MediaInterface mediaRepo;

    /**
     * Main scoring method that processes a single cricket ball delivery.
     * Validates input, updates match state, and handles end-of-innings logic.
     *
     * @param s The ScoreDTO containing ball information
     * @return Updated ScoreDTO with current match state
     */
    @CachePut(value = "matchState", key = "#s.matchId")
    @Transactional
    public ScoreDTO scoring(ScoreDTO s) {
        // 1. Validate input
        ScoreDTO validationError = validateScoringInput(s);
        if (validationError != null) return validationError;

        // 2. Get and validate match
        Match m = matchRepo.findById(s.getMatchId()).orElse(null);
        if (m == null) {
            return createError(s, Constants.ERROR_MATCH_NOT_FOUND);
        }
        if (isMatchFinal(m)) {
            return createError(s, Constants.ERROR_MATCH_ALREADY_ENDED);
        }

        // 3. Get and validate innings
        CricketInnings currentInnings = cricketInningsRepo.findById(s.getInningsId()).orElse(null);
        if (currentInnings == null) {
            return createError(s, Constants.ERROR_INNINGS_NOT_FOUND);
        }
        if (!isInningsBelongsToMatch(currentInnings, m)) {
            return createError(s, "Innings does not belong to match");
        }

        // 4. Check if innings already completed
        int maxBallsPerInnings = calculateMaxBalls(m);
        long legalBallsSoFar = cricketBallInterface.countLegalBallsByInningsId(currentInnings.getId());

        if (maxBallsPerInnings > 0 && legalBallsSoFar >= maxBallsPerInnings) {
            s.setStatus(Constants.STATUS_END);
            return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
        }

        // 5. Validate over and ball numbers
        if (s.getOvers() < 0 || s.getBalls() < 0) {
            return createError(s, "Invalid over/ball number");
        }

        // 6. Check for duplicate ball
        if (isDuplicateBall(s, m, currentInnings.getNo())) {
            return createError(s, Constants.ERROR_DUPLICATE_BALL);
        }

        // 7. Create and process ball
        CricketBall ball = createBallEntity(s, m, currentInnings);
        ScoreDTO eventError = processBallEvent(s, ball);
        if (eventError != null) return eventError;

        // 8. Save ball and update stats
        cricketBallInterface.save(ball);
        statsService.updateTournamentStats(ball.getId());
        evictCacheIfNeeded(m);

        // 9. Calculate current innings state
        updateInningsState(s, currentInnings);

        // 10. Handle second innings logic
        if (!s.isFirstInnings()) {
            return handleSecondInnings(s, m, currentInnings, maxBallsPerInnings);
        } else {
            s.setTarget(s.getRuns());
        }

        // 11. Check for all-out or overs completed
        return checkInningsEnd(s, m, currentInnings, maxBallsPerInnings);
    }






    /**
     * Handles the end of an innings and creates the next innings if it's the first innings.
     * For second innings, determines match winner and marks match as complete.
     *
     * @param s              ScoreDTO
     * @param m              Match entity
     * @param currentInnings Current innings entity
     * @return Updated ScoreDTO
     */
    private ScoreDTO handleEndOfInningsAndMaybeCreateNext(ScoreDTO s, Match m, CricketInnings currentInnings) {

        CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), Constants.FIRST_INNINGS);
        int firstRuns = getInningsRuns(firstInnings);

        if (s.isFirstInnings()) {
            CricketInnings existingSecond = cricketInningsRepo.findByMatchIdAndNo(m.getId(), Constants.SECOND_INNINGS);
            if (existingSecond != null && existingSecond.getId() != null) {
                // idempotency: second innings already created
                s.setInningsId(existingSecond.getId());
                s.setStatus(Constants.STATUS_END_FIRST);
                s.setFirstInnings(false);
                s.setTarget(firstRuns + 1);
                return s;
            }
            CricketInnings innings = new CricketInnings();
            innings.setMatch(m);
            innings.setNo(Constants.SECOND_INNINGS);
            // set chasing team: the other team
            Team t1 = m.getTeam1();
            Team t2 = m.getTeam2();
            if (t1 == null || t2 == null) {
                return createError(s, "Match teams missing");
            }

            Team chasing = (currentInnings.getTeam() != null && currentInnings.getTeam().getId().equals(t1.getId())) ? t2 : t1;
            innings.setTeam(chasing);
            cricketInningsRepo.save(innings);

            // set DTO to represent second innings start
            s.setInningsId(innings.getId());
            s.setStatus(Constants.STATUS_END_FIRST);

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
                s.setStatus(Constants.STATUS_END_MATCH);
                return s;
            }

            CricketInnings second = cricketInningsRepo.findByMatchIdAndNo(m.getId(), Constants.SECOND_INNINGS);
            int secondRuns = getInningsRuns(second);

            determineMatchWinner(m, currentInnings, secondRuns, firstRuns);

            if (!isMatchFinal(m)) {
                if (!Constants.STATUS_TIED.equalsIgnoreCase(m.getStatus())) {
                    m.setStatus(Constants.STATUS_COMPLETED);
                }
                matchRepo.save(m);
                awardService.computeMatchAwards(m.getId());
                matchService.endMatch(m.getId());
            }

            s.setStatus(Constants.STATUS_END_MATCH);
            s.setTarget(Math.max(0, (firstRuns + 1) - secondRuns));
            return s;
        }
    }

    /**
     * Validates the scoring input data.
     *
     * @param s ScoreDTO to validate
     * @return Error ScoreDTO if validation fails, null otherwise
     */
    private ScoreDTO validateScoringInput(ScoreDTO s) {
        if (s == null) {
            ScoreDTO err = new ScoreDTO();
            err.setStatus(Constants.STATUS_ERROR);
            err.setComment("ScoreDTO required");
            return err;
        }
        if (s.getMatchId() == null) {
            return createError(s, "matchId required");
        }
        if (s.getInningsId() == null) {
            return createError(s, "inningsId required");
        }
        return null;
    }

    /**
     * Creates an error ScoreDTO with the given message.
     */
    private ScoreDTO createError(ScoreDTO s, String message) {
        s.setStatus(Constants.STATUS_ERROR);
        s.setComment(message);
        return s;
    }

    /**
     * Checks if an innings belongs to a specific match.
     */
    private boolean isInningsBelongsToMatch(CricketInnings innings, Match match) {
        return innings.getMatch() != null 
            && innings.getMatch().getId() != null
            && innings.getMatch().getId().equals(match.getId());
    }

    /**
     * Calculates the maximum number of balls allowed in the innings.
     */
    private int calculateMaxBalls(Match match) {
        return match.getOvers() == 0 ? 0 : match.getOvers() * Constants.BALLS_PER_OVER;
    }

    /**
     * Checks if a ball with the same over and ball number already exists.
     */
    private boolean isDuplicateBall(ScoreDTO s, Match m, int inningsNo) {
        List<CricketBall> existing = cricketBallInterface.findByOverNumberAndBallNumberAndMatch_Id(
                s.getOvers(), s.getBalls(), m.getId(), inningsNo);
        return existing != null && !existing.isEmpty();
    }

    /**
     * Creates a CricketBall entity from ScoreDTO.
     */
    private CricketBall createBallEntity(ScoreDTO s, Match m, CricketInnings innings) {
        CricketBall ball = new CricketBall();
        ball.setInnings(innings);
        ball.setBatsman(s.getBatsmanId() == null ? null : playerRepo.findActiveById(s.getBatsmanId()).orElse(null));
        ball.setBowler(s.getBowlerId() == null ? null : playerRepo.findActiveById(s.getBowlerId()).orElse(null));
        ball.setFielder(s.getFielderId() == null ? null : playerRepo.findActiveById(s.getFielderId()).orElse(null));
        ball.setMatch(m);
        ball.setOverNumber(s.getOvers());
        ball.setBallNumber(s.getBalls());
        ball.setComment(s.getComment());
        
        // Initialize defaults
        ball.setRuns(0);
        ball.setExtra(0);
        ball.setExtraType(null);
        ball.setLegalDelivery(Boolean.FALSE);
        ball.setIsFour(Boolean.FALSE);
        ball.setIsSix(Boolean.FALSE);
        
        if(s.getMediaId() != null) {
            ball.setMedia(mediaRepo.findById(s.getMediaId()).orElse(null));
        }
        
        return ball;
    }

    /**
     * Processes the ball event and updates the ball entity accordingly.
     *
     * @param s    ScoreDTO containing event information
     * @param ball CricketBall entity to update
     * @return Error ScoreDTO if processing fails, null otherwise
     */
    private ScoreDTO processBallEvent(ScoreDTO s, CricketBall ball) {
        if (s.getEventType() == null || s.getEventType().isBlank()) {
            return createError(s, "eventType required");
        }

        // Validate wicket event
        if (Constants.EVENT_WICKET.equalsIgnoreCase(s.getEventType()) 
            && ball.getBatsman() == null 
            && s.getOutPlayerId() == null) {
            return createError(s, "batsmanId or outPlayerId required for wicket");
        }

        String normalizedEventType = normalizeEventType(s.getEventType());

        switch (normalizedEventType) {
            case Constants.EVENT_RUN:
                processRunEvent(s, ball);
                break;
            case Constants.EVENT_BOUNDARY:
            case Constants.EVENT_BOUNDRY:
                processBoundaryEvent(s, ball);
                break;
            case Constants.EVENT_WIDE:
                processWideEvent(s, ball);
                break;
            case Constants.EVENT_NO_BALL:
            case Constants.EVENT_NB:
                processNoBallEvent(s, ball);
                break;
            case Constants.EVENT_BYE:
                processByeEvent(s, ball);
                break;
            case Constants.EVENT_LEG_BYE:
                processLegByeEvent(s, ball);
                break;
            case Constants.EVENT_WICKET:
                processWicketEvent(s, ball);
                break;
            default:
                return createError(s, Constants.ERROR_INVALID_EVENT_TYPE + ": " + s.getEventType());
        }

        
        return null;
    }

    /**
     * Normalizes event type string for consistent processing.
     */
    private String normalizeEventType(String eventType) {
        return eventType.trim().toLowerCase()
                .replace("_", "")
                .replace("-", "");
    }

    private void processRunEvent(ScoreDTO s, CricketBall ball) {
        int runs = parseIntSafe(s.getEvent());
        ball.setRuns(runs);
        ball.setExtra(0);
        ball.setExtraType(null);
        ball.setLegalDelivery(true);
    }

    private void processBoundaryEvent(ScoreDTO s, CricketBall ball) {
        int boundary = parseIntSafe(s.getEvent());
        ball.setRuns(boundary);
        ball.setExtra(0);
        ball.setExtraType(null);
        ball.setLegalDelivery(true);
        if (boundary == Constants.BOUNDARY_FOUR) ball.setIsFour(true);
        if (boundary == Constants.BOUNDARY_SIX) ball.setIsSix(true);
    }

    private void processWideEvent(ScoreDTO s, CricketBall ball) {
        int wideRuns = parseIntSafe(s.getEvent());
        int extras = s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : (wideRuns + 1);
        if (extras <= 0) extras = 1;
        ball.setRuns(0);
        ball.setExtra(extras);
        ball.setExtraType(Constants.EXTRA_WIDE);
        ball.setLegalDelivery(false);
    }

    private void processNoBallEvent(ScoreDTO s, CricketBall ball) {
        int noBallValue = parseIntSafe(s.getEvent());
        int batRuns = s.getRunsOnThisBall() > 0 ? s.getRunsOnThisBall() : noBallValue;
        int extras = s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : 1;
        if (extras <= 0) extras = 1;
        ball.setRuns(batRuns);
        ball.setExtra(extras);
        ball.setExtraType(Constants.EXTRA_NO_BALL);
        ball.setLegalDelivery(false);
    }

    private void processByeEvent(ScoreDTO s, CricketBall ball) {
        int byeRuns = parseIntSafe(s.getEvent());
        ball.setRuns(0);
        ball.setExtra(s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : byeRuns);
        ball.setExtraType(Constants.EXTRA_BYE);
        ball.setLegalDelivery(true);
    }

    private void processLegByeEvent(ScoreDTO s, CricketBall ball) {
        int legByeRuns = parseIntSafe(s.getEvent());
        ball.setRuns(0);
        ball.setExtra(s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : legByeRuns);
        ball.setExtraType(Constants.EXTRA_LEG_BYE);
        ball.setLegalDelivery(true);
    }

    private void processWicketEvent(ScoreDTO s, CricketBall ball) {
        String dismissal = (s.getDismissalType() != null && !s.getDismissalType().isBlank())
                ? s.getDismissalType()
                : s.getEvent();
        ball.setDismissalType(dismissal);

        Player outPlayer = null;
        if (s.getOutPlayerId() != null) {
            outPlayer = playerRepo.findActiveById(s.getOutPlayerId()).orElse(null);
        }
        if (outPlayer == null) {
            outPlayer = ball.getBatsman();
        }
        ball.setOutPlayer(outPlayer);

        ball.setRuns(s.getRunsOnThisBall());
        ball.setExtra(s.getExtrasThisBall());
        if (s.getExtraType() != null && !s.getExtraType().isBlank()) {
            ball.setExtraType(s.getExtraType());
        }

        ball.setLegalDelivery(s.getIsLegal() != null ? s.getIsLegal() : Boolean.TRUE);

        // Set fielder for relevant dismissals
        if (dismissal != null && (dismissal.equalsIgnoreCase(Constants.DISMISSAL_CAUGHT) 
            || dismissal.equalsIgnoreCase(Constants.DISMISSAL_RUNOUT) 
            || dismissal.equalsIgnoreCase(Constants.DISMISSAL_STUMPED))) {
            if (s.getFielderId() != null) {
                ball.setFielder(playerRepo.findActiveById(s.getFielderId()).orElse(null));
            }
        }
    }

    /**
     * Evicts tournament awards cache if match belongs to a tournament.
     */
    private void evictCacheIfNeeded(Match match) {
        if (match.getTournament() != null && match.getTournament().getId() != null) {
            Long tournamentId = match.getTournament().getId();
            cacheEvictionService.evictTournamentAwards(tournamentId);
        }
    }

    /**
     * Updates the ScoreDTO with current innings state (runs, overs, wickets, CRR).
     */
    private void updateInningsState(ScoreDTO s, CricketInnings innings) {
        int inningsRuns = cricketBallInterface.sumRunsAndExtrasByInningsId(innings.getId());
        int legalBallsNow = (int) cricketBallInterface.countLegalBallsByInningsId(innings.getId());
        int wicketsNow = (int) cricketBallInterface.countWicketsByInningsId(innings.getId());

        s.setRuns(inningsRuns);
        s.setBalls(legalBallsNow % Constants.BALLS_PER_OVER);
        s.setOvers(legalBallsNow / Constants.BALLS_PER_OVER);
        s.setWickets(wicketsNow);

        // Calculate current run rate
        double crr = legalBallsNow == 0 ? 0.0 : ((double) inningsRuns * Constants.BALLS_PER_OVER) / (double) legalBallsNow;
        s.setCrr(round2(crr));
    }

    /**
     * Handles second innings logic including target, required run rate, and match completion.
     */
    private ScoreDTO handleSecondInnings(ScoreDTO s, Match m, CricketInnings currentInnings, int maxBallsPerInnings) {
        CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), Constants.FIRST_INNINGS);
        int firstRuns = getInningsRuns(firstInnings);
        int target = firstRuns + 1;
        int currentRuns = s.getRuns();
        int remainingRuns = target - currentRuns;
        int legalBallsNow = s.getOvers() * Constants.BALLS_PER_OVER + s.getBalls();
        int remainingBalls = Math.max(0, maxBallsPerInnings - legalBallsNow);

        // Target reached - chasing team wins
        if (remainingRuns < 0) {
            Team chasingTeam = currentInnings.getTeam();
            if (!isMatchFinal(m)) {
                m.setWinnerTeam(chasingTeam);
                m.setStatus(Constants.STATUS_COMPLETED);
                matchRepo.save(m);
                awardService.computeMatchAwards(m.getId());
                matchService.endMatch(m.getId());
            }
            s.setStatus(Constants.STATUS_END_MATCH);
            s.setTarget(0);
            s.setRrr(0.0);
            return s;
        }

        // Overs finished - determine winner
        if (remainingBalls == 0) {
            return handleMatchEndByOvers(s, m, currentInnings, currentRuns, firstRuns, remainingRuns);
        }

        // Calculate required run rate
        double rrr = ((double) remainingRuns * Constants.BALLS_PER_OVER) / (double) remainingBalls;
        s.setRrr(round2(rrr));
        s.setTarget(remainingRuns);

        return s;
    }

    /**
     * Handles match completion when overs are finished in second innings.
     */
    private ScoreDTO handleMatchEndByOvers(ScoreDTO s, Match m, CricketInnings currentInnings, 
                                           int currentRuns, int firstRuns, int remainingRuns) {
        Team otherTeam = getOpposingTeam(m, currentInnings.getTeam());
        
        if (currentRuns > firstRuns) {
            m.setWinnerTeam(currentInnings.getTeam());
        } else if (currentRuns < firstRuns) {
            m.setWinnerTeam(otherTeam);
        } else {
            m.setWinnerTeam(null);
            m.setStatus(Constants.STATUS_TIED);
        }

        if (!isMatchFinal(m)) {
            if (!Constants.STATUS_TIED.equalsIgnoreCase(m.getStatus())) {
                m.setStatus(Constants.STATUS_COMPLETED);
            }
            matchRepo.save(m);
            awardService.computeMatchAwards(m.getId());
            matchService.endMatch(m.getId());
        }

        s.setStatus(Constants.STATUS_END_MATCH);
        s.setTarget(remainingRuns);
        s.setRrr(Double.POSITIVE_INFINITY);
        return s;
    }

    /**
     * Checks if the innings has ended (all out or overs completed).
     */
    private ScoreDTO checkInningsEnd(ScoreDTO s, Match m, CricketInnings currentInnings, int maxBallsPerInnings) {
        int teamPlayers = getTeamPlayerCount(currentInnings.getTeam());
        int maxWickets = Math.max(0, teamPlayers - 1);
        int legalBallsNow = s.getOvers() * Constants.BALLS_PER_OVER + s.getBalls();

        // Check for all-out
        if (s.getWickets() >= maxWickets) {
            return handleAllOut(s, m, currentInnings);
        }

        // Check for overs completed
        if (legalBallsNow >= maxBallsPerInnings) {
            return handleOversCompleted(s, m, currentInnings);
        }

        return s;
    }

    /**
     * Handles innings end when team is all out.
     */
    private ScoreDTO handleAllOut(ScoreDTO s, Match m, CricketInnings currentInnings) {
        if (s.isFirstInnings()) {
            return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
        } else {
            return handleSecondInningsAllOut(s, m, currentInnings);
        }
    }

    /**
     * Handles second innings all-out scenario.
     */
    private ScoreDTO handleSecondInningsAllOut(ScoreDTO s, Match m, CricketInnings currentInnings) {
        CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), Constants.FIRST_INNINGS);
        int firstRuns = getInningsRuns(firstInnings);
        int secondRuns = s.getRuns();

        determineMatchWinner(m, currentInnings, secondRuns, firstRuns);

        if (!isMatchFinal(m)) {
            if (!Constants.STATUS_TIED.equalsIgnoreCase(m.getStatus())) {
                m.setStatus(Constants.STATUS_COMPLETED);
            }
            matchRepo.save(m);
            awardService.computeMatchAwards(m.getId());
            matchService.endMatch(m.getId());
        }

        s.setStatus(Constants.STATUS_END_MATCH);
        s.setTarget(Math.max(0, (firstRuns + 1) - secondRuns));
        s.setRrr(Double.POSITIVE_INFINITY);
        return s;
    }

    /**
     * Handles innings end when overs are completed.
     */
    private ScoreDTO handleOversCompleted(ScoreDTO s, Match m, CricketInnings currentInnings) {
        if (s.isFirstInnings()) {
            return handleEndOfInningsAndMaybeCreateNext(s, m, currentInnings);
        } else {
            return handleSecondInningsOversComplete(s, m, currentInnings);
        }
    }

    /**
     * Handles second innings overs completed scenario.
     */
    private ScoreDTO handleSecondInningsOversComplete(ScoreDTO s, Match m, CricketInnings currentInnings) {
        CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(m.getId(), Constants.FIRST_INNINGS);
        int firstRuns = getInningsRuns(firstInnings);
        int secondRuns = s.getRuns();

        determineMatchWinner(m, currentInnings, secondRuns, firstRuns);

        if (!isMatchFinal(m)) {
            if (!Constants.STATUS_TIED.equalsIgnoreCase(m.getStatus())) {
                m.setStatus(Constants.STATUS_COMPLETED);
            }
            matchRepo.save(m);
            awardService.computeMatchAwards(m.getId());
            matchService.endMatch(m.getId());
        }

        s.setStatus(Constants.STATUS_END_MATCH);
        return s;
    }

    /**
     * Determines match winner based on runs scored.
     */
    private void determineMatchWinner(Match m, CricketInnings currentInnings, int currentRuns, int firstRuns) {
        if (currentRuns > firstRuns) {
            m.setWinnerTeam(currentInnings.getTeam());
        } else if (currentRuns < firstRuns) {
            Team otherTeam = getOpposingTeam(m, currentInnings.getTeam());
            m.setWinnerTeam(otherTeam);
        } else {
            m.setWinnerTeam(null);
            m.setStatus(Constants.STATUS_TIED);
        }
    }

    /**
     * Gets the total runs scored in an innings.
     */
    private int getInningsRuns(CricketInnings innings) {
        if (innings == null) return 0;
        return cricketBallInterface.sumRunsAndExtrasByInningsId(innings.getId());
    }

    /**
     * Gets the opposing team in a match.
     */
    private Team getOpposingTeam(Match match, Team currentTeam) {
        if (currentTeam == null || match.getTeam1() == null || match.getTeam2() == null) {
            return null;
        }
        return match.getTeam1().getId().equals(currentTeam.getId()) ? match.getTeam2() : match.getTeam1();
    }

    /**
     * Gets the number of players in a team.
     */
    private int getTeamPlayerCount(Team team) {
        int teamPlayers = Constants.DEFAULT_TEAM_SIZE;
        try {
            if (team != null) {
                Optional<Team> teamOpt = teamRepo.findById(team.getId());
                if (teamOpt.isPresent()) {
                    Team teamEntity = teamOpt.get();
                    if (teamEntity.getPlayers() != null) {
                        teamPlayers = (int) teamEntity.getPlayers().size();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return teamPlayers;
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

    /**
     * Gets the current match state for a given match and innings.
     * Used to send initial state when a client connects.
     *
     * @param matchId Match ID
     * @return ScoreDTO with current match state
     */
    @Cacheable(value = "matchState", key = "#matchId")
    public ScoreDTO getCurrentMatchState(Long matchId) {
        ScoreDTO state = new ScoreDTO();

        Match m = matchRepo.findById(matchId).orElse(null);
        if (m == null) {
            state.setStatus(Constants.STATUS_ERROR);
            state.setComment(Constants.ERROR_MATCH_NOT_FOUND);
            return state;
        }

        state.setMatchId(matchId);

        // Determine current innings
        CricketInnings secondInnings = cricketInningsRepo.findByMatchIdAndNo(matchId, Constants.SECOND_INNINGS);
        CricketInnings currentInnings;
        boolean isFirstInnings;

        if (secondInnings != null && secondInnings.getId() != null) {
            currentInnings = secondInnings;
            isFirstInnings = false;
        } else {
            currentInnings = cricketInningsRepo.findByMatchIdAndNo(matchId, Constants.FIRST_INNINGS);
            isFirstInnings = true;
        }

        if (currentInnings == null) {
            state.setStatus(Constants.STATUS_ERROR);
            state.setComment(Constants.ERROR_INNINGS_NOT_FOUND);
            return state;
        }

        state.setInningsId(currentInnings.getId());
        state.setFirstInnings(isFirstInnings);
        state.setTeamId(currentInnings.getTeam() != null ? currentInnings.getTeam().getId() : null);

        // Calculate current state
        int inningsRuns = cricketBallInterface.sumRunsAndExtrasByInningsId(currentInnings.getId());
        int legalBallsNow = (int) cricketBallInterface.countLegalBallsByInningsId(currentInnings.getId());
        int wicketsNow = (int) cricketBallInterface.countWicketsByInningsId(currentInnings.getId());

        state.setRuns(inningsRuns);
        state.setBalls(legalBallsNow % Constants.BALLS_PER_OVER);
        state.setOvers(legalBallsNow / Constants.BALLS_PER_OVER);
        state.setWickets(wicketsNow);

        // Calculate CRR
        double crr = legalBallsNow == 0 ? 0.0 : ((double) inningsRuns * Constants.BALLS_PER_OVER) / (double) legalBallsNow;
        state.setCrr(round2(crr));

        // Calculate target and RRR for second innings
        if (!isFirstInnings) {
            CricketInnings firstInnings = cricketInningsRepo.findByMatchIdAndNo(matchId, Constants.FIRST_INNINGS);
            int firstRuns = getInningsRuns(firstInnings);
            int target = firstRuns + 1;
            int remainingRuns = target - inningsRuns;

            int maxBallsPerInnings = calculateMaxBalls(m);
            int remainingBalls = Math.max(0, maxBallsPerInnings - legalBallsNow);

            state.setTarget(remainingRuns);
            if (remainingBalls > 0) {
                double rrr = ((double) remainingRuns * Constants.BALLS_PER_OVER) / (double) remainingBalls;
                state.setRrr(round2(rrr));
            } else {
                state.setRrr(0.0);
            }
        } else {
            state.setTarget(inningsRuns);
            state.setRrr(0.0);
        }

        // Set match status
        if (isMatchFinal(m)) {
            state.setStatus(Constants.STATUS_END_MATCH);
        } else {
            state.setStatus("LIVE");
        }

        return state;
    }

    /**
     * Undoes the last ball delivery for a given match and innings.
     * Removes the most recent ball entry and recalculates match state.
     *
     * @param matchId Match ID
     * @param inningsId Innings ID
     * @return ScoreDTO with updated match state after undo
     */
    @CacheEvict(value = "matchState", key = "#matchId")
    @Transactional
    public ScoreDTO undoLastBall(Long matchId, Long inningsId) {
        ScoreDTO result = new ScoreDTO();
        result.setMatchId(matchId);
        result.setInningsId(inningsId);

        // Validate match
        Match m = matchRepo.findById(matchId).orElse(null);
        if (m == null) {
            result.setStatus(Constants.STATUS_ERROR);
            result.setComment(Constants.ERROR_MATCH_NOT_FOUND);
            return result;
        }

        // Validate innings
        CricketInnings innings = cricketInningsRepo.findById(inningsId).orElse(null);
        if (innings == null) {
            result.setStatus(Constants.STATUS_ERROR);
            result.setComment(Constants.ERROR_INNINGS_NOT_FOUND);
            return result;
        }

        // Find and delete the last ball
        List<CricketBall> balls = cricketBallInterface.findByInnings_IdOrderByIdDesc(inningsId);
        if (balls == null || balls.isEmpty()) {
            result.setStatus(Constants.STATUS_ERROR);
            result.setComment("No balls to undo");
            return result;
        }

        CricketBall lastBall = balls.get(0);
        cricketBallInterface.delete(lastBall);

        // Evict tournament awards cache if needed
        // Note: We don't call statsService.updateTournamentStats here because:
        // 1. The ball entity is deleted and no longer valid for queries
        // 2. Stats will be recalculated from remaining balls on next aggregation
        if (m.getTournament() != null && m.getTournament().getId() != null) {
            cacheEvictionService.evictTournamentAwards(m.getTournament().getId());
        }

        // Get updated match state (recalculated from remaining balls)
        return getCurrentMatchState(matchId);
    }
}
