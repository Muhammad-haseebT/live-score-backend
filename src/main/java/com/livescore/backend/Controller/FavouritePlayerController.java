package com.livescore.backend.Controller;

import com.livescore.backend.Entity.FavouritePlayer;
import com.livescore.backend.Service.FavouritePlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favourite-player")
@RequiredArgsConstructor
public class FavouritePlayerController {

    private final FavouritePlayerService fpService;

    /**
     * POST /api/favourite-player/vote
     * Body: { "matchId": 1, "accountId": 5, "playerId": 12 }
     */
    @PostMapping("/vote")
    public ResponseEntity<?> vote(@RequestBody Map<String, Long> body) {
        try {
            Long matchId   = body.get("matchId");
            Long accountId = body.get("accountId");
            Long playerId  = body.get("playerId");

            if (matchId == null || accountId == null || playerId == null) {
                return ResponseEntity.badRequest().body("matchId, accountId, playerId required");
            }

            String result = fpService.submitVote(matchId, accountId, playerId);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            // "Aap pehle hi vote kar chuke hain!" — 409 Conflict
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    /**
     * GET /api/favourite-player/results/{matchId}
     * Returns FavouritePlayer with playerVoteCounts map
     */
    @GetMapping("/results/{matchId}")
    public ResponseEntity<?> results(@PathVariable Long matchId) {
        try {
            FavouritePlayer fp = fpService.getResults(matchId);
            return ResponseEntity.ok(fp);
        } catch (RuntimeException e) {
            // No votes yet — return empty
            return ResponseEntity.ok(Map.of(
                    "playerVoteCounts", Map.of(),
                    "votedAccountIds",  new java.util.HashSet<>()
            ));
        }
    }

    /**
     * GET /api/favourite-player/check?matchId=1&accountId=5
     * Returns true/false whether this account already voted
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> check(
            @RequestParam Long matchId,
            @RequestParam Long accountId) {
        return ResponseEntity.ok(fpService.checkVote(matchId, accountId));
    }
}