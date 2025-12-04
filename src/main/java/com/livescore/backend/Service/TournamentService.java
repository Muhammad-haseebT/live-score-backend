package com.livescore.backend.Service;

import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.SportsInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TournamentService {

    @Autowired
    TournamentInterface ti;

    @Autowired
    SportsInterface sportsInterface;  // Correct injection

    public ResponseEntity<Tournament> add(Tournament t) {
        if (t.getSports() == null || t.getSports().getSportsid() == 0) {
            System.out.println(t.getSports().getSportsid());
            return ResponseEntity.badRequest().build();
        }

        Sports sports = sportsInterface.findById(t.getSports().getSportsid())
                .orElse(null);
        if (sports == null) {
            System.out.println("2");
            return ResponseEntity.notFound().build();
        }



        t.setSports(sports);

        Tournament saved = ti.save(t);
        return ResponseEntity.ok(saved);
    }

    public ResponseEntity<List<Tournament>> getAll() {
        List<Tournament> tournaments = ti.findAll();
        return ResponseEntity.ok(tournaments);
    }

    public ResponseEntity<Tournament> delete(int id) {
        ti.deleteById(id);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Tournament> update(int id, Tournament t) {
        Tournament tournament = ti.findById(id).orElse(null);
        if (tournament == null) {
            return ResponseEntity.notFound().build();
        }
        tournament.setName(t.getName());
        tournament.setSports(t.getSports());
        Tournament saved = ti.save(tournament);
        return ResponseEntity.ok(saved);
    }
}
