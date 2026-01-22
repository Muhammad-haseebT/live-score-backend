package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDto;
import com.livescore.backend.DTO.ShowRequestDto;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlayerService {
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;
    public ResponseEntity<?> createPlayer(PlayerDto player) {
        if (player == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player details are required")
            );
        }
        if(player.getName() == null || player.getName().isBlank()){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player name is required")
            );
        }
        if(player.getPlayerRole() == null || player.getPlayerRole().isBlank()){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player role is required")
            );
        }
        if (player.getUsername() == null || player.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Username is required")
            );
        }
        Player p1=new Player();
        p1.setName(player.getName());
        p1.setPlayerRole(player.getPlayerRole());

        if(playerInterface.existsByAccount_Username(player.getUsername())){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player with this username already exists")
            );
        }
        var account = accountInterface.findByUsername(player.getUsername());
        if (account == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Account not found")
            );
        }
        p1.setAccount(account);

        Player savedPlayer = playerInterface.save(p1);
        return ResponseEntity.ok(Map.of(
                "message", "Player created successfully",
                "playerId", savedPlayer.getId(),
                "name", savedPlayer.getName()
        ));
    }
    public ResponseEntity<?> updatePlayer(Long id, PlayerDto player) {
        if (id == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player id is required")
            );
        }
        if (player == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player details are required")
            );
        }
        return playerInterface.findActiveById(id).map(playerEntity -> {
            if (player.getName() != null && !player.getName().isBlank()) {
                playerEntity.setName(player.getName());
            }
            if (player.getPlayerRole() != null && !player.getPlayerRole().isBlank()) {
                playerEntity.setPlayerRole(player.getPlayerRole());
            }
            return ResponseEntity.ok(playerInterface.save(playerEntity));
        }).orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> deletePlayer(Long id) {
        return playerInterface.findActiveById(id)
                .map(p -> {
                    p.softDelete();
                    playerInterface.save(p);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> getAllPlayers() {

        // Use optimized repository method to avoid N+1 queries
        List<Player> players = playerInterface.findAllWithRequestsAndAccounts();
        List<PlayerDto> result = new ArrayList<>();

        for (Player i : players) {

            PlayerDto p2 = new PlayerDto();
            p2.setId(i.getId());
            p2.setName(i.getName());
            p2.setPlayerRole(i.getPlayerRole());

            p2.setPlayerRequests(new ArrayList<>());

            // playerRequests should already be fetched because of the JOIN FETCH in the query
            if (i.getPlayerRequests() != null) {
                for (PlayerRequest j : i.getPlayerRequests()) {

                    if (j == null) continue;

                    ShowRequestDto pr1 = new ShowRequestDto();
                    pr1.setRequestId(j.getId());
                    pr1.setStatus(j.getStatus());

                    if (j.getTeam() != null) {
                        pr1.setTeamId(j.getTeam().getId());
                        pr1.setTeamName(j.getTeam().getName());
                    }

                    if (j.getTournament() != null) {
                        pr1.setTournamentId(j.getTournament().getId());
                    }

                    p2.getPlayerRequests().add(pr1);
                }
            }

            result.add(p2);
        }

        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> getPlayerById(Long id) {
        return playerInterface.findActiveById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> restorePlayer(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player id is required")
            );
        }
        Player p = playerInterface.findByIdIncludingDeleted(id).orElse(null);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        p.restore();
        playerInterface.save(p);
        return ResponseEntity.ok().build();
    }




}
