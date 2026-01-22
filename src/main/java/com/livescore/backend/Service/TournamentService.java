package com.livescore.backend.Service;

import com.livescore.backend.DTO.TournamentRequestDTO;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TournamentService {
    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private SportsInterface sportsInterface;
    @Autowired
    private SeasonInterface seasonInterface;
    @Autowired
    private PtsTableInterface ptsTableInterface;

    public ResponseEntity<?> createTournament(TournamentRequestDTO tournament) {
        if (tournament == null) {
            return ResponseEntity.badRequest().body("Tournament details are required");
        }
        if (tournament.getName() == null || tournament.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Tournament name is required");
        }
        if (tournament.getSeasonId() == null) {
            return ResponseEntity.badRequest().body("Season id is required");
        }
        if (tournament.getSportsId() == null) {
            return ResponseEntity.badRequest().body("Sports id is required");
        }
        if (tournament.getUsername() == null || tournament.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (tournamentInterface.existsByNameAndSeasonId(tournament.getName(), tournament.getSeasonId())) {
            return ResponseEntity.badRequest().body("Tournament name already exists");
        }
        if (!accountInterface.existsActiveByUsername(tournament.getUsername())) {
            return ResponseEntity.badRequest().body("User not found");
        }
        if (!sportsInterface.existsById(tournament.getSportsId())) {
            return ResponseEntity.badRequest().body("Sports not found");
        }
        if (!seasonInterface.existsById(tournament.getSeasonId())) {
            return ResponseEntity.badRequest().body("Season not found");
        }
        Tournament tournament1=new Tournament();
        tournament1.setName(tournament.getName());
        //check role
        var organizer = accountInterface.findByUsername(tournament.getUsername());
        if (organizer == null || organizer.getRole() == null) {
            return ResponseEntity.badRequest().body("User role not found");
        }
        String role = organizer.getRole().trim().toUpperCase();
        if (!role.equals("ADMIN")) {
            return ResponseEntity.badRequest().body("Only admin can create a tournament");
        }


        tournament1.setOrganizer(organizer);
        Season season = seasonInterface.findById(tournament.getSeasonId()).orElse(null);
        if (season == null) {
            return ResponseEntity.badRequest().body("Season not found");
        }
        Sports sport = sportsInterface.findById(tournament.getSportsId()).orElse(null);
        if (sport == null) {
            return ResponseEntity.badRequest().body("Sport not found");
        }
        tournament1.setSeason(season);
        tournament1.setSport(sport);
        tournament1.setStartDate(tournament.getStartDate());
        tournament1.setEndDate(tournament.getEndDate());
        tournament1.setPlayerType(tournament.getPlayerType());
        tournament1.setTournamentType(tournament.getTournamentType());
        tournament1.setTournamentStage(tournament.getTournamentStage());

       tournamentInterface.save(tournament1);

        if (season.getSportsOffered() == null) {
            season.setSportsOffered(new ArrayList<>());
        }

        if (!season.getSportsOffered().contains(sport)) {
            season.getSportsOffered().add(sport);
            seasonInterface.save(season);
        }



        return ResponseEntity.ok().build();

    }
    public ResponseEntity<?> getTournamentById(Long id) {
        return tournamentInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    public ResponseEntity<?> getAllTournaments() {
        return ResponseEntity.ok(tournamentInterface.findAll());
    }
    public ResponseEntity<?> updateTournament(Long id, TournamentRequestDTO tournament) {
        if (tournament == null) {
            return ResponseEntity.badRequest().body("Tournament details are required");
        }
        var opt = tournamentInterface.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Tournament tournament1 = opt.get();

        if (tournament.getName() != null && !tournament.getName().isBlank()) {
            tournament1.setName(tournament.getName());
        }
        if (tournament.getUsername() != null && !tournament.getUsername().isBlank()) {
            var organizer = accountInterface.findByUsername(tournament.getUsername());
            if (organizer == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            tournament1.setOrganizer(organizer);
        }
        if (tournament.getSeasonId() != null) {
            Season season = seasonInterface.findById(tournament.getSeasonId()).orElse(null);
            if (season == null) {
                return ResponseEntity.badRequest().body("Season not found");
            }
            tournament1.setSeason(season);
        }
        if (tournament.getSportsId() != null) {
            Sports sport = sportsInterface.findById(tournament.getSportsId()).orElse(null);
            if (sport == null) {
                return ResponseEntity.badRequest().body("Sport not found");
            }
            tournament1.setSport(sport);
        }
        if (tournament.getStartDate() != null) {
            tournament1.setStartDate(tournament.getStartDate());
        }
        if (tournament.getEndDate() != null) {
            tournament1.setEndDate(tournament.getEndDate());
        }
        if (tournament.getPlayerType() != null) {
            tournament1.setPlayerType(tournament.getPlayerType());
        }
        if (tournament.getTournamentType() != null) {
            tournament1.setTournamentType(tournament.getTournamentType());
        }
        if (tournament.getTournamentStage() != null) {
            tournament1.setTournamentStage(tournament.getTournamentStage());
        }

        return ResponseEntity.ok(tournamentInterface.save(tournament1));
    }
    public ResponseEntity<?> deleteTournament(Long id) {
        if(tournamentInterface.existsById(id)){
            tournamentInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }


    public ResponseEntity<?> getOverview(Long id) {
        Tournament tournament = tournamentInterface.findById(id).orElse(null);
        if(tournament == null){
            return ResponseEntity.notFound().build();
        }
        OverViewDTO overViewDTO = new OverViewDTO();
        if (tournament.getTeams() == null) {
            overViewDTO.setTeams(0);
        } else {
            overViewDTO.setTeams((int) tournament.getTeams().stream().filter(team -> team != null && "APPROVED".equals(team.getStatus())).count());
        }
        overViewDTO.setPlayerType(tournament.getPlayerType());
        overViewDTO.setStartDate(tournament.getStartDate());
        Pageable p=PageRequest.of(0, 3);
        List<Abc> a= ptsTableInterface.findByTournamentId(id,p);
        overViewDTO.setTop(a == null ? List.of() : a);
        return ResponseEntity.ok(overViewDTO);

    }

    public ResponseEntity<?> getTournamentByName() {
        List<Map<Long,String>> t =new ArrayList<>();
        List<Tournament>at=tournamentInterface.findAllNames();
        for (Tournament tournament : at) {
            Map<Long,String> mp=new HashMap<>();
            mp.put(tournament.getId(),tournament.getName());
            t.add(mp);
        }


        return ResponseEntity.ok(t);
    }
}

@Data
class OverViewDTO {
    private int teams;
    private String playerType;
    private LocalDate startDate;
    List<Abc> top;
}
