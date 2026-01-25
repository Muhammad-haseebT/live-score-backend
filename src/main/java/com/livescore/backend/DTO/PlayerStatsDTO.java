package com.livescore.backend.DTO;


import lombok.Data;

@Data
public class PlayerStatsDTO extends PlayerStatDTO {
    public double strikeRate;
    public int highest;
    public int notOut;
    public double battingAvg;

    public int points; // your points calc
    public int matchesPlayed;
}
