package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerStatDTO {
    public Long playerId;
    public String playerName;
    public Integer runs;
    public Integer wickets;
    public Integer pomCount; // Player-of-match count
    public Double compositeScore; // if you use composite scoring
}