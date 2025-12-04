package com.livescore.backend.Service;

import com.livescore.backend.Entity.Season;
import com.livescore.backend.Interface.SeasonInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonService {
    @Autowired
    SeasonInterface seasonInterface;

    public Season createSeason(Season season) {
        return seasonInterface.save(season);
    }

    public List<Season> getAllSeasons() {
        return seasonInterface.findAll();
    }
}
