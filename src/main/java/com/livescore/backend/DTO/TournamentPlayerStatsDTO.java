package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class TournamentPlayerStatsDTO {
    public Long tournamentId;
    public Long playerId;
    public String playerName;

    public int matchesPlayed;
    public int manOfMatchCount;

    // totals
    public int totalRuns;
    public int totalWickets;

    // batting
    public int ballsFaced;
    public double strikeRate;
    public int highestScore;
    public int fours;
    public int sixes;
    public int notOuts;
    public double battingAverage;

    // bowling
    public int ballsBowled;
    public int runsConceded;
    public double economy;
    public double bowlingAverage;

    // best bowling figures across tournament (best match)
    public Integer bestFigureWickets;
    public Integer bestFigureRuns;
    public Long bestFigureMatchId;
}
