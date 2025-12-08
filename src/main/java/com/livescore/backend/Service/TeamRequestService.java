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
        Account player=playerInterface.findById(teamRequest.getPlayerId()).orElse(null);
        if(team==null||player==null){
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
        if(teamRequest.getStatus().equalsIgnoreCase("approve")){
         teamInterface.findById(teamRequest.getTeamId()).orElse(null).setStatus("approved");

        }
        teamRequest1.setTeam(teamInterface.findById(teamRequest.getTeamId()).orElse(null));
        teamRequest1.setPlayerAccount(playerInterface.findById(teamRequest.getPlayerId()).orElse(null));
        teamRequest1.setStatus(teamRequest.getStatus());
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
        teamRequest.setStatus("rejected");
        teamRequestInterface.save(teamRequest);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> approveTeamRequest(Long id) {
        TeamRequest teamRequest=teamRequestInterface.findById(id).orElse(null);
        if(teamRequest==null){
            return ResponseEntity.notFound().build();
        }
        teamRequest.setStatus("approved");
        teamInterface.findById(teamRequest.getTeam().getId()).orElse(null).setStatus("approved");
        ptsTableService.createPtsTable(new PtsTable(teamRequest.getTeam(),teamRequest.getTeam().getTournament()));

        teamRequestInterface.save(teamRequest);
        return ResponseEntity.ok().build();
    }
}
