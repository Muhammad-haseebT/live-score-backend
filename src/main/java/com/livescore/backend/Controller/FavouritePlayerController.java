package com.livescore.backend.Controller;

import com.livescore.backend.Entity.FavouritePlayer;
import com.livescore.backend.Interface.AwardInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.TournamentInterface;
import com.livescore.backend.Service.FavouritePlayerService;
import com.livescore.backend.Service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favourite-player")
@RequiredArgsConstructor
public class FavouritePlayerController {

    private final FavouritePlayerService fpService;
    private final MatchService matchService;
    private final TournamentInterface tournamentInterface;
    private final PlayerInterface playerInterface;
    private final AwardInterface awardInterface;

    /**
     * POST /api/favourite-player/vote
     * Body: { "matchId": 1, "accountId": 5, "playerId": 12 }
     */
    @PostMapping("/vote")
    public ResponseEntity<?> vote(@RequestBody Map<String, Object> body) {
        try {
            Long matchId   = body.get("matchId") != null ? ((Number) body.get("matchId")).longValue() : null;
            Long accountId = body.get("accountId") != null ? ((Number) body.get("accountId")).longValue() : null;
            Long playerId  = body.get("playerId") != null ? ((Number) body.get("playerId")).longValue() : null;
            String feedback = body.get("feedback") != null ? body.get("feedback").toString() : null;

            if (matchId == null || accountId == null || playerId == null) {
                return ResponseEntity.badRequest().body("matchId, accountId, playerId required");
            }

            // ✅ 4th argument pass karo
            String result = fpService.submitVote(matchId, accountId, playerId, feedback);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
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



    @GetMapping("/top-voted/{tournamentId}")
    public ResponseEntity<?> topVoted(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(fpService.getTopVotedPlayersForTournament(tournamentId));
    }

    /**
     * POST /api/favourite-player/set-mot/{tournamentId}/{playerId}
     * Set Man of Tournament
     */
    @PostMapping("/set-mot/{tournamentId}/{playerId}")
    public ResponseEntity<?> setManOfTournament(
            @PathVariable Long tournamentId,
            @PathVariable Long playerId) {
        try {
            // existing MAN_OF_TOURNAMENT award update ya create karo
            var tournament = tournamentInterface.findById(tournamentId)
                    .orElseThrow(() -> new RuntimeException("Tournament not found"));
            var player = playerInterface.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found"));

            // existing award dhundo, nahi mila to naya banao
            com.livescore.backend.Entity.Award award = awardInterface
                    .findByTournamentId(tournamentId).stream()
                    .filter(a -> "MAN_OF_TOURNAMENT".equals(a.getAwardType()))
                    .findFirst()
                    .orElseGet(() -> {
                        com.livescore.backend.Entity.Award a = new com.livescore.backend.Entity.Award();
                        a.setTournament(tournament);
                        a.setAwardType("MAN_OF_TOURNAMENT");
                        return a;
                    });

            award.setPlayer(player);
            award.setReason("Man of the Tournament");
            awardInterface.save(award);

            return ResponseEntity.ok("Man of Tournament set: " + player.getName());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}