package com.livescore.backend.DTO;

import java.util.List;

public class SeasonSportsRequestDTO {
    private Long seasonId;
    private List<Long> sportsIds;

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }

    public List<Long> getSportsIds() { return sportsIds; }
    public void setSportsIds(List<Long> sportsIds) { this.sportsIds = sportsIds; }
}
