package com.livescore.backend.DTO;


public class PlayerStatsDTO {
    public Long playerId;
    public String playerName;
    public int runs;
    public int ballsFaced;
    public double strikeRate;
    public int fours;
    public int sixes;
    public int highest;
    public int notOut;

    public int wickets;
    public int ballsBowled;
    public int runsConceded;
    public double economy;
    public double bowlingAverage; // runsConceded / wickets (inf if 0)

    public int points; // your points calc


}
