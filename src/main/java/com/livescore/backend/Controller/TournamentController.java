package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TournamentController {
    @Autowired
    TournamentService ts;
    @PostMapping("/tournament")
    public ResponseEntity<Tournament> addTournament(@RequestBody Tournament t){
        return ts.add(t);
    }
    @GetMapping("/tournament")
    public ResponseEntity<List<Tournament>> getAllTournament(){
        return ts.getAll();
    }
    @DeleteMapping("/tournament/{id}")
    public ResponseEntity<Tournament> deleteTournament(@PathVariable int id){
        return ts.delete(id);
    }
    @PutMapping("/tournament/{id}")
    public ResponseEntity<Tournament> updateTournament(@PathVariable int id, @RequestBody Tournament t){
        return ts.update(id, t);
    }
}
