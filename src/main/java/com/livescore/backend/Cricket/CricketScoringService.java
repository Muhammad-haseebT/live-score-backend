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
import com.livescore.backend.Service.StatsService;
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
        Long inningsId = state.getInnings().getId();
        PlayerInnings batsman = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, state.getStriker().getId());
        PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, state.getNonStriker().getId());
        PlayerInnings bowler = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, state.getBowler().getId());

        if (batsman == null) batsman = new PlayerInnings();
        if (nonStriker == null) nonStriker = new PlayerInnings();
        if (bowler == null) bowler = new PlayerInnings();

        return convertToScoreDTO(state, batsman, bowler, nonStriker, false);
    }

    private ScoreDTO convertToScoreDTO(MatchState state, PlayerInnings batsman, PlayerInnings bowler, PlayerInnings nonStriker, Boolean rotate) {
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

        PlayerStatDTO batsmanDto = new PlayerStatDTO();
        PlayerStatDTO nonStrikerDto = new PlayerStatDTO();
        PlayerStatDTO bowlerDto = new PlayerStatDTO();

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
        return scoreDTO;
    }

    @CacheEvict(value = "matchStates", key = "#matchId")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScoreDTO undoLastBall(Long matchId, Long inningsId) {
        CricketBall cb = cricketBallInterface.findLastBallInInnings(inningsId);
        MatchState m = matchStateInterface.findByInnings_Id(inningsId);
        PlayerInnings batsman = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getBatsman().getId());
        PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getNonStriker().getId());
        PlayerInnings bowler = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getBowler().getId());

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
                batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
                bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());
                decrementBall(m);
                break;
            case "bye":
            case "legbye":
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
                m.setExtras(m.getExtras() - r);
                decrementBall(m);
                break;
            case "noball":
                m.setExtras(m.getExtras() - 1);
                batsman.setRuns(batsman.getRuns() - r);
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r - 1);
                if (r == 4) batsman.setFour(batsman.getFour() - 1);
                if (r == 6) batsman.setSixes(batsman.getSixes() - 1);
                break;
            case "wide":
                m.setExtras(m.getExtras() - r - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r - 1);
                break;
            case "wicket":
                handleUndoWicket(m, batsman, bowler, nonStriker, cb);
                break;
        }
        m.setRuns(m.getRuns() - r);
        m.setRr((double) m.getRuns() / m.getBalls());
        m.setTarget(m.getTarget() - r);

        cricketBallInterface.delete(cb);
        matchStateInterface.save(m);
        playerInningsInterface.save(nonStriker);
        playerInningsInterface.save(batsman);
        playerInningsInterface.save(bowler);

        ScoreDTO s = convertToScoreDTO(m, batsman, bowler, nonStriker, false);
        statsService.updateTournamentStats(cb.getId());
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
            case "retired":
            case "mankad":
                m.setWickets(m.getWickets() - 1);
                if (cb.getBatsman().getId().equals(cb.getOutPlayer().getId())) {
                    m.setStriker(cb.getOutPlayer());
                    m.setNonStriker(cb.getNonStriker());
                    if (cb.getDismissalType().equalsIgnoreCase("runout"))
                        batsman.setRuns(batsman.getRuns() - cb.getRuns());
                    batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                    batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
                    if (cb.getRuns() == 4) batsman.setFour(batsman.getFour() - 1);
                    if (cb.getRuns() == 6) batsman.setSixes(batsman.getSixes() - 1);
                    bowler.setRunsConceded(bowler.getRunsConceded() - cb.getRuns());
                    bowler.setBallsBowled(bowler.getBallsBowled() - 1);
                    bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());
                } else {
                    m.setStriker(cb.getBatsman());
                    m.setNonStriker(cb.getOutPlayer());
                }
        }
    }

    // =============================================
    // KEY CHANGE: scoring() method - pre-fetch all entities ONCE
    // =============================================
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
        }

        // ------- 5. Build a context object to pass pre-fetched entities -------
        BallContext ctx = new BallContext(batsmanPlayer, bowlerPlayer, nonStrikerPlayer,
                outPlayer, fielderPlayer, match, ci);

        CricketBall cricketBall = new CricketBall();
        m.setInnings(ci);

        processEvent(score, m, cricketBall, batsman, bowler, ctx);

        cricketBall.setInnings(ci);

        cricketBallInterface.save(cricketBall);
        matchStateInterface.save(m);
        playerInningsInterface.save(nonStriker);
        playerInningsInterface.save(batsman);
        playerInningsInterface.save(bowler);

        boolean shouldRotate = checkRotate(Integer.parseInt(score.getEvent()));
        ScoreDTO s = convertToScoreDTO(m, batsman, bowler, nonStriker, shouldRotate);
        statsService.updateTournamentStats(cricketBall.getId());
        return s;
    }

    private void processEvent(ScoreDTO score, MatchState m, CricketBall c,
                              PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
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
        }
        c.setEvent(score.getEvent());
        c.setEventType(score.getEventType());
    }

    private void handleWickets(ScoreDTO score, MatchState m, CricketBall c,
                               PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        String d = score.getDismissalType().toLowerCase();
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
        c.setDismissalType(score.getDismissalType());
        c.setOutPlayer(ctx.outPlayer);  // ✅ No DB call
        if (ctx.fielder != null)
            c.setFielder(ctx.fielder);  // ✅ No DB call
        c.setRuns(score.getRunsOnThisBall());
        c.setLegalDelivery(true);
        if (!score.getDismissalType().equals("retired") && !score.getDismissalType().equals("mankad"))
            incrementBall(m);
        if (score.getDismissalType().equalsIgnoreCase("runout"))
            batsman.setRuns(batsman.getRuns() + score.getRunsOnThisBall());
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
        cricketBallInterface.save(c);
    }

    private void handleExtras(ScoreDTO score, MatchState m, CricketBall c,
                              PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        int r = Integer.parseInt(score.getEvent());
        m.setRuns(m.getRuns() + r);
        checkRotate(r);

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
        } else {
            bowler.setRunsConceded(bowler.getRunsConceded() + r + 1);
            bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());

            if (score.getEventType().equalsIgnoreCase("wide")) {
                m.setExtras(m.getExtras() + r + 1);
                c.setExtraType("wide");
            } else {
                m.setExtras(m.getExtras() + 1);
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
        cricketBallInterface.save(c);
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