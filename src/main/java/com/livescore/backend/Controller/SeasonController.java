package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Season;
import com.livescore.backend.Service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
