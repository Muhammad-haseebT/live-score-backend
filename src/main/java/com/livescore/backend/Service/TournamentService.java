package com.livescore.backend.Service;

import com.livescore.backend.DTO.TournamentRequestDTO;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.SeasonInterface;
import com.livescore.backend.Interface.SportsInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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

    public ResponseEntity<?> createTournament(TournamentRequestDTO tournament) {
        if(tournament.getName()==null||tournament.getName().isEmpty()){
            return ResponseEntity.badRequest().body("Tournament name is required");
        }
        if(tournamentInterface.existsByNameAndSeasonId(tournament.getName(),tournament.getSeasonId())){
            return ResponseEntity.badRequest().body("Tournament name already exists");
        }
        if(!accountInterface.existsByUsername(tournament.getUsername())){
            return ResponseEntity.badRequest().body("User not found");
        }
        if(!sportsInterface.existsById(tournament.getSportsId())){
            return ResponseEntity.badRequest().body("Sports not found");
        }
        if(!seasonInterface.existsById(tournament.getSeasonId())){
            return ResponseEntity.badRequest().body("Season not found");
        }
        Tournament tournament1=new Tournament();
        tournament1.setName(tournament.getName());
        //check role
        String role = accountInterface.findByUsername(tournament.getUsername()).getRole().trim().toUpperCase();
        if (!role.equals("ADMIN")) {
            return ResponseEntity.badRequest().body("Only admin can create a tournament");
        }
        tournament1.setOrganizer(accountInterface.findByUsername(tournament.getUsername()));
        tournament1.setSeason(seasonInterface.findById(tournament.getSeasonId()).get());
        tournament1.setSport(sportsInterface.findById(tournament.getSportsId()).get());
        tournament1.setStartDate(tournament.getStartDate());
        tournament1.setEndDate(tournament.getEndDate());
        tournament1.setPlayerType(tournament.getPlayerType());
        tournament1.setTournamentType(tournament.getTournamentType());
        tournament1.setTournamentStage(tournament.getTournamentStage());
        return ResponseEntity.ok(tournamentInterface.save(tournament1));

    }
    public ResponseEntity<?> getTournamentById(Long id) {
        if(tournamentInterface.existsById(id)){
            return ResponseEntity.ok(tournamentInterface.findById(id).get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }
    public ResponseEntity<?> getAllTournaments() {
        return ResponseEntity.ok(tournamentInterface.findAll());
    }
    public ResponseEntity<?> updateTournament(Long id, TournamentRequestDTO tournament) {
        if(tournamentInterface.existsById(id)){
            Tournament tournament1=tournamentInterface.findById(id).get();
            tournament1.setName(tournament.getName());
            tournament1.setOrganizer(accountInterface.findByUsername(tournament.getUsername()));
            tournament1.setSeason(seasonInterface.findById(tournament.getSeasonId()).get());
            tournament1.setSport(sportsInterface.findById(tournament.getSportsId()).get());
            tournament1.setStartDate(tournament.getStartDate());
            tournament1.setEndDate(tournament.getEndDate());
            tournament1.setPlayerType(tournament.getPlayerType());
            tournament1.setTournamentType(tournament.getTournamentType());
            tournament1.setTournamentStage(tournament.getTournamentStage());
            return ResponseEntity.ok(tournamentInterface.save(tournament1));
        }else{
            return ResponseEntity.notFound().build();
        }
    }
    public ResponseEntity<?> deleteTournament(Long id) {
        if(tournamentInterface.existsById(id)){
            tournamentInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }


}
