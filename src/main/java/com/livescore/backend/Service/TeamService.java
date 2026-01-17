package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDto;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.TeamRequest;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.PlayerInterface;
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
    private TournamentInterface tournamentInterface;
    public ResponseEntity<?> createTeam(Team team,Long tournamentId,Long playerId) {
        if(team.getName().isEmpty()||team.getName().isBlank()){
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
        team.setTournament(tournamentOpt.get());
        Team savedTeam = teamInterface.save(team);

       Player p1=playerInterface.findById(playerId).get();
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
        return teamInterface.findById(id).map(teamEntity -> {
            teamEntity.setName(team.getName());
            teamEntity.setStatus(team.getStatus());
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
        List<Team> teams = teamInterface.findByTournamentId(tid);

        for (Team team : teams) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", team.getId());
            m.put("name", team.getName());
            response.add(m);
        }

        return ResponseEntity.ok(response);

    }
}
