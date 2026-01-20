package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PtsTableDTO {
    private Long id;
    private Long tournamentId;
    private Long teamId;
    private Long winnerId;
    private Long loserId;
    private String teamName;
    private int played;
    private int wins;
    private int losses;
    private  int points;
    private double nrr;

}

