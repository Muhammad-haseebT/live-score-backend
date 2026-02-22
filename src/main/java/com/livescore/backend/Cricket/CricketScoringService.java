package com.livescore.backend.Cricket;

import com.livescore.backend.DTO.PlayerStatDTO;
import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.Cricket.MatchStateInterface;
import com.livescore.backend.Interface.Cricket.PlayerInningsInterface;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.CricketInningsInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Service.MatchService;
import com.livescore.backend.Service.StatsService;
import com.livescore.backend.Util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CricketScoringService {
    private final MatchStateInterface matchStateInterface;
    private final PlayerInningsInterface playerInningsInterface;
    private final CricketInningsInterface cricketInningsInterface;
    private final StatsService statsService;
    private final PlayerInterface playerInterface;
    private final CricketBallInterface cricketBallInterface;
    private final MatchInterface matchInterface;
    private final MatchService matchService;


    @Transactional(readOnly = true)
    @Cacheable(value = "matchStates", key = "#matchId")
    public ScoreDTO getCurrentMatchState(Long matchId) {
        Match m = matchInterface.findById(matchId).get();
        int size = m.getCricketInnings().size();
        MatchState state = matchStateInterface.findByInnings_Id(m.getCricketInnings().get(size - 1).getId());
        if (state == null) {
            ScoreDTO s = new ScoreDTO();
            s.setMatchId(matchId);
            s.setComment("Error no current State");
            s.setInningsId(m.getCricketInnings().get(size - 1).getId());
            return s;
        }

        return convertToScoreDTO(state, false, false, "");
    }

    private ScoreDTO convertToScoreDTO(MatchState state, Boolean rotate, boolean a, String b) {
        ScoreDTO scoreDTO = new ScoreDTO();

        scoreDTO.setMatchId(state.getInnings().getMatch().getId());
        scoreDTO.setInningsId(state.getInnings().getId());
        scoreDTO.setWickets(state.getWickets());
        scoreDTO.setRuns(state.getRuns());
        scoreDTO.setBalls(state.getBalls());
        scoreDTO.setOvers(state.getOvers());
        scoreDTO.setStatus(state.getStatus());
        scoreDTO.setCrr(state.getCrr());
        scoreDTO.setTarget(state.getTarget());
        scoreDTO.setExtra(state.getExtras());
        scoreDTO.setFirstInnings((state.getInnings().getNo() == 1));

        if (!a) {
            PlayerStatDTO batsmanDto = new PlayerStatDTO();
            PlayerStatDTO nonStrikerDto = new PlayerStatDTO();
            PlayerStatDTO bowlerDto = new PlayerStatDTO();
            PlayerInnings batsman = playerInningsInterface.findByInnings_IdAndPlayer_Id(state.getInnings().getId(), state.getStriker().getId());
            PlayerInnings bowler = playerInningsInterface.findByInnings_IdAndPlayer_Id(state.getInnings().getId(), state.getBowler().getId());
            PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(state.getInnings().getId(), state.getNonStriker().getId());


            if (batsman != null && batsman.getPlayer() != null) {
                scoreDTO.setBatsmanId(batsman.getPlayer().getId());
                batsmanDto.setRuns(batsman.getRuns());
                batsmanDto.setSixes(batsman.getSixes());
                batsmanDto.setFours(batsman.getFour());
                batsmanDto.setBallsFaced(batsman.getBallsFaced());
                batsmanDto.setPlayerId(batsman.getPlayer().getId());
                batsmanDto.setPlayerName(batsman.getPlayer().getName());
                batsmanDto.setStrikeRate(batsman.getBallsFaced() > 0
                        ? (batsman.getRuns() * 100.0) / batsman.getBallsFaced() : 0);
            }
            if (nonStriker != null && nonStriker.getPlayer() != null) {
                scoreDTO.setNonStrikerId(nonStriker.getPlayer().getId());
                nonStrikerDto.setRuns(nonStriker.getRuns());
                nonStrikerDto.setSixes(nonStriker.getSixes());
                nonStrikerDto.setFours(nonStriker.getFour());
                nonStrikerDto.setBallsFaced(nonStriker.getBallsFaced());
                nonStrikerDto.setPlayerId(nonStriker.getPlayer().getId());
                nonStrikerDto.setPlayerName(nonStriker.getPlayer().getName());
                nonStrikerDto.setStrikeRate(nonStriker.getBallsFaced() > 0
                        ? (nonStriker.getRuns() * 100.0) / nonStriker.getBallsFaced() : 0);
            }
            if (bowler != null && bowler.getPlayer() != null) {
                scoreDTO.setBowlerId(bowler.getPlayer().getId());
                bowlerDto.setRunsConceded(bowler.getRunsConceded());
                bowlerDto.setWickets(bowler.getWickets());
                bowlerDto.setBallsBowled(bowler.getBallsBowled());
                bowlerDto.setPlayerId(bowler.getPlayer().getId());
                bowlerDto.setPlayerName(bowler.getPlayer().getName());
                bowlerDto.setEconomy(bowler.getBallsBowled() > 0
                        ? (bowler.getRunsConceded() * 6.0) / bowler.getBallsBowled() : 0);
            }
            if (rotate) {
                scoreDTO.setBatsmanId(nonStriker.getPlayer().getId());
                scoreDTO.setNonStrikerId(batsman.getPlayer().getId());
            }

            scoreDTO.setBatsman1Stats(batsmanDto);
            scoreDTO.setBatsman2Stats(nonStrikerDto);
            scoreDTO.setBowlerStats(bowlerDto);
        }
        scoreDTO.setCricketBalls(cricketBallInterface.getBalls(scoreDTO.getInningsId(), scoreDTO.getMatchId()));
        if (!a) {
            int o = matchInterface.findById(scoreDTO.getMatchId()).get().getOvers() * 6;
            int balls = scoreDTO.getOvers() * 6 + scoreDTO.getBalls();

            if (scoreDTO.isFirstInnings() && (balls >= o || scoreDTO.getWickets() == 10)) {
                scoreDTO.setComment("End_Innings");
                state.setTarget(state.getTarget() + 1);
                matchStateInterface.save(state);
            }

            if (!scoreDTO.isFirstInnings()) {
                if (scoreDTO.getWickets() == 10 || balls >= o || scoreDTO.getRuns() >= scoreDTO.getTarget()) {
                    scoreDTO.setComment("End_Innings");
                }
                if (scoreDTO.getWickets() == 10 || balls >= 0) {
                    if (scoreDTO.getRuns() == scoreDTO.getTarget() - 1) {
                        scoreDTO.setComment("Super_Over");
                    }
                }
            }
        }
        return scoreDTO;
    }

    @CacheEvict(value = "matchStates", key = "#matchId")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScoreDTO undoLastBall(Long matchId, Long inningsId) {
        CricketBall cb = cricketBallInterface.findLastBallInInnings(inningsId);
        if (cb == null) {
            return getCurrentMatchState(matchId);
        }

        MatchState m = matchStateInterface.findByInnings_Id(inningsId);
        PlayerInnings batsman = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getBatsman().getId());
        PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getNonStriker().getId());
        PlayerInnings bowler = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getBowler().getId());

        // grab IDs BEFORE delete for stats recalculation
        Long undoBatsmanId = cb.getBatsman() != null ? cb.getBatsman().getId() : null;
        Long undoBowlerId = cb.getBowler() != null ? cb.getBowler().getId() : null;
        Long undoFielderId = cb.getFielder() != null ? cb.getFielder().getId() : null;
        Long tournamentId = cb.getMatch().getTournament().getId();

        int r = Integer.parseInt(cb.getEvent());

        switch (cb.getEventType()) {
            case "run":
            case "boundary":
                batsman.setRuns(batsman.getRuns() - r);
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                if (r == 4) batsman.setFour(batsman.getFour() - 1);
                else if (r == 6) batsman.setSixes(batsman.getSixes() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
                batsman.setRr(batsman.getBallsFaced() > 0
                        ? (double) batsman.getRuns() / batsman.getBallsFaced() : 0);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setRuns(m.getRuns() - r);
                decrementBall(m);
                break;

            case "bye":
            case "legbye":
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
                batsman.setRr(batsman.getBallsFaced() > 0
                        ? (double) batsman.getRuns() / batsman.getBallsFaced() : 0);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setExtras(m.getExtras() - r);
                m.setRuns(m.getRuns() - r);
                decrementBall(m);
                break;

            case "noball":
                m.setExtras(m.getExtras() - 1);
                batsman.setRuns(batsman.getRuns() - r);
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r - 1);
                if (r == 4) batsman.setFour(batsman.getFour() - 1);
                if (r == 6) batsman.setSixes(batsman.getSixes() - 1);
                batsman.setRr(batsman.getBallsFaced() > 0
                        ? (double) batsman.getRuns() / batsman.getBallsFaced() : 0);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setRuns(m.getRuns() - r - 1);
                break;

            case "wide":
                m.setExtras(m.getExtras() - r - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r - 1);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setRuns(m.getRuns() - r - 1);
                break;

            case "wicket":
                handleUndoWicket(m, batsman, bowler, nonStriker, cb);
                break;
        }

        // restore striker/nonStriker/bowler on MatchState
        if (!cb.getEventType().equals("wicket")) {
            m.setStriker(batsman.getPlayer());
            m.setNonStriker(nonStriker.getPlayer());
            m.setBowler(bowler.getPlayer());
        }

        // safe rate calculations — avoid division by zero
        int totalBalls = (m.getOvers() * 6) + m.getBalls();
        if (totalBalls > 0) {
            m.setRr((double) m.getRuns() / totalBalls);
            m.setCrr((double) m.getRuns() * 6 / totalBalls);
        } else {
            m.setRr(0.0);
            m.setCrr(0.0);
        }

        // target adjustment
        if (m.getInnings().getNo() == 1) {
            // first innings: target was being built up, so subtract
            m.setTarget(m.getTarget() + r);
        } else {
            // second innings: target is what's needed, so ADD back
            m.setTarget(m.getTarget() - r);
            if (totalBalls > 0) {
                m.setRequiredRR((double) m.getTarget() * 6 / totalBalls);
            } else {
                m.setRequiredRR(0.0);
            }
        }

        // delete the ball first, then save everything
        cricketBallInterface.delete(cb);
        matchStateInterface.save(m);
        playerInningsInterface.save(nonStriker);
        playerInningsInterface.save(batsman);
        playerInningsInterface.save(bowler);

        ScoreDTO s = convertToScoreDTO(m, false, false, "");

        // recalculate stats from scratch instead of per-ball update
        // (ball is already deleted so updateTournamentStats would find nothing)
        if (undoBatsmanId != null) {
            statsService.recalculatePlayerStats(undoBatsmanId, tournamentId);
        }
        if (undoBowlerId != null && !undoBowlerId.equals(undoBatsmanId)) {
            statsService.recalculatePlayerStats(undoBowlerId, tournamentId);
        }
        if (undoFielderId != null
                && !undoFielderId.equals(undoBatsmanId)
                && !undoFielderId.equals(undoBowlerId)) {
            statsService.recalculatePlayerStats(undoFielderId, tournamentId);
        }

        return s;
    }

    private void handleUndoWicket(MatchState m, PlayerInnings batsman, PlayerInnings bowler, PlayerInnings nonStriker, CricketBall cb) {
        switch (cb.getDismissalType().toLowerCase()) {
            case "runout":
            case "bowled":
            case "caught":
            case "hitwicket":
            case "stumped":
            case "lbw":
            case "overthefence":
            case "onehandonebounce":
                decrementBall(m);
                bowler.setWickets(bowler.getWickets() - 1);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
            case "retired":
            case "mankad":
                m.setWickets(m.getWickets() - 1);
                if (cb.getBatsman().getId().equals(cb.getOutPlayer().getId())) {
                    m.setStriker(cb.getOutPlayer());
                    m.setNonStriker(cb.getNonStriker());
                } else {
                    m.setStriker(cb.getBatsman());
                    m.setNonStriker(cb.getOutPlayer());
                }
                if (cb.getDismissalType().equalsIgnoreCase("runout"))
                    batsman.setRuns(batsman.getRuns() - cb.getRuns());
                batsman.setBallsFaced(batsman.getBallsFaced());
                batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
                if (cb.getRuns() == 4) batsman.setFour(batsman.getFour() - 1);
                if (cb.getRuns() == 6) batsman.setSixes(batsman.getSixes() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - cb.getRuns());

                bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());
                m.setRuns(m.getRuns() - cb.getRuns());

        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "matchStates", key = "#result.matchId")
    public ScoreDTO scoring(ScoreDTO score) {

        // ------- 1. Fetch all Player entities ONCE -------
        Player batsmanPlayer = playerInterface.findActiveById(score.getBatsmanId()).get();
        Player bowlerPlayer = playerInterface.findActiveById(score.getBowlerId()).get();
        Player nonStrikerPlayer = playerInterface.findActiveById(score.getNonStrikerId()).get();
        Player outPlayer = score.getOutPlayerId() != null
                ? playerInterface.findActiveById(score.getOutPlayerId()).get() : null;
        Player newPlayer = score.getNewPlayerId() != null
                ? playerInterface.findActiveById(score.getNewPlayerId()).get() : null;
        Player fielderPlayer = score.getFielderId() != null
                ? playerInterface.findActiveById(score.getFielderId()).get() : null;
        // ------- 2. Fetch match & innings ONCE -------
        Match match = matchInterface.findById(score.getMatchId()).get();
        CricketInnings ci = cricketInningsInterface.findById(score.getInningsId()).get();

        // ------- 3. Fetch MatchState & PlayerInnings -------
        MatchState m = matchStateInterface.findByInnings_Id(score.getInningsId());
        PlayerInnings batsman = playerInningsInterface.findByInnings_IdAndPlayer_Id(score.getInningsId(), score.getBatsmanId());
        PlayerInnings bowler = playerInningsInterface.findByInnings_IdAndPlayer_Id(score.getInningsId(), score.getBowlerId());
        PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(score.getInningsId(), score.getNonStrikerId());

        if (m == null) m = new MatchState();
        if (batsman == null) batsman = new PlayerInnings();
        if (bowler == null) bowler = new PlayerInnings();
        if (nonStriker == null) nonStriker = new PlayerInnings();
        if (m.getStatus() == null) m.setStatus("LIVE");

        if (batsman.getPlayer() == null) batsman.setPlayer(batsmanPlayer);
        if (bowler.getPlayer() == null) bowler.setPlayer(bowlerPlayer);
        if (nonStriker.getPlayer() == null) nonStriker.setPlayer(nonStrikerPlayer);

        batsman.setInnings(ci);
        bowler.setInnings(ci);

        nonStriker.setInnings(ci);
        if (score.getDismissalType() != null) {
            String r = score.getDismissalType().replace(" ", "");
            score.setDismissalType(r);
            System.out.println(r);
        }
        // ------- 4. Set striker/nonStriker/bowler on MatchState using pre-fetched players -------
        m.setBowler(bowlerPlayer);
        if (outPlayer == null) {
            m.setStriker(batsmanPlayer);
            m.setNonStriker(nonStrikerPlayer);
        } else {

            if (Objects.equals(score.getOutPlayerId(), score.getBatsmanId())) {
                m.setStriker(newPlayer);
                m.setNonStriker(nonStrikerPlayer);

            } else {
                m.setStriker(batsmanPlayer);
                m.setNonStriker(newPlayer);
            }

            PlayerInnings existingNewPlayer = playerInningsInterface
                    .findByInnings_IdAndPlayer_Id(score.getInningsId(), newPlayer.getId());
            if (existingNewPlayer == null) {
                PlayerInnings newPlayerInnings = new PlayerInnings();
                newPlayerInnings.setPlayer(newPlayer);
                newPlayerInnings.setInnings(ci);
                playerInningsInterface.save(newPlayerInnings);
            }

        }

        // ------- 5. Build a context object to pass pre-fetched entities -------
        BallContext ctx = new BallContext(batsmanPlayer, bowlerPlayer, nonStrikerPlayer,
                outPlayer, fielderPlayer, match, ci);

        CricketBall cricketBall = new CricketBall();
        m.setInnings(ci);

        MatchState matchState = processEvent(score, m, cricketBall, batsman, bowler, ctx);
        m = matchState;
        boolean a = "End_Innings".equals(score.getEventType());
        if (a) {
            score.setComment("");

        }


        boolean shouldRotate = false;
        if (!a) {

            cricketBall.setInnings(ci);
            bowler.setRole("BOWLER");
            cricketBallInterface.save(cricketBall);
            matchStateInterface.save(m);
            playerInningsInterface.save(nonStriker);
            playerInningsInterface.save(batsman);
            playerInningsInterface.save(bowler);
            shouldRotate = checkRotate(Integer.parseInt(score.getEvent()));

        }
        ScoreDTO s = convertToScoreDTO(m, shouldRotate, a, "");
        return s;
    }

    private MatchState processEvent(ScoreDTO score, MatchState m, CricketBall c,
                                    PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        boolean a = false;
        switch (score.getEventType()) {
            case "run":
            case "boundary":
                addScore(score, m, c, batsman, bowler, ctx);
                break;
            case "wide":
            case "noball":
            case "legbye":
            case "bye":
                handleExtras(score, m, c, batsman, bowler, ctx);
                break;
            case "wicket":
                handleWickets(score, m, c, batsman, bowler, ctx);
                break;
            case "End_Innings":
                a = true;
                if (score.isFirstInnings()) {
                    score.setFirstInnings(false);
                    Match match = ctx.match;
                    MatchState newState = new MatchState();
                    CricketInnings cricketInnings = new CricketInnings();
                    cricketInnings.setNo(2);
                    cricketInnings.setMatch(match);
                    cricketInnings.setTeam(Objects.equals(score.getTeamId(), match.getTeam1().getId()) ? match.getTeam2() : match.getTeam1());
                    cricketInnings = cricketInningsInterface.save(cricketInnings);
                    newState.setInnings(cricketInnings);
                    newState.setRuns(0);
                    newState.setWickets(0);
                    newState.setRr(0.0);
                    newState.setCrr(0.0);
                    newState.setExtras(0);
                    newState.setOvers(0);
                    newState.setBalls(0);

                    m = matchStateInterface.save(newState);
                    score.setComment("");


                } else {
                    Match match = ctx.match;
                    if (score.getRuns() >= score.getTarget()) {
                        match.setWinnerTeam(match.getCricketInnings().get(1).getTeam());
                    } else if (score.getRuns() < score.getTarget()) {
                        match.setWinnerTeam(match.getCricketInnings().get(0).getTeam());

                    }
                    match.setStatus(Constants.STATUS_COMPLETED);
                    matchService.endMatch(match.getId());
                }
        }


        c.setEvent(score.getEvent());

        c.setEventType(score.getEventType());
        return m;
    }

    private void handleWickets(ScoreDTO score, MatchState m, CricketBall c,
                               PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        String d = score.getDismissalType().toLowerCase().trim();
        switch (d) {
            case "bowled":
            case "caught":
            case "hitwicket":
            case "stumped":
            case "lbw":
            case "overthefence":
            case "onehandonebounce":
            case "runout":
            case "retired":
            case "mankad":
                handleNormalWickets(score, m, c, batsman, bowler, ctx);
                break;
        }
    }

    private void handleNormalWickets(ScoreDTO score, MatchState m, CricketBall c,
                                     PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        String r = score.getDismissalType().toLowerCase();
        c.setDismissalType(score.getDismissalType());
        c.setBatsman(batsman.getPlayer());
        c.setNonStriker(ctx.nonStriker);
        c.setOutPlayer(ctx.outPlayer);  // ✅ No DB call
        c.setBowler(bowler.getPlayer());
        if (ctx.fielder != null)
            c.setFielder(ctx.fielder);  // ✅ No DB call
        c.setRuns(score.getRunsOnThisBall());
        c.setLegalDelivery(true);
        if (!r.equals("retired") && !r.equals("mankad")) {
            incrementBall(m);
            bowler.setBallsBowled(bowler.getBallsBowled() + 1);
            bowler.setWickets(bowler.getWickets() + 1);

        }
        if (score.getDismissalType().equalsIgnoreCase("runout")) {
            batsman.setRuns(batsman.getRuns() + score.getRunsOnThisBall());
        }
        c.setBallNumber(m.getBalls());
        c.setOverNumber(m.getOvers());
        c.setMatch(ctx.match);       // ✅ No DB call
        c.setInnings(ctx.innings);   // ✅ No DB call
        c.setEventType("wicket");

        m.setWickets(m.getWickets() + 1);
        if (m.getWickets() == 10) {
            m.setStatus("WICKET-OUT");
        }
        m.setRuns(m.getRuns() + score.getRunsOnThisBall());
        if (score.isFirstInnings()) {
            m.setTarget(m.getTarget() + score.getRunsOnThisBall());
        } else {
            m.setTarget(m.getTarget() - score.getRunsOnThisBall());
            m.setRequiredRR((double) m.getTarget() * 6 / ((m.getOvers() * 6) + m.getBalls()));
        }
        bowler.setRunsConceded(bowler.getRunsConceded() + score.getRunsOnThisBall());


        m.setCrr((double) m.getRuns() * 6 / ((m.getOvers() * 6) + m.getBalls()));

    }

    private void addScore(ScoreDTO score, MatchState m, CricketBall c,
                          PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        int r = Integer.parseInt(score.getEvent());
        m.setRuns(m.getRuns() + r);

        if (score.isFirstInnings()) {
            m.setTarget(m.getTarget() + r);
        }
        incrementBall(m);

        c.setNonStriker(ctx.nonStriker);  // ✅ No DB call
        c.setRuns(r);
        c.setBatsman(ctx.batsman);        // ✅ No DB call
        c.setBowler(ctx.bowler);          // ✅ No DB call
        c.setLegalDelivery(true);
        c.setBallNumber(m.getBalls());
        c.setOverNumber(m.getOvers());
        c.setMatch(ctx.match);            // ✅ No DB call

        bowler.setRunsConceded(bowler.getRunsConceded() + r);
        bowler.setBallsBowled(bowler.getBallsBowled() + 1);
        bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());

        batsman.setRuns(batsman.getRuns() + r);
        batsman.setBallsFaced(batsman.getBallsFaced() + 1);
        batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());

        m.setCrr((double) m.getRuns() * 6 / ((m.getOvers() * 6) + m.getBalls()));
        if (r == 4) {
            c.setIsFour(true);
            batsman.setFour(batsman.getFour() + 1);
        } else if (r == 6) {
            c.setIsSix(true);
            batsman.setSixes(batsman.getSixes() + 1);
        }

    }

    private void handleExtras(ScoreDTO score, MatchState m, CricketBall c,
                              PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        int r = Integer.parseInt(score.getEvent());


        if (score.isFirstInnings()) {
            m.setTarget(m.getTarget() + r);
        }

        if (!score.getEventType().equalsIgnoreCase("wide") && !score.getEventType().equalsIgnoreCase("noball")) {
            bowler.setBallsBowled(bowler.getBallsBowled() + 1);
            bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());
            batsman.setBallsFaced(batsman.getBallsFaced() + 1);
            batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
            m.setExtras(m.getExtras() + r);
            incrementBall(m);
            c.setExtraType(score.getEventType());
            c.setLegalDelivery(true);
            m.setRuns(m.getRuns() + r);
        } else {
            bowler.setRunsConceded(bowler.getRunsConceded() + r + 1);
            bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());

            if (score.getEventType().equalsIgnoreCase("wide")) {
                m.setExtras(m.getExtras() + r + 1);
                m.setRuns(m.getRuns() + r + 1);
                c.setExtraType("wide");
            } else {
                m.setExtras(m.getExtras() + 1);
                m.setRuns(m.getRuns() + r + 1);
                batsman.setRuns(batsman.getRuns() + r);
                batsman.setBallsFaced(batsman.getBallsFaced() + 1);
                batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
                if (r == 4) batsman.setFour(batsman.getFour() + 1);
                else if (r == 6) batsman.setSixes(batsman.getSixes() + 1);
                c.setExtraType("noball");
                c.setExtra(1);
            }
            c.setLegalDelivery(false);
        }

        if (r == 4) c.setIsFour(true);
        else if (r == 6) c.setIsSix(true);
        c.setRuns(r);
        c.setNonStriker(ctx.nonStriker);  // ✅ No DB call
        c.setBatsman(ctx.batsman);        // ✅ No DB call
        c.setBowler(ctx.bowler);          // ✅ No DB call
        c.setMatch(ctx.match);            // ✅ No DB call
        c.setBallNumber(m.getBalls());
        c.setOverNumber(m.getOvers());

        m.setCrr((double) m.getRuns() * 6 / ((m.getOvers() * 6) + m.getBalls()));

    }

    void incrementBall(MatchState m) {
        if (m.getBalls() + 1 >= 6) {
            m.setOvers(m.getOvers() + 1);
            m.setBalls(0);
        } else {
            m.setBalls(m.getBalls() + 1);
        }
    }

    void decrementBall(MatchState m) {
        if (m.getBalls() == 0) {
            m.setOvers(m.getOvers() - 1);
            m.setBalls(5);
        } else {
            m.setBalls(m.getBalls() - 1);
        }
    }

    private boolean checkRotate(int r) {
        return r % 2 != 0;
    }


    // ------- Inner class to hold pre-fetched entities -------
    private static class BallContext {
        final Player batsman;
        final Player bowler;
        final Player nonStriker;
        final Player outPlayer;
        final Player fielder;
        final Match match;
        final CricketInnings innings;

        BallContext(Player batsman, Player bowler, Player nonStriker,
                    Player outPlayer, Player fielder, Match match, CricketInnings innings) {
            this.batsman = batsman;
            this.bowler = bowler;
            this.nonStriker = nonStriker;
            this.outPlayer = outPlayer;
            this.fielder = fielder;
            this.match = match;
            this.innings = innings;
        }
    }

}