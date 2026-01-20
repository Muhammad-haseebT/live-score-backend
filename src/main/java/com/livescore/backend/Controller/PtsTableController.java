package com.livescore.backend.Controller;

import com.livescore.backend.Entity.PtsTable;
import com.livescore.backend.Service.PtsTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PtsTableController {
    @Autowired
    private PtsTableService ptsTableService;
    @PostMapping("/ptsTable")
    public ResponseEntity<?> createPtsTable(@RequestBody PtsTable ptsTable) {
        return ptsTableService.createPtsTable(ptsTable);
    }
    @PutMapping("/ptsTable/{id}")
    public ResponseEntity<?> updatePtsTable(@PathVariable Long id) {
        return ptsTableService.updatePointsTableAfterMatch(id);
    }
    @DeleteMapping("/ptsTable/{id}")
    public ResponseEntity<?> deletePtsTable(@PathVariable Long id) {
        return ptsTableService.deletePtsTable(id);
    }
    @GetMapping("/ptsTable")
    public ResponseEntity<?> getAllPtsTables() {
        return ptsTableService.getAllPtsTables();
    }
    @GetMapping("/ptsTable/{id}")
    public ResponseEntity<?> getPtsTableById(@PathVariable Long id) {
        return ptsTableService.getPtsTableById(id);
    }
    @GetMapping("/ptsTable/tournament/{tournamentId}")
    public ResponseEntity<?> getPtsTablesByTournament(@PathVariable Long tournamentId) {
        return ptsTableService.getPtsTablesByTournament(tournamentId);
    }
}
