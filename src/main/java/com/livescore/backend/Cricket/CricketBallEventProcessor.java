package com.livescore.backend.Cricket;


import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Interface.MediaInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CricketBallEventProcessor {
    @Autowired
    private PlayerInterface playerRepo;
    @Autowired private MediaInterface mediaRepo;

    public CricketBall createAndProcessBall(ScoreDTO s, Match m, CricketInnings innings) {
        CricketBall ball = createBallEntity(s, m, innings);
        processBallEvent(s, ball);
        return ball;
    }

    private CricketBall createBallEntity(ScoreDTO s, Match m, CricketInnings innings) {
        CricketBall ball = new CricketBall();
        ball.setInnings(innings);
        ball.setBatsman(getPlayer(s.getBatsmanId()));
        ball.setBowler(getPlayer(s.getBowlerId()));
        ball.setFielder(getPlayer(s.getFielderId()));
        ball.setMatch(m);
        ball.setOverNumber(s.getOvers());
        ball.setBallNumber(s.getBalls());
        ball.setComment(s.getComment());
        ball.setMedia(s.getMediaId() != null ? mediaRepo.findById(s.getMediaId()).orElse(null) : null);

        // Initialize defaults
        ball.setRuns(0);
        ball.setExtra(0);
        ball.setExtraType(null);
        ball.setLegalDelivery(Boolean.FALSE);
        ball.setIsFour(Boolean.FALSE);
        ball.setIsSix(Boolean.FALSE);

        return ball;
    }

    public void processBallEvent(ScoreDTO s, CricketBall ball) {
        String eventType = s.getEventType().trim().toLowerCase().replace("_", "").replace("-", "");

        switch (eventType) {
            case "run": processRun(s, ball); break;
            case "boundary": case "boundry": processBoundary(s, ball); break;
            case "wide": processWide(s, ball); break;
            case "noball": case "nb": processNoBall(s, ball); break;
            case "bye": processBye(s, ball); break;
            case "legbye": processLegBye(s, ball); break;
            case "wicket": processWicket(s, ball); break;
            default: throw new IllegalArgumentException("Invalid event type: " + s.getEventType());
        }
    }

    private void processRun(ScoreDTO s, CricketBall ball) {
        ball.setRuns(parseIntSafe(s.getEvent()));
        ball.setLegalDelivery(true);
    }

    private void processBoundary(ScoreDTO s, CricketBall ball) {
        int boundary = parseIntSafe(s.getEvent());
        ball.setRuns(boundary);
        ball.setLegalDelivery(true);
        ball.setIsFour(boundary == 4);
        ball.setIsSix(boundary == 6);
    }

    private void processWide(ScoreDTO s, CricketBall ball) {
        int extras = s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : 1;
        ball.setExtra(extras);
        ball.setExtraType(Constants.EXTRA_WIDE);
        ball.setLegalDelivery(false);
    }

    private void processNoBall(ScoreDTO s, CricketBall ball) {
        ball.setRuns(s.getRunsOnThisBall());
        ball.setExtra(s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : 1);
        ball.setExtraType(Constants.EXTRA_NO_BALL);
        ball.setLegalDelivery(false);
    }

    private void processBye(ScoreDTO s, CricketBall ball) {
        ball.setExtra(s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : parseIntSafe(s.getEvent()));
        ball.setExtraType(Constants.EXTRA_BYE);
        ball.setLegalDelivery(true);
    }

    private void processLegBye(ScoreDTO s, CricketBall ball) {
        ball.setExtra(s.getExtrasThisBall() > 0 ? s.getExtrasThisBall() : parseIntSafe(s.getEvent()));
        ball.setExtraType(Constants.EXTRA_LEG_BYE);
        ball.setLegalDelivery(true);
    }

    private void processWicket(ScoreDTO s, CricketBall ball) {
        ball.setDismissalType(s.getDismissalType() != null ? s.getDismissalType() : s.getEvent());
        ball.setOutPlayer(getPlayer(s.getOutPlayerId()) != null ? getPlayer(s.getOutPlayerId()) : ball.getBatsman());
        ball.setRuns(s.getRunsOnThisBall());
        ball.setExtra(s.getExtrasThisBall());
        if (s.getExtraType() != null) ball.setExtraType(s.getExtraType());
        ball.setLegalDelivery(s.getIsLegal() != null ? s.getIsLegal() : Boolean.TRUE);

        // Set fielder for caught/runout/stumped
        String dismissal = ball.getDismissalType();
        if (dismissal != null && (dismissal.equalsIgnoreCase("caught") ||
                dismissal.equalsIgnoreCase("runout") || dismissal.equalsIgnoreCase("stumped"))) {
            ball.setFielder(getPlayer(s.getFielderId()));
        }
    }

    private Player getPlayer(Long id) {
        return id != null ? playerRepo.findActiveById(id).orElse(null) : null;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
