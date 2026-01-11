package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class TournamentResponseDTO {
    private Long id;
    private String name;
    private Long seasonId;
    private Long sportsId;
    private String startDate;
    private String endDate;
    private String playerType;
    private String tournamentType;
    private String tournamentStage;
}

