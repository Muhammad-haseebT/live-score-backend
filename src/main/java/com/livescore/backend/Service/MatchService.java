package com.livescore.backend.Service;

import com.livescore.backend.DTO.MatchDTO;
import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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


    // -------------------- Helpers --------------------
    private ResponseEntity<String> validateMatchDTO(MatchDTO matchDTO) {
        if (matchDTO == null || matchDTO.getScorerId() == null) {
            return ResponseEntity.badRequest().body("scorerId is required");
        }
        if (accountInterface.findActiveByUsername(matchDTO.getScorerId()).isEmpty()) {
            return ResponseEntity.badRequest().body("Account with id " + matchDTO.getScorerId() + " does not exist");
        }
        if (matchDTO.getTeam1Id() == null || teamInterface.findById(matchDTO.getTeam1Id()).isEmpty()) {
            return ResponseEntity.badRequest().body("Team with id " + matchDTO.getTeam1Id() + " does not exist");
        }
        if (matchDTO.getTeam2Id() == null || teamInterface.findById(matchDTO.getTeam2Id()).isEmpty()) {
            return ResponseEntity.badRequest().body("Team with id " + matchDTO.getTeam2Id() + " does not exist");
        }
        if (matchDTO.getTournamentId() == null || tournamentInterface.findById(matchDTO.getTournamentId()).isEmpty()) {
            return ResponseEntity.badRequest().body("Tournament with id " + matchDTO.getTournamentId() + " does not exist");
        }
        if (matchDTO.getDate() == null) {
            return ResponseEntity.badRequest().body("date is required");
        }
        if (matchDTO.getTime() == null) {
            return ResponseEntity.badRequest().body("time is required");
        }
        LocalDate today = LocalDate.now();
        if (matchDTO.getDate().isBefore(today)) {
            return ResponseEntity.badRequest().body("Match date " + matchDTO.getDate() + " does not belong to the future");
        }
        if (matchDTO.getDate().isEqual(today) && matchDTO.getTime().isBefore(LocalTime.now())) {
            return ResponseEntity.badRequest().body("Match time " + matchDTO.getTime() + " does not belong to the future");
        }
        return null;
    }

    private void applyMatchDTO(Match match, MatchDTO matchDTO) {
        match.setTournament(tournamentInterface.findById(matchDTO.getTournamentId()).orElse(null));
        match.setTeam1(teamInterface.findById(matchDTO.getTeam1Id()).orElse(null));
        match.setTeam2(teamInterface.findById(matchDTO.getTeam2Id()).orElse(null));
        match.setScorer(accountInterface.findActiveByUsername(matchDTO.getScorerId()).orElse(null));
        match.setVenue(matchDTO.getVenue());
        match.setDate(matchDTO.getDate());
        match.setTime(matchDTO.getTime());
        match.setOvers(matchDTO.getOvers());
        match.setSets(matchDTO.getSets());
    }

    // -------------------- Public API --------------------


    @CacheEvict(value = {"matches", "matchById"}, allEntries = true)
    public ResponseEntity<?> createMatch(MatchDTO matchDTO) {
        ResponseEntity<String> validation = validateMatchDTO(matchDTO);
        if (validation != null) return validation;
        Match match = new Match();
        applyMatchDTO(match, matchDTO);
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match created successfully");
    }

    @CacheEvict(value = {"matches", "matchById"}, allEntries = true)
    public ResponseEntity<?> updateMatch(Long id, MatchDTO matchDTO) {
        Match match = matchInterface.findById(id).orElse(null);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<String> validation = validateMatchDTO(matchDTO);
        if (validation != null) return validation;
        applyMatchDTO(match, matchDTO);
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match updated successfully");
    }
    @CacheEvict(value = {"matches", "matchById"}, allEntries = true)
    public ResponseEntity<?> deleteMatch(Long id) {
        Match match = matchInterface.findById(id).orElse(null);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        matchInterface.delete(match);
        return ResponseEntity.ok().body("Match deleted successfully");
    }

    public ResponseEntity<?> getMatch(Long id) {
        Match match = matchInterface.findById(id).orElse(null);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertToDTO(match));
    }


    public ResponseEntity<?> getAllMatches() {

        List<Match> matches = matchInterface.findAll();
        return ResponseEntity.ok().body(convertToDTO(matches));
    }


    @Cacheable(value = "matchById",key = "#tournamentId")
    public ResponseEntity<?> getMatchesByTournament(Long tournamentId) {
        List<Match> matches = matchInterface.findByTournamentId(tournamentId);
        if (matches.size() == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(convertToDTO(matches));
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
    @CacheEvict(value = {"matches", "matchById"}, allEntries = true)
    public ResponseEntity<?> startMatch(Long id, MatchDTO m) {
        if (m == null) {
            return ResponseEntity.badRequest().body("Match details are required");
        }
        Match match = matchInterface.findById(id).orElse(null);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        match.setStatus("LIVE");
        if (m.getScorerId() == null || accountInterface.findActiveByUsername(m.getScorerId()).isEmpty()) {
            return ResponseEntity.badRequest().body("Scorer account not found");
        }
        match.setScorer(accountInterface.findActiveByUsername(m.getScorerId()).orElse(null));

        if (m.getTossWinnerId() == null || teamInterface.findById(m.getTossWinnerId()).isEmpty()) {
            return ResponseEntity.badRequest().body("Toss winner team not found");
        }
        match.setTossWinner(teamInterface.findById(m.getTossWinnerId()).orElse(null));
        match.setDecision(m.getDecision());
        if (m.getSportId() == null || sportsInterface.findById(m.getSportId()).isEmpty()) {
            return ResponseEntity.badRequest().body("Sport not found");
        }
        Sports s = sportsInterface.findById(m.getSportId()).orElse(null);
        if (s.getName().equalsIgnoreCase("cricket")) {
            match.setOvers(m.getOvers());
        } else if (s.getName().equalsIgnoreCase("volleyball") || s.getName().equalsIgnoreCase("Badminton")
                || s.getName().equalsIgnoreCase("Tabletennis")) {
            match.setSets(m.getSets());
        }
        CricketInnings innings = new CricketInnings();
        innings.setMatch(match);
        innings.setNo(1);
        if (match.getDecision().equalsIgnoreCase("bat")) {
            innings.setTeam(teamInterface.findById(m.getTossWinnerId()).orElse(null));
        } else {
            if (m.getTeam1Id() == null || m.getTeam2Id() == null) {
                return ResponseEntity.badRequest().body("team1Id and team2Id are required");
            }
            Long battingTeamId = m.getTeam1Id().equals(m.getTossWinnerId()) ? m.getTeam2Id() : m.getTeam1Id();
            innings.setTeam(teamInterface.findById(battingTeamId).orElse(null));
        }
        cricketInningsRepo.save(innings);

        match.setDecision(m.getDecision());
        match.setTossWinner(teamInterface.findById(m.getTossWinnerId()).orElse(null));
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match started successfully");
    }
    @CacheEvict(value = {"matches", "matchById"}, allEntries = true)
    @Transactional
    public ResponseEntity<?> endMatch(Long matchId) {
        // 1. Get match
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + matchId + " does not exist");
        }

        if (match.getStatus() != null && (match.getStatus().equalsIgnoreCase("FINISHED") || match.getStatus().equalsIgnoreCase("COMPLETED"))) {
            return ResponseEntity.ok("Match already ended");
        }

        if (match.getTeam1() == null || match.getTeam2() == null || match.getTournament() == null) {
            return ResponseEntity.badRequest().body("Match is missing tournament or teams");
        }

        List<Player> playersTeam1 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam1().getId());
        List<Player> playersTeam2 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam2().getId());

        if (playersTeam1 == null || playersTeam2 == null || playersTeam1.isEmpty() || playersTeam2.isEmpty()) {
            return ResponseEntity.badRequest().body("No players found for team 1 or team 2");
        }

        // 2. Mark match as finished
        match.setStatus("COMPLETED");
        matchInterface.save(match);


        ptsTableService.updatePointsTableAfterMatch(matchId);


        // 5. Update stats for all players
        for (Player p : playersTeam1) {
            statsService.updateStats(p.getId(), match.getTournament().getId(), matchId);
        }
        for (Player p : playersTeam2) {
            statsService.updateStats(p.getId(), match.getTournament().getId(), matchId);
        }

        return ResponseEntity.ok("Match ended successfully and stats updated");
    }
    @CacheEvict(value = {"matches", "matchById"}, allEntries = true)
    public ResponseEntity<?> abandonMatch(Long id) {
        Match match = matchInterface.findById(id).orElse(null);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }
        match.setStatus("ABANDONED");
        matchInterface.save(match);

        return ResponseEntity.ok().body("Match abandoned successfully");
    }


    public ResponseEntity<?> getMatchesByStatus(String status) {
        List<Match> matches = matchInterface.findByStatus(status); // only matches with given status

        return ResponseEntity.ok(convertToDTO(matches));
    }

    @Cacheable(
            value = "matches",
            key = "T(java.util.Objects).hash(#sport != null ? #sport : 'All', #status != null ? #status : 'All')"
    )
    public List<MatchDTO> getmatchbystatusandSport(String sport, String status) {

        List<Match> matches;
        boolean isSportAll = (sport == null || sport.isEmpty() || sport.equalsIgnoreCase("All"));
        boolean isStatusAll = (status == null || status.isEmpty() || status.equalsIgnoreCase("All"));

        if (isSportAll && isStatusAll) {
            matches = matchInterface.findAll();
        } else if (isSportAll) {
            matches = matchInterface.findByStatus(status.toUpperCase());
        } else if (isStatusAll) {
            matches = matchInterface.findByTournament_SportName(sport);
        } else {
            matches = matchInterface.findByTournament_SportName(sport, status);
        }


        return convertToDTO(matches);
    }

    public ResponseEntity<?> getMatchesByScorer(Long id) {

        List<Match> matches = matchInterface.findByScorerID(id);
        return ResponseEntity.ok(convertToDTO(matches));
    }


    private MatchDTO convertToDTO(Match match) {
        MatchDTO matchDTO = new MatchDTO();
        matchDTO.setId(match.getId());
        matchDTO.setTournamentId(match.getTournament().getId());
        matchDTO.setTournamentName(match.getTournament().getName());
        matchDTO.setTeam1Id(match.getTeam1().getId());
        matchDTO.setTeam1Name(match.getTeam1().getName());
        matchDTO.setTeam2Id(match.getTeam2().getId());
        matchDTO.setTeam2Name(match.getTeam2().getName());
        matchDTO.setScorerId(match.getScorer().getUsername());
        matchDTO.setStatus(match.getStatus().toUpperCase());
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
        if (match.getWinnerTeam() != null) {
            matchDTO.setWinnerTeamId(match.getWinnerTeam().getId());
            matchDTO.setWinnerTeamName(match.getWinnerTeam().getName());
        }
        if (match.getStatus().equalsIgnoreCase("LIVE")) {

            if (match.getCricketInnings().size() == 1)
                matchDTO.setInningsId(match.getCricketInnings().get(0).getId());
            else
                matchDTO.setInningsId(match.getCricketInnings().get(1).getId());

        }

        return matchDTO;
    }

    public List<MatchDTO> convertToDTO(List<Match> matches) {
        return matches.stream().map(this::convertToDTO).toList();
    }
}
