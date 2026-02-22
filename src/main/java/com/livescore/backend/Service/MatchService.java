package com.livescore.backend.Service;

import com.livescore.backend.DTO.MatchDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Interface.Cricket.MatchStateInterface;
import com.livescore.backend.Util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @Autowired
    private AwardInterface awardInterface;
    @Autowired
    private MatchStateInterface matchStateInterface;
    @Autowired
    private PtsTableInterface ptsTableInterface;




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


    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    public ResponseEntity<?> createMatch(MatchDTO matchDTO) {
        ResponseEntity<String> validation = validateMatchDTO(matchDTO);
        if (validation != null) return validation;
        Match match = new Match();
        applyMatchDTO(match, matchDTO);
        matchInterface.save(match);
        return ResponseEntity.ok().body("Match created successfully");
    }

    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
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


    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    @Transactional
    public ResponseEntity<?> endMatch(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + matchId + " does not exist");
        }

        if (match.getStatus() != null && (match.getStatus().equalsIgnoreCase("FINISHED")
                || match.getStatus().equalsIgnoreCase("COMPLETED"))) {
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

        // 1. Mark match as finished
        match.setStatus("COMPLETED");
        matchInterface.save(match);

        // 2. Update points table
        ptsTableService.updatePointsTableAfterMatch(matchId);

        statsService.onMatchEnd(matchId);

        return ResponseEntity.ok("Match ended successfully and stats updated");
    }

    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    public ResponseEntity<?> abandonMatch(Long id) {
        Match match = matchInterface.findById(id).orElse(null);
        Long i1,i2;

        if (match == null) {
            return ResponseEntity.badRequest().body("Match with id " + id + " does not exist");
        }
        i1=match.getCricketInnings().get(0).getId();
        i2=match.getCricketInnings().get(1).getId();
        MatchState m1=matchStateInterface.findByInnings_Id(i1);
        MatchState m2=matchStateInterface.findByInnings_Id(i2);

        PtsTable p1=ptsTableInterface.findByTeamIdAndTournamentId(match.getTeam1().getId(), match.getTournament().getId());
        PtsTable p2=ptsTableInterface.findByTeamIdAndTournamentId(match.getTeam2().getId(), match.getTournament().getId());
        if(match.getStatus()=="Live"){
            if(m1!=null&&m1.getOvers()>=5){
             endMatch(id);
            }

        }
        p1.setPoints(p1.getPoints()+1);
        p2.setPoints(p2.getPoints()+1);
        ptsTableInterface.save(p1);
        ptsTableInterface.save(p2);
        match.setStatus(Constants.STATUS_ABANDONED);
        matchInterface.save(match);

        return ResponseEntity.ok().body("Match abandoned successfully");
    }


    public ResponseEntity<?> getMatchesByStatus(String status) {
        List<Match> matches = matchInterface.findByStatus(status); // only matches with given status

        return ResponseEntity.ok(convertToDTO(matches));
    }

    @Cacheable(
            value = "matches",
            key = "'sport:' + (#sport != null ? #sport : 'All') + ':status:' + (#status != null ? #status : 'All')"
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
            else if (match.getCricketInnings().size() == 2)

                matchDTO.setInningsId(match.getCricketInnings().get(1).getId());

        }

        return matchDTO;
    }

    public List<MatchDTO> convertToDTO(List<Match> matches) {
        return matches.stream().map(this::convertToDTO).toList();
    }





}
