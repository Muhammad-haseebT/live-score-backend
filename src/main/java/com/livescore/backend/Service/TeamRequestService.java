package com.livescore.backend.Service;

import com.livescore.backend.DTO.TeamRequestDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TeamRequestService {
    @Autowired
    TeamRequestInterface teamRequestInterface;
    @Autowired
    TeamInterface teamInterface;
    @Autowired
    AccountInterface playerInterface;

    @Autowired
    PtsTableService ptsTableService;

    private String toUpper(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        return t.toUpperCase();
    }

    public ResponseEntity<?> createTeamRequest(TeamRequestDTO teamRequest) {
        if(teamRequest.getTeamId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team id is required")
            );
        }
        if(teamRequest.getPlayerId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player id is required")
            );
        }

        Team team=teamInterface.findById(teamRequest.getTeamId()).orElse(null);
        if(team == null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team not found")
            );
        }
        if(team.getPlayers() == null || team.getPlayers().size()<11){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "11 players required")
            );
        }
        Account player=playerInterface.findActiveById(teamRequest.getPlayerId()).orElse(null);
        if(player==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team or Player not found")
            );
        }
        TeamRequest teamRequest1=new TeamRequest();
        teamRequest1.setTeam(team);
        teamRequest1.setPlayerAccount(player);
        teamRequestInterface.save(teamRequest1);
        return ResponseEntity.ok().build();
    }
    public ResponseEntity<?> updateTeamRequest(Long id, TeamRequestDTO teamRequest) {
        TeamRequest teamRequest1=teamRequestInterface.findById(id).orElse(null);
        if(teamRequest1==null){
            return ResponseEntity.notFound().build();
        }
        String status = toUpper(teamRequest == null ? null : teamRequest.getStatus());
        if("APPROVE".equals(status) || "APPROVED".equals(status)){
            Team t = teamInterface.findById(teamRequest.getTeamId()).orElse(null);
            if (t != null) {
                t.setStatus("APPROVED");
                teamInterface.save(t);
            }

        }
        teamRequest1.setTeam(teamInterface.findById(teamRequest.getTeamId()).orElse(null));
        teamRequest1.setPlayerAccount(playerInterface.findActiveById(teamRequest.getPlayerId()).orElse(null));
        if (status != null) {
            teamRequest1.setStatus(status);
        }
        teamRequestInterface.save(teamRequest1);
        return ResponseEntity.ok().build();
    }
    public ResponseEntity<?> deleteTeamRequest(Long id) {
        if(teamRequestInterface.existsById(id)){
            teamRequestInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    public ResponseEntity<?> getAllTeamRequests() {
        return ResponseEntity.ok(teamRequestInterface.findAll());
    }
    public ResponseEntity<?> getTeamRequestById(Long id) {
        return teamRequestInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> rejectTeamRequest(Long id) {
        TeamRequest teamRequest=teamRequestInterface.findById(id).orElse(null);
        if(teamRequest==null){
            return ResponseEntity.notFound().build();
        }
        teamRequest.setStatus("REJECTED");
        teamRequestInterface.save(teamRequest);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> approveTeamRequest(Long id) {
        TeamRequest teamRequest=teamRequestInterface.findById(id).orElse(null);
        if(teamRequest==null){
            return ResponseEntity.notFound().build();
        }
        teamRequest.setStatus("APPROVED");
        Team t = teamInterface.findById(teamRequest.getTeam().getId()).orElse(null);
        if (t != null) {
            t.setStatus("APPROVED");
            teamInterface.save(t);
        }
        ptsTableService.createPtsTable(new PtsTable(teamRequest.getTeam(),teamRequest.getTeam().getTournament()));

        teamRequestInterface.save(teamRequest);
        return ResponseEntity.ok().build();
    }
}
