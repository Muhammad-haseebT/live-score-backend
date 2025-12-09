package com.livescore.backend.Controller;
import com.livescore.backend.DTO.MatchDTO;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
public class MatchController {
    @Autowired
    private MatchService matchService;
    @Autowired
    private MatchInterface matchInterface;

    @PostMapping("/match")
    public ResponseEntity<?> createMatch(@RequestBody MatchDTO matchDTO) {
        return matchService.createMatch(matchDTO);
    }

    @PutMapping("/match/{id}")
    public ResponseEntity<?> updateMatch(@PathVariable Long id, @RequestBody MatchDTO matchDTO) {
        return matchService.updateMatch(id, matchDTO);
    }

    @DeleteMapping("/match/{id}")
    public ResponseEntity<?> deleteMatch(@PathVariable Long id) {
        return matchService.deleteMatch(id);
    }

    @GetMapping("/match/{id}")
    public ResponseEntity<?> getMatch(@PathVariable Long id) {
        return matchService.getMatch(id);
    }

    @GetMapping("/match")
    public ResponseEntity<?> getAllMatches() {
        return matchService.getAllMatches();
    }

    @GetMapping("/match/tournament/{tournamentId}")
    public ResponseEntity<?> getMatchesByTournament(@PathVariable Long tournamentId) {
        return matchService.getMatchesByTournament(tournamentId);
    }

    @GetMapping("/match/team/{teamId}")
    public ResponseEntity<?> getMatchesByTeam(@PathVariable Long teamId) {
        return matchService.getMatchesByTeam(teamId);
    }

    @GetMapping("/match/status/{status}")
    public ResponseEntity<?> getMatchesByStatus(@PathVariable String status) {
        return matchService.getMatchesByStatus(status);
    }


    @GetMapping("/match/date/{date}")
    public ResponseEntity<?> getMatchesByDate(@PathVariable LocalDate date) {
        return matchService.getMatchesByDate(date);
    }

    @GetMapping("/match/time/{time}")
    public ResponseEntity<?> getMatchesByTime(@PathVariable LocalTime time) {
        return matchService.getMatchesByTime(time);
    }
    //start match (status changed to live ,scorerID assign ho ,tosswinner id,decision assign ho)
    @PutMapping("/match/start/{id}")
    public ResponseEntity<?> startMatch(@PathVariable Long id,@RequestBody MatchDTO match) {
        return matchService.startMatch(id,match);
    }
    //end match (status changed to finished ,winner team id assign ho)
    @PutMapping("/match/end/{id}")
    public ResponseEntity<?> endMatch(@PathVariable Long id) {
        return matchService.endMatch(id);
    }
    //abondan match
    @PutMapping("/match/abandon/{id}")
    public ResponseEntity<?> abandonMatch(@PathVariable Long id) {
        return matchService.abandonMatch(id);
    }

}
