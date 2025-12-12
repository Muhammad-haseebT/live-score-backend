package com.livescore.backend.DTO;

import lombok.Data;

@Data

public class AwardsDTO {
    public Long matchId;
    public Long manOfMatchId;
    public String manOfMatchName;
    public Long bestBatsmanId;
    public String bestBatsmanName;
    public Long bestBowlerId;
    public String bestBowlerName;
}
