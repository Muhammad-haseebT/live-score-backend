package com.livescore.backend.Service;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.SeasonInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonService {
    @Autowired
    SeasonInterface seasonInterface;
    @Autowired
    AccountInterface ai;

    public Season createSeason(Season season) {
        String  arid=season.getAccount().getArid();
       Account acc= ai.findByArid(arid);
       season.setAccount(acc);

        return seasonInterface.save(season);
    }

    public List<Season> getAllSeasons() {
        return seasonInterface.findAll();
    }

    public Season getSeasonById(int id) {
        return seasonInterface.findById(id).orElse(null);
    }

    public void deleteSeason(int id) {
        seasonInterface.deleteById(id);
        return;
    }
}
