package com.livescore.backend.Service;

import com.livescore.backend.DTO.TeamRequestDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        Account player=playerInterface.findActiveById(teamRequest.getPlayerId()).orElse(null);
        if(player==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team or Player not found")
            );
        }
        if(teamRequestInterface.existsByTeamAndPlayerAccount(team,player)){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team request already exists for this team and player")
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

    public ResponseEntity<?> getTeamRequestsByTournamentId() {
        List<TeamRequest> requests = teamRequestInterface.findAllByStatus("PENDING");

        List<HashMap<String, Object>> response = requests.stream().map(r -> {
            HashMap<String, Object> m = new HashMap<>();

            Team team = r.getTeam();

            m.put("requestId", r.getId());
            m.put("teamName", team != null ? team.getName() : null);

            String tournamentName = null;
            if (team != null && team.getTournament() != null) {
                // Tournament entity me field "name" assume hai
                tournamentName = team.getTournament().getName();
            }
            m.put("tournamentName", tournamentName);

            m.put("CaptainName", r.getTeam().getCreator().getName());
            //player names and usernames
            List<Map<String, String>> players = new ArrayList<>();
            if (team != null && team.getPlayers() != null) {
                for (Player p : team.getPlayers()) {
                    if (p.getIsDeleted() != null && p.getIsDeleted()) continue;
                    Map<String, String> playerInfo = new HashMap<>();
                    playerInfo.put("name", p.getName());
                    if (p.getAccount() != null) {
                        playerInfo.put("username", p.getAccount().getUsername());
                    } else {
                        playerInfo.put("username", null);
                    }
                    players.add(playerInfo);
                }
            }
            m.put("players", players);
            m.put("status", r.getStatus());

            return m;
        }).toList();

        return ResponseEntity.ok(response);
    }
}
