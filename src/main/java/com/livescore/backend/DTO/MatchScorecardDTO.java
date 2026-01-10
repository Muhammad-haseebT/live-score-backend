package com.livescore.backend.DTO;

import lombok.Data;

@Data

public class MatchScorecardDTO {
    public Long matchId;
    public InningsDTO firstInnings;
    public InningsDTO secondInnings;
    public String status;
}
