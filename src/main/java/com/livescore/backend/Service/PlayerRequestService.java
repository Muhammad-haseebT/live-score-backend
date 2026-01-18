package com.livescore.backend.Service;


import com.livescore.backend.DTO.PlayerRequestDTO;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlayerRequestService {
    @Autowired
    PlayerRequestInterface playerRequestInterface;
    @Autowired
    PlayerInterface playerInterface;
    @Autowired
    TeamInterface teamInterface;
    @Autowired
    TournamentInterface tournamentInterface;
    @Autowired
    StatsService statsService;
    public ResponseEntity<?> createPlayerRequest(PlayerRequestDTO playerRequest) {
        if(playerRequest.getPlayerId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player id is required")
            );
        }
        if(playerRequest.getTeamId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team id is required")
            );
        }
        if(playerRequest.getTournamentId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Tournament id is required")
            );
        }
        Player player=playerInterface.findActiveById(playerRequest.getPlayerId()).orElse(null);
        Team team=teamInterface.findById(playerRequest.getTeamId()).orElse(null);
        Tournament tournament=tournamentInterface.findById(playerRequest.getTournamentId()).orElse(null);
        if(player==null||team==null||tournament==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player, Team or Tournament not found")
            );
        }

        PlayerRequest existing = playerRequestInterface.findExistingRequest(playerRequest.getPlayerId(), playerRequest.getTournamentId());

        if (existing != null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player already belongs to a team in this tournament")
            );
        }

        PlayerRequest playerRequest1=new PlayerRequest();
        playerRequest1.setPlayer(player);
        playerRequest1.setTeam(team);
        playerRequest1.setTournament(tournament);
        playerRequestInterface.save(playerRequest1);
        return ResponseEntity.ok().build();



    }

    public ResponseEntity<?> updatePlayerRequest(Long id, PlayerRequestDTO playerRequest) {
        System.out.println(id);
        PlayerRequest playerRequest1=playerRequestInterface.findById(id).orElse(null);
        if(playerRequest1==null){
            return ResponseEntity.notFound().build();
        }
        playerRequest1.setPlayer(playerInterface.findActiveById(playerRequest.getPlayerId()).orElse(null));
        playerRequest1.setTeam(teamInterface.findById(playerRequest.getTeamId()).orElse(null));
        playerRequest1.setTournament(tournamentInterface.findById(playerRequest.getTournamentId()).orElse(null));
        playerRequest1.setStatus(playerRequest.getStatus());
        playerRequestInterface.save(playerRequest1);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> deletePlayerRequest(Long id) {
        if(playerRequestInterface.existsById(id)){
            playerRequestInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    public ResponseEntity<?> getAllPlayerRequests() {
        return ResponseEntity.ok(playerRequestInterface.findAll());
    }
    public ResponseEntity<?> getPlayerRequestById(Long id) {
        return playerRequestInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> approvePlayerRequest(Long id) {
        PlayerRequest playerRequest=playerRequestInterface.findById(id).orElse(null);
        if(playerRequest==null){
            return ResponseEntity.notFound().build();
        }
        if (playerRequest.getPlayer() == null || Boolean.TRUE.equals(playerRequest.getPlayer().getIsDeleted())) {
            return ResponseEntity.badRequest().body("Player not found");
        }
        if(playerRequest.getStatus().equals("APPROVED")){
            return ResponseEntity.badRequest().body("Player already approved");
        }
        PlayerRequest existing = playerRequestInterface.findExistingRequest(playerRequest.getPlayer().getId(), playerRequest.getTournament().getId());

        if (existing != null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player already belongs to a team in this tournament")
            );
        }

        playerRequest.setStatus("APPROVED");
        playerRequestInterface.save(playerRequest);
        Player player=playerRequest.getPlayer();
        player.setTeam(playerRequest.getTeam());
        playerInterface.save(player);

        statsService.createStats(playerRequest.getPlayer().getId(),playerRequest.getTournament().getId());
        return ResponseEntity.ok().build();
    }

}
