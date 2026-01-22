package com.livescore.backend.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TournamentStatsDTO {
    public Long tournamentId;
    public String tournamentName;
    public String sportName;

    public String playerType;
    public LocalDate startDate;
    public LocalDate endDate;

    public int approvedTeams;
    public int matches;

    public List<PtsTableDTO> pointsTable;
    public List<TopTeamDTO> topTeams;

    public TournamentAwardsDTO awards;

    @Data
    public static class TopTeamDTO {
        public String teamName;
        public int points;
    }
}
