package com.livescore.backend.Service;

import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.SportsInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SportsService {
    @Autowired
    private SportsInterface sportsInterface;

    public ResponseEntity<?> createSports(Sports sports) {
        if(sports.getName()==null||sports.getName().isEmpty()){
            return ResponseEntity.badRequest().body("Sports name is required");
        }
        if(sportsInterface.existsByName(sports.getName())){
            return ResponseEntity.badRequest().body("Sports name already exists");
        }
        return ResponseEntity.ok(sportsInterface.save(sports));
    }

    public ResponseEntity<?> getSportsById(Long id) {
        if(sportsInterface.findById(id).isPresent()){
            return ResponseEntity.ok(sportsInterface.findById(id).get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> getAllSports() {
        return ResponseEntity.ok(sportsInterface.findAll());
    }

    public ResponseEntity<?> updateSports(Long id, Sports sports) {
        if(sportsInterface.findById(id).isPresent()){
            Sports sports1=sportsInterface.findById(id).get();
            sports1.setName(sports.getName());
            return ResponseEntity.ok(sportsInterface.save(sports1));
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> deleteSports(Long id) {
        if(sportsInterface.findById(id).isPresent()){
            sportsInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }
}
