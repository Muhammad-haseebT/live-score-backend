package com.livescore.backend.Service;

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

import java.util.*;

@Service
public class TeamService {
    @Autowired
    private TeamInterface teamInterface;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private PlayerRequestInterface pri;

    @Autowired
    private TournamentInterface tournamentInterface;
    public ResponseEntity<?> createTeam(Team team,Long tournamentId,Long playerId) {
        if (team == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team details are required")
            );
        }
        if (tournamentId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Tournament id is required")
            );
        }
        if (playerId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player id is required")
            );
        }
        if (team.getName() == null || team.getName().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team name is required")
            );
        }
        Optional<Tournament> tournamentOpt = tournamentInterface.findById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Tournament not found with ID: " + tournamentId)
            );
        }
        Optional<Player> playerOpt = playerInterface.findActiveById(playerId);
        if (playerOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player not found with ID: " + playerId)
            );
        }
        Player p1 = playerOpt.get();
        team.setTournament(tournamentOpt.get());
        team.setCreator(p1);
        Team savedTeam = teamInterface.save(team);
       p1.setPlayerRole("CAPTAIN");
       playerInterface.save(p1);

        return ResponseEntity.ok(Map.of(
                "message", "Team created successfully",
                "teamId", savedTeam.getId(),
                "name", savedTeam.getName(),
                "tournamentId", savedTeam.getTournament().getId()
        ));


    }
    public ResponseEntity<?> updateTeam(Long id, Team team) {
        if (team == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team details are required")
            );
        }
        return teamInterface.findById(id).map(teamEntity -> {
            if (team.getName() != null && !team.getName().isBlank()) {
                teamEntity.setName(team.getName());
            }
            if (team.getStatus() != null && !team.getStatus().isBlank()) {
                teamEntity.setStatus(team.getStatus());
            }
            return ResponseEntity.ok(teamInterface.save(teamEntity));
        }).orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> deleteTeam(Long id) {
        if(teamInterface.existsById(id)){
            teamInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    public ResponseEntity<?> getAllTeams() {
        return ResponseEntity.ok(teamInterface.findAll());
    }
    public ResponseEntity<?> getTeamById(Long id) {
        return teamInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    public ResponseEntity<?> getTeamsByStatus(String status) {
        return ResponseEntity.ok(teamInterface.findByStatus(status));
    }
    public ResponseEntity<?> getTeamsByTournamentIdAndStatus(Long tournamentId, String status) {
        return ResponseEntity.ok(teamInterface.findByTournamentIdAndStatus(tournamentId, status));
    }



    public ResponseEntity<?> getTeamByTournamentId(Long tid) {
        List<Map<String, Object>> response = new ArrayList<>();
        if (tid == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Tournament id is required")
            );
        }
        List<Team> teams = teamInterface.findByTournamentId(tid);
        if (teams == null || teams.isEmpty()) {
            return ResponseEntity.ok(response);
        }

        for (Team team : teams) {
            if (team == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", team.getId());
            m.put("name", team.getName());
            m.put("status",team.getStatus());
            response.add(m);
        }

        return ResponseEntity.ok(response);

    }

    public ResponseEntity<?> getTeamByTournamentIdAndAccountId(Long tid, Long aid) {
        if (tid == null || aid == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Tournament id and account id are required")
            );
        }

        // Get player ID
        Long creatorPlayerId = playerInterface.findByAccount_Id(aid)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(Player::getId)
                .orElse(null);

        if (creatorPlayerId == null) {
            return ResponseEntity.notFound().build();
        }

        // Get team
        Optional<Team> teamOpt = teamInterface.findByTournamentIdAndPlayerId(tid, creatorPlayerId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Team team = teamOpt.get();

        // OPTIMIZED: Single query with JOIN FETCH
        List<PlayerRequest> players = pri
                .findByTeamIdWithPlayer(team.getId());

        List<Map<String, Object>> playersLite = players.stream()
                .filter(p -> p != null && p.getPlayer() != null)
                .filter(p -> !Boolean.TRUE.equals(p.getPlayer().getIsDeleted()))
                .map(p -> {
                    Map<String, Object> playerMap = new HashMap<>();
                    playerMap.put("id", p.getId());
                    playerMap.put("name", p.getPlayer().getName());
                    playerMap.put("status", p.getStatus());
                    return playerMap;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("teamName", team.getName());
        response.put("teamId", team.getId());
        response.put("teamStatus", team.getStatus());
        response.put("players", playersLite);

        return ResponseEntity.ok(response);
    }

}
