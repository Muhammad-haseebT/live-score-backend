package com.livescore.backend.DTO;

import lombok.Data;

import java.util.List;

@Data
public class TournamentAwardsDTO {
    public Long tournamentId;

    public Long manOfTournamentId;
    public String manOfTournamentName;

    public Long highestScorerId;
    public String highestScorerName;
    public Integer highestRuns;

    public Long bestBatsmanId;
    public String bestBatsmanName;
    public Integer bestBatsmanRuns;

    public Long bestBowlerId;
    public String bestBowlerName;
    public Integer bestBowlerWickets;

    public List<PlayerStatDTO> topBatsmen;
    public List<PlayerStatDTO> topBowlers;
}