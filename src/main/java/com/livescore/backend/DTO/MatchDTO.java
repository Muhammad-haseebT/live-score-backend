package com.livescore.backend.DTO;

import com.livescore.backend.Entity.Sports;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchDTO {
    private Long id;
    private Long tournamentId;
    private Long team1Id;
    private Long team2Id;
    private Long scorerId;
    private String status;
    private String venue;
    private LocalDate date;
    private LocalTime time;
    private Long tossWinnerId;
    private String decision;
    private Long winnerTeamId;
    private Long sportId;
    private int overs;
    private int sets;




}
