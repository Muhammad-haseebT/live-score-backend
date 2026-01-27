package com.livescore.backend.Cricket;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Service.CacheEvictionService;
import com.livescore.backend.Service.StatsService;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CricketScoringService {
    @Autowired private CricketScoringValidator validator;
    @Autowired private CricketBallEventProcessor eventProcessor;
    @Autowired private CricketInningsManager inningsManager;
    @Autowired private CricketStateCalculator stateCalculator;
    @Autowired private CricketBallInterface ballRepo;
    @Autowired private StatsService statsService;
    @Autowired private CacheEvictionService cacheEvictionService;

    @CachePut(value = "matchState", key = "#s.matchId")
    @Transactional
    public ScoreDTO scoring(ScoreDTO s) {
        if(s.isUndo()){
            return undoLastBall(s.getMatchId(), s.getInningsId());
        }
        // 1. Validate
        CricketScoringValidator.ValidationResult validation = validator.validate(s);
        if (!validation.valid) {
            return createError(s, validation.error);
        }

        Match match = validation.match;
        CricketInnings innings = validation.innings;

        // 2. Check if innings completed
        if (inningsManager.isInningsComplete(innings, match)) {
            s.setStatus(Constants.STATUS_END);
            return inningsManager.handleInningsEnd(s, match, innings);
        }

        // 3. Create and process ball (using request data as-is)
        CricketBall ball = eventProcessor.createAndProcessBall(s, match, innings);

        // ✅ 4. INCREMENT BALL NUMBER (simple logic!)
        boolean isLegalBall = ball.getLegalDelivery();
        if (isLegalBall) {
            int nextBall = s.getBalls() + 1;
            if (nextBall >= Constants.BALLS_PER_OVER) {
                s.setOvers(s.getOvers() + 1);
                s.setBalls(0);
            } else {
                s.setBalls(nextBall);
            }
        }
        // If illegal (wide/no-ball), ball number remains same

        ballRepo.save(ball);

        // 5. Update stats (async)
        statsService.updateTournamentStats(ball);

        if (match.getTournament() != null) {
            cacheEvictionService.evictTournamentAwards(match.getTournament().getId());
        }

        // 6. Calculate current state (but preserve ball number)
        int currentOver = s.getOvers();
        int currentBall = s.getBalls();

        s = stateCalculator.calculateState(s, match, innings);

        // Restore ball numbers (don't let DB calculation overwrite)
        s.setOvers(currentOver);
        s.setBalls(currentBall);

        return s;
    }

    private ScoreDTO createError(ScoreDTO s, String msg) {
        s.setStatus(Constants.STATUS_ERROR);
        s.setComment(msg);
        return s;
    }

    @Cacheable(value = "matchState", key = "#matchId")
    public ScoreDTO getCurrentMatchState(Long matchId) {
        return stateCalculator.getCurrentState(matchId);
    }

    @CacheEvict(value = "matchState", key = "#matchId")
    @Transactional
    public ScoreDTO undoLastBall(Long matchId, Long inningsId) {
        List<CricketBall> balls = ballRepo.findByInnings_IdOrderByIdDesc(inningsId);
        if (balls != null && !balls.isEmpty()) {
            ballRepo.delete(balls.get(0));
        }
        return stateCalculator.getCurrentState(matchId);
    }
}
