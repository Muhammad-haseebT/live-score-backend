package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.SportsInterface;
import com.livescore.backend.Service.SportsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SportsController {
    @Autowired
    SportsService sportsService;
    @PostMapping("/sports")
    public ResponseEntity<Sports> addSports(@RequestBody Sports s){
        return sportsService.add(s);
    }
    @GetMapping("/sports")
    public ResponseEntity<List<Sports>> getAllSports(){
        return sportsService.getAll();
    }
    @DeleteMapping("/sports/{id}")
    public ResponseEntity<Sports> deleteSports(@PathVariable int id){
        return sportsService.delete(id);
    }
    @PutMapping("/sports/{id}")
    public ResponseEntity<Sports> updateSports(@PathVariable int id, @RequestBody Sports s){
        return sportsService.update(id, s);
    }
}
