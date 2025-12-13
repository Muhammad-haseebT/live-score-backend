package com.livescore.backend.Service;

import com.livescore.backend.DTO.MatchDTO;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {
    @Autowired
    private MatchInterface matchInterface;
    @Autowired
    private TeamInterface teamInterface;
    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private SportsInterface sportsInterface;
    @Autowired
    private PtsTableService ptsTableService;
    @Autowired
    private StatsService statsService;
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;
    @Autowired
    private CricketInningsInterface cricketInningsRepo;


    public ResponseEntity<?> createMatch(MatchDTO matchDTO) {
        if (accountInterface.getById(matchDTO.getScorerId()) == null) {
            return ResponseEntity.badRequest().body("Account with id " + matchDTO.getScorerId() + " does not exist");
        }
        if (teamInterface.getById(matchDTO.getTeam1Id()) == null) {
            return ResponseEntity.badRequest().body("Team with id " + matchDTO.getTeam1Id() + " does not exist");
        }
        if (teamInterface.getById(matchDTO.getTeam2Id()) == null) {
            return ResponseEntity.badRequest().body("Team with id " + matchDTO.getTeam2Id() + " does not exist");
        }
        if (tournamentInterface.getById(matchDTO.getTournamentId()) == null) {
            return ResponseEntity.badRequest().body("Tournament with id " + matchDTO.getTournamentId() + " does not exist");
        }
        if (matchDTO.getDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Match date " + matchDTO.getDate() + " does not belong to the future");
        }
        if (matchDTO.getTime().isBefore(LocalTime.now())) {
            return ResponseEntity.badRequest().body("Match time " + matchDTO.getTime() + " does not belong to the future");
        }
        Match match = new Match();
        match.setTournament(tournamentInterface.getById(matchDTO.getTournamentId()));
        match.setTeam1(teamInterface.getById(matchDTO.getTeam1Id()));
        match.setTeam2(teamInterface.getById(matchDTO.getTeam2Id()));
        match.setScorer(accountInterface.getById(matchDTO.getScorerId()));
        match.setVenue(matchDTO.getVenue());
        match.setDate(matchDTO.getDate());
        match.setTime(matchDTO.getTime());
        match.setOvers(matchDTO.getOvers());

        match.setSets(matchDTO.getSets());

        matchInterface.save(match);

        return ResponseEntity.ok().body("Match created successfully");

    }
    public ResponseEntity<?> updateMatch(Long id, MatchDTO matchDTO) {
        Match match = matchInterface.getById(id);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }
        if (accountInterface.getById(matchDTO.getScorerId()) == null) {
            return ResponseEntity.badRequest().body("Account with id " + matchDTO.getScorerId() + " does not exist");
        }
        if (teamInterface.getById(matchDTO.getTeam1Id()) == null) {
            return ResponseEntity.badRequest().body("Team with id " + matchDTO.getTeam1Id() + " does not exist");
        }
        if (teamInterface.getById(matchDTO.getTeam2Id()) == null) {
            return ResponseEntity.badRequest().body("Team with id " + matchDTO.getTeam2Id() + " does not exist");
        }
        if (tournamentInterface.getById(matchDTO.getTournamentId()) == null) {
            return ResponseEntity.badRequest().body("Tournament with id " + matchDTO.getTournamentId() + " does not exist");
        }
        if (matchDTO.getDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Match date " + matchDTO.getDate() + " does not belong to the future");
        }
        if (matchDTO.getTime().isBefore(LocalTime.now())) {
            return ResponseEntity.badRequest().body("Match time " + matchDTO.getTime() + " does not belong to the future");
        }
        match.setTournament(tournamentInterface.getById(matchDTO.getTournamentId()));
        match.setTeam1(teamInterface.getById(matchDTO.getTeam1Id()));
        match.setTeam2(teamInterface.getById(matchDTO.getTeam2Id()));
        match.setScorer(accountInterface.getById(matchDTO.getScorerId()));
        match.setVenue(matchDTO.getVenue());
        match.setDate(matchDTO.getDate());
        match.setTime(matchDTO.getTime());
        match.setOvers(matchDTO.getOvers());
        match.setSets(matchDTO.getSets());
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match updated successfully");
    }
    public ResponseEntity<?> deleteMatch(Long id) {
        Match match = matchInterface.getById(id);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }
        matchInterface.delete(match);
        return ResponseEntity.ok().body("Match deleted successfully");
    }
    public ResponseEntity<?> getMatch(Long id) {

        Match match = matchInterface.findById(id).orElse(null);

        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }

        return ResponseEntity.ok(match);
    }


    public ResponseEntity<?> getAllMatches() {

        List<Match> matches=matchInterface.findAll();
        List<MatchDTO> matchDTOs=new ArrayList<>();
        for(Match match:matches){
            MatchDTO matchDTO=new MatchDTO();
            matchDTO.setId(match.getId());
            matchDTO.setTournamentId(match.getTournament().getId());
            matchDTO.setTournamentName(match.getTournament().getName());
            matchDTO.setTeam1Id(match.getTeam1().getId());
            matchDTO.setTeam1Name(match.getTeam1().getName());
            matchDTO.setTeam2Id(match.getTeam2().getId());
            matchDTO.setTeam2Name(match.getTeam2().getName());
            matchDTO.setScorerId(match.getScorer().getId());
            matchDTO.setStatus(match.getStatus());
            matchDTO.setVenue(match.getVenue());
            matchDTO.setDate(match.getDate());
            matchDTO.setTime(match.getTime());
            matchDTO.setDecision(match.getDecision());
            matchDTO.setOvers(match.getOvers());
            matchDTO.setSets(match.getSets());
            matchDTO.setSportId(match.getTournament().getSport().getId());
            if (match.getTossWinner() != null) {
                matchDTO.setTossWinnerId(match.getTossWinner().getId());
            } else {
                matchDTO.setTossWinnerId(null);
            }

            matchDTOs.add(matchDTO);
        }

        return ResponseEntity.ok().body(matchDTOs);
    }




    public ResponseEntity<?> getMatchesByTournament(Long tournamentId) {
        return ResponseEntity.ok().body(matchInterface.findByTournamentId(tournamentId));
    }
    public ResponseEntity<?> getMatchesByTeam(Long teamId) {
        return ResponseEntity.ok().body(matchInterface.findByTeam1IdOrTeam2Id(teamId, teamId));
    }


    public ResponseEntity<?> getMatchesByDate(LocalDate date) {
        return ResponseEntity.ok().body(matchInterface.findByDate(date));
    }
    public ResponseEntity<?> getMatchesByTime(LocalTime time) {
        return ResponseEntity.ok().body(matchInterface.findByTime(time));
    }

    public ResponseEntity<?> startMatch(Long id,MatchDTO m) {
        System.out.println(m.getScorerId());
        Match match = matchInterface.findById(id).orElse(null);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }
        match.setStatus("live");
        match.setScorer(accountInterface.findById(m.getScorerId()).orElse(null));
        match.setTossWinner(teamInterface.getById(m.getTossWinnerId()));
        match.setDecision(m.getDecision());
        Sports s= sportsInterface.getById(m.getSportId());
        if(s.getName().equalsIgnoreCase("cricket")){
            match.setOvers(m.getOvers());
        } else if (s.getName().equalsIgnoreCase("volleyball")||s.getName().equalsIgnoreCase("Badminton")
                ||s.getName().equalsIgnoreCase("Tabletennis")) {
            match.setSets(m.getSets());
        }
        CricketInnings innings = new CricketInnings();
        innings.setMatch(match);
        innings.setNo(1);
        if(match.getDecision().equalsIgnoreCase("bat")){
            innings.setTeam(teamInterface.findById(m.getTossWinnerId()).orElse(null));
        }else{
            innings.setTeam(teamInterface.findById(m.getTeam1Id()==m.getTossWinnerId()?m.getTeam2Id():m.getTeam1Id()).orElse(null));
        }
        cricketInningsRepo.save(innings);

        match.setDecision(m.getDecision());
        match.setTossWinner(teamInterface.findById(m.getTossWinnerId()).orElse(null));
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match started successfully");
    }


    public ResponseEntity<?> endMatch(Long matchId) {
        // 1. Get match
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + matchId + " does not exist");
        }

        // 2. Mark match as finished
        match.setStatus("FINISHED");
        matchInterface.save(match);

        // 3. Update points table for teams
        ptsTableService.updatePointsTableAfterMatch(matchId);

        // 4. Get all players from match

        List<Player> playersTeam1 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam1().getId());
        List<Player> playersTeam2 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam2().getId());

        if(playersTeam1.isEmpty()||playersTeam2.isEmpty()){
            return ResponseEntity.badRequest().body("No players found for team 1 or team 2");
        }
        // 5. Update stats for all players
        for (Player p : playersTeam1) {
            statsService.updateStats(p.getId(), match.getTournament().getId(),matchId);
        }
        for (Player p : playersTeam2) {
            statsService.updateStats(p.getId(), match.getTournament().getId(),matchId);
        }

        return ResponseEntity.ok("Match ended successfully and stats updated");
    }

    public ResponseEntity<?> abandonMatch(Long id) {
        Match match = matchInterface.getById(id);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }
        match.setStatus("abandoned");
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match abandoned successfully");
    }



    public ResponseEntity<?> getMatchesByStatus(String status) {
        List<Match> matches = matchInterface.findByStatus(status); // only matches with given status
        List<MatchDTO> matchDTOs = new ArrayList<>();
        for (Match match : matches) {
            MatchDTO matchDTO = new MatchDTO();
            matchDTO.setId(match.getId());
            matchDTO.setTournamentId(match.getTournament().getId());
            matchDTO.setTournamentName(match.getTournament().getName());
            matchDTO.setTeam1Id(match.getTeam1().getId());
            matchDTO.setTeam1Name(match.getTeam1().getName());
            matchDTO.setTeam2Id(match.getTeam2().getId());
            matchDTO.setTeam2Name(match.getTeam2().getName());
            matchDTO.setStatus(match.getStatus());
            matchDTO.setVenue(match.getVenue());
            matchDTO.setDate(match.getDate());
            matchDTO.setTime(match.getTime());
            matchDTOs.add(matchDTO);
        }
        return ResponseEntity.ok(matchDTOs);
    }

}
