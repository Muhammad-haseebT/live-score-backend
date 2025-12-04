package com.livescore.backend.Service;

import com.livescore.backend.Entity.Season;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.SeasonInterface;
import com.livescore.backend.Interface.SportsInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SportsService {

    @Autowired
    SportsInterface si;

    @Autowired
    SeasonInterface seasonInterface;

    public ResponseEntity<Sports> add(Sports s) {

        if (s.getSeason() == null || s.getSeason().getSid() == 0) {
            return ResponseEntity.badRequest().build();
        }
        Season season = seasonInterface.findById(s.getSeason().getSid())
                .orElse(null);
        if (season == null) {
            return ResponseEntity.notFound().build();
        }


        s.setSeason(season);

        Sports saved = si.save(s);
        return ResponseEntity.ok(saved);
    }

    public ResponseEntity<List<Sports>> getAll() {
        List<Sports> sports = si.findAll();
        return ResponseEntity.ok(sports);
    }

    public ResponseEntity<Sports> delete(int id) {
        si.deleteById(id);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Sports> update(int id, Sports s) {
        Sports sports = si.findById(id).orElse(null);
        if (sports == null) {
            return ResponseEntity.notFound().build();
        }
        sports.setName(s.getName());
        sports.setSeason(s.getSeason());
        Sports saved = si.save(sports);
        return ResponseEntity.ok(saved);
    }
}
