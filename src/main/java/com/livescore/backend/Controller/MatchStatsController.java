package com.livescore.backend.Controller;

import com.livescore.backend.DTO.AwardsDTO;
import com.livescore.backend.DTO.MatchScorecardDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.Service.AwardService;
import com.livescore.backend.Service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/match")
public class MatchStatsController {
    @Autowired
    private StatsService statsService;
    @Autowired private AwardService awardService;

    @GetMapping("/{matchId}/scorecard")
    public ResponseEntity<MatchScorecardDTO> getScorecard(@PathVariable Long matchId) {
        return statsService.getMatchScorecard(matchId);
    }

    @GetMapping("/{matchId}/awards")
    public ResponseEntity<AwardsDTO> getMatchAwards(@PathVariable Long matchId) {
        AwardsDTO dto = awardService.ensureAndGetAwards(matchId);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/tournaments/{tournamentId}/awards")
    public ResponseEntity<TournamentAwardsDTO> getTournamentAwards(@PathVariable Long tournamentId) {
        TournamentAwardsDTO dto = awardService.ensureAndGetTournamentAwards(tournamentId);
        return ResponseEntity.ok(dto);
    }

}
