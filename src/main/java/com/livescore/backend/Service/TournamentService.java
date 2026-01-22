package com.livescore.backend.Service;

import com.livescore.backend.DTO.TournamentRequestDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.DTO.TournamentStatsDTO;
import com.livescore.backend.DTO.PtsTableDTO;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Entity.PtsTable;
import com.livescore.backend.Interface.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TournamentService {
    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private SportsInterface sportsInterface;
    @Autowired
    private SeasonInterface seasonInterface;
    @Autowired
    private PtsTableInterface ptsTableInterface;

    @Autowired
    private MatchInterface matchInterface;

    @Autowired
    private AwardService awardService;

    public ResponseEntity<?> createTournament(TournamentRequestDTO tournament) {
        if (tournament == null) {
            return ResponseEntity.badRequest().body("Tournament details are required");
        }
        if (tournament.getName() == null || tournament.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Tournament name is required");
        }
        if (tournament.getSeasonId() == null) {
            return ResponseEntity.badRequest().body("Season id is required");
        }
        if (tournament.getSportsId() == null) {
            return ResponseEntity.badRequest().body("Sports id is required");
        }
        if (tournament.getUsername() == null || tournament.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (tournamentInterface.existsByNameAndSeasonId(tournament.getName(), tournament.getSeasonId())) {
            return ResponseEntity.badRequest().body("Tournament name already exists");
        }
        if (!accountInterface.existsActiveByUsername(tournament.getUsername())) {
            return ResponseEntity.badRequest().body("User not found");
        }
        if (!sportsInterface.existsById(tournament.getSportsId())) {
            return ResponseEntity.badRequest().body("Sports not found");
        }
        if (!seasonInterface.existsById(tournament.getSeasonId())) {
            return ResponseEntity.badRequest().body("Season not found");
        }
        Tournament tournament1=new Tournament();
        tournament1.setName(tournament.getName());
        //check role
        var organizer = accountInterface.findByUsername(tournament.getUsername());
        if (organizer == null || organizer.getRole() == null) {
            return ResponseEntity.badRequest().body("User role not found");
        }
        String role = organizer.getRole().trim().toUpperCase();
        if (!role.equals("ADMIN")) {
            return ResponseEntity.badRequest().body("Only admin can create a tournament");
        }


        tournament1.setOrganizer(organizer);
        Season season = seasonInterface.findById(tournament.getSeasonId()).orElse(null);
        if (season == null) {
            return ResponseEntity.badRequest().body("Season not found");
        }
        Sports sport = sportsInterface.findById(tournament.getSportsId()).orElse(null);
        if (sport == null) {
            return ResponseEntity.badRequest().body("Sport not found");
        }
        tournament1.setSeason(season);
        tournament1.setSport(sport);
        tournament1.setStartDate(tournament.getStartDate());
        tournament1.setEndDate(tournament.getEndDate());
        tournament1.setPlayerType(tournament.getPlayerType());
        tournament1.setTournamentType(tournament.getTournamentType());
        tournament1.setTournamentStage(tournament.getTournamentStage());

       tournamentInterface.save(tournament1);

        if (season.getSportsOffered() == null) {
            season.setSportsOffered(new ArrayList<>());
        }

        if (!season.getSportsOffered().contains(sport)) {
            season.getSportsOffered().add(sport);
            seasonInterface.save(season);
        }



        return ResponseEntity.ok().build();

    }
    public ResponseEntity<?> getTournamentById(Long id) {
        return tournamentInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    public ResponseEntity<?> getAllTournaments() {
        return ResponseEntity.ok(tournamentInterface.findAll());
    }
    public ResponseEntity<?> updateTournament(Long id, TournamentRequestDTO tournament) {
        if (tournament == null) {
            return ResponseEntity.badRequest().body("Tournament details are required");
        }
        var opt = tournamentInterface.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Tournament tournament1 = opt.get();

        if (tournament.getName() != null && !tournament.getName().isBlank()) {
            tournament1.setName(tournament.getName());
        }
        if (tournament.getUsername() != null && !tournament.getUsername().isBlank()) {
            var organizer = accountInterface.findByUsername(tournament.getUsername());
            if (organizer == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            tournament1.setOrganizer(organizer);
        }
        if (tournament.getSeasonId() != null) {
            Season season = seasonInterface.findById(tournament.getSeasonId()).orElse(null);
            if (season == null) {
                return ResponseEntity.badRequest().body("Season not found");
            }
            tournament1.setSeason(season);
        }
        if (tournament.getSportsId() != null) {
            Sports sport = sportsInterface.findById(tournament.getSportsId()).orElse(null);
            if (sport == null) {
                return ResponseEntity.badRequest().body("Sport not found");
            }
            tournament1.setSport(sport);
        }
        if (tournament.getStartDate() != null) {
            tournament1.setStartDate(tournament.getStartDate());
        }
        if (tournament.getEndDate() != null) {
            tournament1.setEndDate(tournament.getEndDate());
        }
        if (tournament.getPlayerType() != null) {
            tournament1.setPlayerType(tournament.getPlayerType());
        }
        if (tournament.getTournamentType() != null) {
            tournament1.setTournamentType(tournament.getTournamentType());
        }
        if (tournament.getTournamentStage() != null) {
            tournament1.setTournamentStage(tournament.getTournamentStage());
        }

        return ResponseEntity.ok(tournamentInterface.save(tournament1));
    }
    public ResponseEntity<?> deleteTournament(Long id) {
        if(tournamentInterface.existsById(id)){
            tournamentInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }


    public ResponseEntity<?> getOverview(Long id) {
        Tournament tournament = tournamentInterface.findById(id).orElse(null);
        if(tournament == null){
            return ResponseEntity.notFound().build();
        }
        OverViewDTO overViewDTO = new OverViewDTO();
        if (tournament.getTeams() == null) {
            overViewDTO.setTeams(0);
        } else {
            overViewDTO.setTeams((int) tournament.getTeams().stream().filter(team -> team != null && "APPROVED".equals(team.getStatus())).count());
        }
        overViewDTO.setPlayerType(tournament.getPlayerType());
        overViewDTO.setStartDate(tournament.getStartDate());
        Pageable p=PageRequest.of(0, 3);
        List<Abc> a= ptsTableInterface.findByTournamentId(id,p);
        overViewDTO.setTop(a == null ? List.of() : a);
        return ResponseEntity.ok(overViewDTO);

    }

    public ResponseEntity<?> getTournamentStats(Long tournamentId) {
        if (tournamentId == null) {
            return ResponseEntity.badRequest().body("tournamentId is required");
        }
        TournamentStatsDTO dto = getTournamentStatsDto(tournamentId);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tournament not found");
        }
        return ResponseEntity.ok(dto);
    }

    public ResponseEntity<?> getTournamentAwards(Long tournamentId) {
        if (tournamentId == null) {
            return ResponseEntity.badRequest().body("tournamentId is required");
        }
        TournamentAwardsDTO dto = getTournamentAwardsDto(tournamentId);
        return ResponseEntity.ok(dto);
    }

    @Cacheable(cacheNames = "tournamentAwards", key = "#tournamentId")
    public TournamentAwardsDTO getTournamentAwardsDto(Long tournamentId) {
        return awardService.ensureAndGetTournamentAwards(tournamentId);
    }

    @Cacheable(cacheNames = "tournamentStats", key = "#tournamentId")
    public TournamentStatsDTO getTournamentStatsDto(Long tournamentId) {
        Tournament tournament = tournamentInterface.findById(tournamentId).orElse(null);
        if (tournament == null) {
            return null;
        }

        TournamentStatsDTO dto = new TournamentStatsDTO();
        dto.tournamentId = tournament.getId();
        dto.tournamentName = tournament.getName();
        dto.playerType = tournament.getPlayerType();
        dto.startDate = tournament.getStartDate();
        dto.endDate = tournament.getEndDate();
        dto.sportName = tournament.getSport() == null ? null : tournament.getSport().getName();

        dto.approvedTeams = tournament.getTeams() == null ? 0 : (int) tournament.getTeams().stream()
                .filter(team -> team != null && "APPROVED".equals(team.getStatus()))
                .count();

        List<com.livescore.backend.Entity.Match> matches = matchInterface.findByTournament_Id(tournamentId);
        dto.matches = matches == null ? 0 : matches.size();

        List<PtsTable> ptsTables = ptsTableInterface.findByTournamentId(tournamentId);
        List<PtsTableDTO> ptsTableDtos = (ptsTables == null ? List.<PtsTableDTO>of() : ptsTables.stream().map(pt -> {
            PtsTableDTO d = new PtsTableDTO();
            d.setTeamName(pt.getTeam() == null ? null : pt.getTeam().getName());
            d.setId(pt.getId());
            d.setTournamentId(pt.getTournament() == null ? null : pt.getTournament().getId());
            d.setTeamId(pt.getTeam() == null ? null : pt.getTeam().getId());
            d.setPlayed(pt.getPlayed());
            d.setWins(pt.getWins());
            d.setLosses(pt.getLosses());
            d.setPoints(pt.getPoints());
            d.setNrr(pt.getNrr());
            return d;
        }).sorted((a, b) -> {
            int c = Integer.compare(b.getPoints(), a.getPoints());
            if (c != 0) return c;
            return Double.compare(b.getNrr(), a.getNrr());
        }).collect(Collectors.toList()));

        dto.pointsTable = ptsTableDtos;

        Pageable topPage = PageRequest.of(0, 3);
        List<Abc> top = ptsTableInterface.findByTournamentId(tournamentId, topPage);
        dto.topTeams = (top == null ? List.<TournamentStatsDTO.TopTeamDTO>of() : top.stream().map(x -> {
            TournamentStatsDTO.TopTeamDTO t = new TournamentStatsDTO.TopTeamDTO();
            t.teamName = x.getName();
            t.points = x.getPoints();
            return t;
        }).collect(Collectors.toList()));

        if (dto.sportName != null && dto.sportName.equalsIgnoreCase("CRICKET")) {
            dto.awards = awardService.ensureAndGetTournamentAwards(tournamentId);
        }

        return dto;
    }

}

@Data
class OverViewDTO {
    private int teams;
    private String playerType;
    private LocalDate startDate;
    List<Abc> top;
}
