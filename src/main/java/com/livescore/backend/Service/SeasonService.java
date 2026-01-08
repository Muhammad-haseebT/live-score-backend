package com.livescore.backend.Service;

import com.livescore.backend.DTO.SeasonCreateRequestDTO;
import com.livescore.backend.DTO.SeasonSportsRequestDTO;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.SeasonInterface;
import com.livescore.backend.Interface.SportsInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeasonService {
    @Autowired
    private SeasonInterface seasonInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private SportsInterface sportsInterface;

    public ResponseEntity<?> createSeason(SeasonCreateRequestDTO season) {
        if (season.getName() == null || season.getName().isEmpty()) {
            return ResponseEntity.badRequest().body("Season name is required");
        }
        if (seasonInterface.existsByName(season.getName())) {
            return ResponseEntity.badRequest().body("Season name already exists");
        }
        if (!accountInterface.existsByUsername(season.getUsername())) {
            return ResponseEntity.badRequest().body(season.getUsername());
        }
        var rolecheck = accountInterface.findByUsername(season.getUsername());
        if (!rolecheck.getRole().equalsIgnoreCase("admin")) {
            return ResponseEntity.badRequest().body("Only admin can create a season");
        }

        Season season1 = new Season();
        season1.setName(season.getName());
        season1.setAccount(rolecheck);


        return ResponseEntity.ok(seasonInterface.save(season1));

    }


    public ResponseEntity<?> getSeasonById(Long id) {
        if (seasonInterface.findById(id).isPresent()) {
            return ResponseEntity.ok(
                    seasonInterface.findSportWiseTournamentCount(id)
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> getAllSeasons() {
        return ResponseEntity.ok(seasonInterface.findAll());
    }

    public ResponseEntity<?> updateSeason(Long id, Season season) {
        if (seasonInterface.findById(id).isPresent()) {
            Season season1 = seasonInterface.findById(id).get();
            season1.setName(season.getName());
            return ResponseEntity.ok(seasonInterface.save(season1));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> deleteSeason(Long id) {
        if (seasonInterface.findById(id).isPresent()) {
            seasonInterface.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> getSeasonNames() {
        return ResponseEntity.ok(seasonInterface.findAll().stream().map(Season::getName).collect(Collectors.toList()));
    }

    public ResponseEntity<?> getSeasonWiseTournament(Long id) {
        if (seasonInterface.findById(id).isPresent()) {
            List<SportTournamentCount> s = seasonInterface.findSportWiseTournamentCount(id);
            List<Tournament> t = new ArrayList<>();
            for (SportTournamentCount sr : s) {
                t = tournamentInterface.findBySeasonIdAndSportName(id, sr.getName());
            }
            return ResponseEntity.ok(t);

        } else {
            return ResponseEntity.notFound().build();
        }

    }

    public ResponseEntity<?> addSportsToSeason(SeasonSportsRequestDTO request)
    {
        Season season = seasonInterface.findById(request.getSeasonId())
                .orElseThrow(() -> new RuntimeException("Season not found"));

        List<Sports> sportsList = sportsInterface.findAllById(request.getSportsIds());
        if (sportsList.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid sports found for given IDs");
        }

        if (season.getSportsOffered() == null) {
            season.setSportsOffered(new ArrayList<>());
        }

        for (Sports s : sportsList) {
            if (!season.getSportsOffered().contains(s)) {
                season.getSportsOffered().add(s);
            }
        }

       return ResponseEntity.ok(seasonInterface.save(season));
    }
}
