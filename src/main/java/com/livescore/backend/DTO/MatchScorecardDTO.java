package com.livescore.backend.DTO;

import lombok.Data;

@Data
// MatchScorecardDTO.java
public class MatchScorecardDTO {
    public Long matchId;
    public InningsDTO firstInnings;
    public InningsDTO secondInnings;
    public String status;
}
