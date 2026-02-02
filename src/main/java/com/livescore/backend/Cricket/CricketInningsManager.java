package com.livescore.backend.Cricket;
import com.livescore.backend.DTO.ScoreDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.CricketInningsInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Service.AwardService;
import com.livescore.backend.Service.CacheEvictionService;
import com.livescore.backend.Service.MatchService;
import com.livescore.backend.Util.Constants;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class CricketInningsManager {
    @Autowired private CricketInningsInterface inningsRepo;
    @Autowired private CricketBallInterface ballRepo;
    @Autowired private MatchInterface matchRepo;
    @Autowired private TeamInterface teamRepo;
    @Autowired private AwardService awardService;
    @Autowired private MatchService matchService;

    /**
     * Checks if innings is complete (overs finished or all out)
     */
    public boolean isInningsComplete(CricketInnings innings, Match match) {
        int maxBalls = match.getOvers() * Constants.BALLS_PER_OVER;
        long legalBalls = ballRepo.countLegalBallsByInningsId(innings.getId());
        return maxBalls > 0 && legalBalls >= maxBalls;
    }

    /**
     * Handles innings end - creates second innings or ends match
     */
    public ScoreDTO handleInningsEnd(ScoreDTO s, Match match, CricketInnings currentInnings) {
        if (s.isFirstInnings()) {
            return createSecondInnings(s, match, currentInnings);
        } else {
            return endMatch(s, match, currentInnings);
        }
    }

    /**
     * Creates second innings after first innings completes
     */
    private ScoreDTO createSecondInnings(ScoreDTO s, Match match, CricketInnings firstInnings) {
        // Check if second innings already exists (idempotency)
        CricketInnings existingSecond = inningsRepo.findByMatchIdAndNo(match.getId(), Constants.SECOND_INNINGS);
        if (existingSecond != null) {
            s.setInningsId(existingSecond.getId());
            s.setStatus(Constants.STATUS_END_FIRST);
            s.setFirstInnings(false);
            s.setTarget(getInningsRuns(firstInnings) + 1);
            return s;
        }

        // Create new second innings
        CricketInnings secondInnings = new CricketInnings();
        secondInnings.setMatch(match);
        secondInnings.setNo(Constants.SECOND_INNINGS);

        // Set chasing team (opposite of first innings team)
        Team chasingTeam = getOpposingTeam(match, firstInnings.getTeam());
        if (chasingTeam == null) {
            s.setStatus(Constants.STATUS_ERROR);
            s.setComment("Cannot determine chasing team");
            return s;
        }

        secondInnings.setTeam(chasingTeam);
        inningsRepo.save(secondInnings);

        // Reset DTO for second innings
        int target = getInningsRuns(firstInnings) + 1;
        s.setInningsId(secondInnings.getId());
        s.setStatus(Constants.STATUS_END_FIRST);
        s.setFirstInnings(false);
        s.setRuns(0);
        s.setOvers(0);
        s.setWickets(0);
        s.setBalls(0);
        s.setTarget(target);
        s.setCrr(0.0);
        s.setRrr(0.0);
        CricketStateCalculator stateCalculator=new CricketStateCalculator();
        stateCalculator.clearInningsBatsmenCache(firstInnings.getId());
        return s;
    }

    /**
     * Ends match and determines winner
     */
    private ScoreDTO endMatch(ScoreDTO s, Match match, CricketInnings secondInnings) {
        if (isMatchFinal(match)) {
            s.setStatus(Constants.STATUS_END_MATCH);
            return s;
        }

        CricketInnings firstInnings = inningsRepo.findByMatchIdAndNo(match.getId(), Constants.FIRST_INNINGS);
        int firstRuns = getInningsRuns(firstInnings);
        int secondRuns = getInningsRuns(secondInnings);

        // Determine winner
        if (secondRuns > firstRuns) {
            match.setWinnerTeam(secondInnings.getTeam());
        } else if (secondRuns < firstRuns) {
            match.setWinnerTeam(getOpposingTeam(match, secondInnings.getTeam()));
        } else {
            match.setWinnerTeam(null);
            match.setStatus(Constants.STATUS_TIED);
        }

        // Save match and compute awards
        if (!Constants.STATUS_TIED.equalsIgnoreCase(match.getStatus())) {
            match.setStatus(Constants.STATUS_COMPLETED);
        }
        matchRepo.save(match);
        awardService.computeMatchAwards(match.getId());
        matchService.endMatch(match.getId());

        s.setStatus(Constants.STATUS_END_MATCH);
        s.setTarget(Math.max(0, (firstRuns + 1) - secondRuns));
        return s;
    }

    /**
     * Checks if innings should end (all out scenario)
     */
    public boolean shouldEndInnings(ScoreDTO s, CricketInnings innings) {
        Team team = innings.getTeam();
        int teamSize = getTeamSize(team);
        int maxWickets = teamSize - 1;

        return s.getWickets() >= maxWickets;
    }

    // Helper methods
    private int getInningsRuns(CricketInnings innings) {
        if (innings == null) return 0;
        return ballRepo.sumRunsAndExtrasByInningsId(innings.getId());
    }

    private Team getOpposingTeam(Match match, Team currentTeam) {
        if (currentTeam == null || match.getTeam1() == null || match.getTeam2() == null) {
            return null;
        }
        return match.getTeam1().getId().equals(currentTeam.getId()) ?
                match.getTeam2() : match.getTeam1();
    }

    private int getTeamSize(Team team) {
        if (team == null) return Constants.DEFAULT_TEAM_SIZE;
        try {
            Team t = teamRepo.findById(team.getId()).orElse(null);
            if (t != null && t.getPlayers() != null) {
                return t.getPlayers().size();
            }
        } catch (Exception ignored) {}
        return Constants.DEFAULT_TEAM_SIZE;
    }

    private boolean isMatchFinal(Match m) {
        if (m == null || m.getStatus() == null) return false;
        String st = m.getStatus().trim().toUpperCase();
        return st.equals("COMPLETED") || st.equals("FINISHED") ||
                st.equals("ABANDONED") || st.equals("TIED");
    }
}