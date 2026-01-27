package com.livescore.backend.Cricket;

import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.CricketInningsInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CricketScoringValidator {
    @Autowired
    private MatchInterface matchRepo;
    @Autowired private CricketInningsInterface inningsRepo;
    @Autowired private CricketBallInterface ballRepo;

    public ValidationResult validate(ScoreDTO s) {
        if (s == null || s.getMatchId() == null || s.getInningsId() == null) {
            return ValidationResult.error("Invalid input data");
        }

        Match match = matchRepo.findById(s.getMatchId()).orElse(null);
        if (match == null) return ValidationResult.error(Constants.ERROR_MATCH_NOT_FOUND);
        if (isMatchFinal(match)) return ValidationResult.error(Constants.ERROR_MATCH_ALREADY_ENDED);

        CricketInnings innings = inningsRepo.findById(s.getInningsId()).orElse(null);
        if (innings == null) return ValidationResult.error(Constants.ERROR_INNINGS_NOT_FOUND);
        if (!innings.getMatch().getId().equals(match.getId())) {
            return ValidationResult.error("Innings does not belong to match");
        }

        if (s.getEventType() == null || s.getEventType().isBlank()) {
            return ValidationResult.error("eventType required");
        }

        // Check duplicate ball
        if (isDuplicateBall(s, match.getId(), innings.getNo())) {
            return ValidationResult.error(Constants.ERROR_DUPLICATE_BALL);
        }

        return ValidationResult.success(match, innings);
    }

    private boolean isDuplicateBall(ScoreDTO s, Long matchId, int inningsNo) {
        List<CricketBall> existing = ballRepo.findByOverNumberAndBallNumberAndMatch_Id(
                s.getOvers(), s.getBalls(), matchId, inningsNo);
        return existing != null && !existing.isEmpty();
    }

    private boolean isMatchFinal(Match m) {
        if (m == null || m.getStatus() == null) return false;
        String st = m.getStatus().trim().toUpperCase();
        return st.equals("COMPLETED") || st.equals("FINISHED") || st.equals("ABANDONED") || st.equals("TIED");
    }

    // Validation result wrapper
    public static class ValidationResult {
        public boolean valid;
        public String error;
        public Match match;
        public CricketInnings innings;

        public static ValidationResult error(String msg) {
            ValidationResult r = new ValidationResult();
            r.valid = false;
            r.error = msg;
            return r;
        }

        public static ValidationResult success(Match m, CricketInnings i) {
            ValidationResult r = new ValidationResult();
            r.valid = true;
            r.match = m;
            r.innings = i;
            return r;
        }
    }
}