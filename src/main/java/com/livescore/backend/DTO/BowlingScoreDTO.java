package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class BowlingScoreDTO {
    public Long playerId;
    public String playerName;

    public int balls;
    public String overs;

    public int maidens;
    public int runsConceded;
    public int wickets;

    public double economy;
}
