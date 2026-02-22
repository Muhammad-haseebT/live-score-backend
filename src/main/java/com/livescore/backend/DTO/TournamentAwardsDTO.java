package com.livescore.backend.DTO;

import lombok.Data;
import java.util.List;

@Data
public class TournamentAwardsDTO {
    private Long tournamentId;
    private String tournamentName;

    private AwardDTO manOfTournament;
    private AwardDTO bestBatsman;
    private AwardDTO bestBowler;
    private AwardDTO bestFielder;
    private AwardDTO mostSixes;

    private List<AwardDTO> allAwards;

    // top performers lists
    private List<PlayerStatsRow> topRunScorers;
    private List<PlayerStatsRow> topWicketTakers;

    @Data
    public static class AwardDTO {
        private Long playerId;
        private String playerName;
        private String awardType;
        private Integer points;
        private String reason;
    }

    @Data
    public static class PlayerStatsRow {
        private Long playerId;
        private String playerName;
        private Integer runs;
        private Integer wickets;
        private Integer ballsFaced;
        private Integer ballsBowled;
        private Integer fours;
        private Integer sixes;
        private Integer highest;
        private Integer strikeRate;
        private Integer catches;
        private Integer runouts;
        private Integer stumpings;
        private Integer fifties;
        private Integer hundreds;
        private Integer maidens;
        private Integer dotBalls;
        private Integer playerOfMatchCount;
        private Integer totalPoints;
    }
}