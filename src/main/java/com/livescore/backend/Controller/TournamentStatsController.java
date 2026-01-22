package com.livescore.backend.Controller;

import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.Service.AwardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournament")
public class TournamentStatsController {

    @Autowired
    private AwardService awardService;

    @GetMapping("/{tournamentId}/stats")
    public ResponseEntity<TournamentAwardsDTO> getTournamentStats(@PathVariable Long tournamentId) {
        TournamentAwardsDTO dto = awardService.ensureAndGetTournamentAwards(tournamentId);
        return ResponseEntity.ok(dto);
    }
}
