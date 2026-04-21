package com.livescore.backend.Entity.Chess;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "chess_match_states")
public class ChessMatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // ── Move counts ───────────────────────────────────────────────
    @Column(name = "team1_moves") private Integer team1Moves = 0;
    @Column(name = "team2_moves") private Integer team2Moves = 0;

    // ── Checks delivered ─────────────────────────────────────────
    @Column(name = "team1_checks") private Integer team1Checks = 0;
    @Column(name = "team2_checks") private Integer team2Checks = 0;

    // ── Result: LIVE, CHECKMATE, STALEMATE, DRAW, RESIGN, TIMEOUT, COMPLETED
    @Column(name = "status") private String status = "LIVE";

    // ── Who has the current turn ──────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_turn_team_id")
    private Team currentTurnTeam;

    // ── Result type (for display) ─────────────────────────────────
    @Column(name = "result_type") private String resultType; // CHECKMATE, STALEMATE, RESIGN, TIMEOUT, DRAW_AGREED

    @Column(name = "match_start_time") private Long matchStartTime;
    @Column(name = "current_move_start_time") private Long currentMoveStartTime;

    // ── Total moves in match ──────────────────────────────────────
    @Column(name = "total_moves") private Integer totalMoves = 0;
}
