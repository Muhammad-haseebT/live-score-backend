package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDto;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.PlayerInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PlayerService {
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private AccountInterface accountInterface;
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
        //same username player aya to add na ho
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
        return ResponseEntity.ok(playerInterface.findAll());
    }
    public ResponseEntity<?> getPlayerById(Long id) {
        return playerInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
