package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerFullStatsDTO {
    public Long playerId;
    public String playerName;

    public Integer totalRuns;
    public Integer highest;
    public Integer ballsFaced;

    public Integer ballsBowled;
    public Integer runsConceded;

    public Double strikeRate;
    public Double economy;

    public Double battingAvg;
    public Double bowlingAverage;

    public Integer notOuts;
    public Integer matchesPlayed;

    public Integer wickets;
    public Integer fours;
    public Integer sixes;

    public Integer pomCount;
}
