package com.livescore.backend.DTO;

import lombok.Data;

import java.util.List;
@Data
// InningsDTO.java
public class InningsDTO {
    public Long inningsId;
    public Long teamId;
    public String teamName;
    public int totalRuns;
    public int wickets;
    public int totalBalls; // legal deliveries
    public String oversString; // e.g., "14.3"
    public int extras;
    public List<BallDTO> balls; // optional: ball-by-ball
}
