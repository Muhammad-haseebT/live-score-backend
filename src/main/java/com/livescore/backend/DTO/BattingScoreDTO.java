package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class BattingScoreDTO {
    public Long playerId;
    public String playerName;

    public int runs;
    public int balls;
    public int fours;
    public int sixes;
    public double strikeRate;

    public boolean notOut;
}
