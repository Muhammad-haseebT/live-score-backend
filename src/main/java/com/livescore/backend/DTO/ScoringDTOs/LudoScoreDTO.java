// ══ LudoScoreDTO.java ════════════════════════════════════════════
package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;
import java.util.List;

@Data
public class LudoScoreDTO {

    // ── Request ───────────────────────────────────────────────────
    private Long    matchId;
    private Long    teamId;
    private Long    playerId;
    // CAPTURE, HOME_RUN, WIN, END_MATCH
    private String  eventType;
    private boolean undo;

    // ── Response ──────────────────────────────────────────────────
    private Integer team1HomeRuns;
    private Integer team2HomeRuns;
    private Integer team1Captures;
    private Integer team2Captures;
    private String  status;       // LIVE, COMPLETED
    private Long    matchStartTime;

    private List<LudoEventDTO> ludoEvents;
    private String comment;
}
