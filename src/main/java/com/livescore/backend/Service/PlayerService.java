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
        if(player.getName().isEmpty()||player.getName().isBlank()){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player name is required")
            );
        }
        if(player.getPlayerRole().isBlank()||player.getPlayerRole().isEmpty()){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player role is required")
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
        p1.setAccount(accountInterface.findByUsername(player.getUsername()));

        Player savedPlayer = playerInterface.save(p1);
        return ResponseEntity.ok(Map.of(
                "message", "Player created successfully",
                "playerId", savedPlayer.getId(),
                "name", savedPlayer.getName()
        ));
    }
    public ResponseEntity<?> updatePlayer(Long id, PlayerDto player) {
        return playerInterface.findById(id).map(playerEntity -> {
            playerEntity.setName(player.getName());
            playerEntity.setPlayerRole(player.getPlayerRole());
            return ResponseEntity.ok(playerInterface.save(playerEntity));
        }).orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> deletePlayer(Long id) {
        if(playerInterface.existsById(id)){
            playerInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    public ResponseEntity<?> getAllPlayers() {

        List<Player> players = playerInterface.findAll();
        List<PlayerDto> result = new ArrayList<>();

        for (Player i : players) {

            PlayerDto p2 = new PlayerDto();
            p2.setId(i.getId());
            p2.setName(i.getName());
            p2.setPlayerRole(i.getPlayerRole());


            p2.setPlayerRequests(new ArrayList<>());

            List<PlayerRequest> requests =
                    playerRequestInterface.findbyPlayer_Id(i.getId());

            for (PlayerRequest j : requests) {

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

            result.add(p2);
        }

        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> getPlayerById(Long id) {
        return playerInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
