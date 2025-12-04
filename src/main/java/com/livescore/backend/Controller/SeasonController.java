package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Season;
import com.livescore.backend.Service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SeasonController {
    @Autowired
    SeasonService seasonService;

    @PostMapping("/season")
    public Season createSeason(@RequestBody Season season) {
        return seasonService.createSeason(season);
    }

    @GetMapping("/season")
    public List<Season> getAllSeasons() {
        return seasonService.getAllSeasons();
    }

    @GetMapping("season/{id}")
    public Season getSeasonById(@PathVariable int id) {
        return seasonService.getSeasonById(id);
    }
    @DeleteMapping("season/{id}")
    public void deleteSeason(@PathVariable int id) {
        seasonService.deleteSeason(id);
    }
}
