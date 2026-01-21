package com.livescore.backend.Controller;

import com.livescore.backend.DTO.PlayerStatsDTO;
import com.livescore.backend.Service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/player")
public class PlayerStatsController {
    @Autowired
    private StatsService statsService;

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<?> getPlayerStats(
            @PathVariable Long playerId,
            @RequestParam Long tournamentId,
            @RequestParam(required=false) Long matchId
    ) {
        PlayerStatsDTO dto = statsService.getPlayerTournamentStats(playerId, tournamentId, matchId);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/{playerId}/Cricket" )
    public ResponseEntity<?> getPlayerCricketStats(
            @PathVariable Long playerId,
            @RequestParam Long tournamentId,
            @RequestParam(required=false) Long matchId
    ) {
        PlayerStatsDTO dto = statsService.getPlayerCricketTournamentStats(playerId, tournamentId, matchId);
        return ResponseEntity.ok(dto);
    }

}
