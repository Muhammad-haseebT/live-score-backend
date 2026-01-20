package com.livescore.backend.DTO;

import com.livescore.backend.Entity.Sports;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data

public class MatchDTO {
    private Long id;

    private Long tournamentId;
    private String tournamentName;

    private Long team1Id;
    private String team1Name;

    private Long team2Id;
    private String team2Name;

    private String scorerId;
    private String status;
    private String venue;
    private LocalDate date;
    private LocalTime time;

    private Long tossWinnerId;
    private String decision;
    private Long winnerTeamId;
    private String winnerTeamName;
    private Long sportId;
    private int overs;
    private int sets;
}

