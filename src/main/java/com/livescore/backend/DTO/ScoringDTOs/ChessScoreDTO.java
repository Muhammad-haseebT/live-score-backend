package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;
import java.util.List;

@Data
public class ChessScoreDTO {

    // ── Request ───────────────────────────────────────────────────
    private Long    matchId;
    private Long    teamId;           // team making the move/event
    private Long    playerId;
    // MOVE, CHECK, CHECKMATE, STALEMATE, RESIGN, TIMEOUT, DRAW_AGREED, END_MATCH
    private String  eventType;
    private String  moveNotation;     // e.g. "e4", "Nf3", "O-O"
    private boolean undo;

    // ── Response ──────────────────────────────────────────────────
    private Integer team1Moves;
    private Integer team2Moves;
    private Integer team1Checks;
    private Integer team2Checks;
    private Integer totalMoves;

    private String  status;           // LIVE, COMPLETED
    private String  resultType;       // CHECKMATE, STALEMATE, RESIGN, TIMEOUT, DRAW_AGREED
    private Boolean isDraw;

    private Long    currentTurnTeamId;
    private String  currentTurnTeamName;

    private Long    matchStartTime;
    private Long    currentMoveStartTime;

    private List<ChessEventDTO> chessEvents;
    private String comment;
}
