package com.livescore.backend.Sport.Chess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.ScoringDTOs.ChessEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.ChessScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Chess.ChessEvent;
import com.livescore.backend.Entity.Chess.ChessMatchState;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service("CHESS")
@RequiredArgsConstructor
public class ChessScoringService implements ScoringServiceInterface {

    private final ChessEventInterface      chessEventInterface;
    private final ChessMatchStateInterface chessStateInterface;
    private final MatchInterface           matchInterface;
    private final PlayerInterface          playerInterface;
    private final TeamInterface            teamInterface;
    private final ChessStatsService        chessStatsService;
    private final ChessPtsTableService     chessPtsTableService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────
    @Override
    @Cacheable(value = "chessStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        ChessMatchState state = chessStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "chessStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        ChessScoreDTO req = objectMapper.convertValue(rawPayload, ChessScoreDTO.class);
        return process(req);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "chessStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long unused) {
        return undoLast(matchId);
    }

    // ─────────────────────────────────────────
    private ChessScoreDTO process(ChessScoreDTO req) {
        ChessMatchState state = chessStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();
        ChessEvent saved = null;

        switch (req.getEventType().toUpperCase()) {
            case "MOVE"        -> saved = handleMove(req, state, match);
            case "CHECK"       -> saved = handleCheck(req, state, match);
            case "CHECKMATE"   -> { saved = handleTerminal(req, state, match, "CHECKMATE", false); }
            case "RESIGN"      -> { saved = handleTerminal(req, state, match, "RESIGN", false); }
            case "TIMEOUT"     -> { saved = handleTerminal(req, state, match, "TIMEOUT", false); }
            case "STALEMATE"   -> { saved = handleDraw(req, state, match, "STALEMATE"); }
            case "DRAW_AGREED" -> { saved = handleDraw(req, state, match, "DRAW_AGREED"); }
            case "END_MATCH"   -> handleEndMatch(state, match);
            default -> throw new IllegalArgumentException("Unknown event: " + req.getEventType());
        }

        chessStateInterface.save(state);
        if (saved != null) chessStatsService.onEventSaved(saved);
        if ("COMPLETED".equals(state.getStatus())) {
            chessPtsTableService.updateAfterMatch(match.getId());
            chessStatsService.onMatchEnd(match.getId());
        }
        return toDTO(state, "");
    }

    // ─────────────────────────────────────────
    // EVENT HANDLERS
    // ─────────────────────────────────────────

    /** MOVE — team makes a move, turn switches */
    private ChessEvent handleMove(ChessScoreDTO req, ChessMatchState state, Match match) {
        Team team   = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;

        // Increment move counts
        boolean isT1 = team.getId().equals(match.getTeam1().getId());
        if (isT1) state.setTeam1Moves(state.getTeam1Moves() + 1);
        else      state.setTeam2Moves(state.getTeam2Moves() + 1);
        state.setTotalMoves(state.getTotalMoves() + 1);

        // Switch turn to opponent
        state.setCurrentTurnTeam(isT1 ? match.getTeam2() : match.getTeam1());
        state.setCurrentMoveStartTime(System.currentTimeMillis());

        ChessEvent ev = build(match, team, plyr, "MOVE", state);
        ev.setMoveNotation(req.getMoveNotation());
        ev.setMoveNumber(state.getTotalMoves());
        return chessEventInterface.save(ev);
    }

    /** CHECK — team delivers check (no turn switch, just record) */
    private ChessEvent handleCheck(ChessScoreDTO req, ChessMatchState state, Match match) {
        Team team = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        boolean isT1 = team.getId().equals(match.getTeam1().getId());
        if (isT1) state.setTeam1Checks(state.getTeam1Checks() + 1);
        else      state.setTeam2Checks(state.getTeam2Checks() + 1);
        ChessEvent ev = build(match, team, plyr, "CHECK", state);
        ev.setMoveNotation(req.getMoveNotation());
        ev.setMoveNumber(state.getTotalMoves());
        return chessEventInterface.save(ev);
    }

    /**
     * CHECKMATE / RESIGN / TIMEOUT
     * teamId = winning team for CHECKMATE
     * teamId = resigning/timing-out team for RESIGN/TIMEOUT
     */
    private ChessEvent handleTerminal(ChessScoreDTO req, ChessMatchState state,
                                       Match match, String type, boolean isDraw) {
        Team team = teamInterface.findById(req.getTeamId()).get();
        state.setStatus("COMPLETED");
        state.setResultType(type);

        Team winner;
        if ("CHECKMATE".equals(type)) {
            winner = team; // team that delivered checkmate
        } else {
            // RESIGN / TIMEOUT — the teamId is the loser
            boolean loserIsT1 = team.getId().equals(match.getTeam1().getId());
            winner = loserIsT1 ? match.getTeam2() : match.getTeam1();
        }
        match.setWinnerTeam(winner);
        matchInterface.save(match);

        ChessEvent ev = build(match, team, null, type, state);
        ev.setMoveNumber(state.getTotalMoves());
        return chessEventInterface.save(ev);
    }

    /** STALEMATE / DRAW_AGREED — no winner */
    private ChessEvent handleDraw(ChessScoreDTO req, ChessMatchState state, Match match, String type) {
        Team team = req.getTeamId() != null ? teamInterface.findById(req.getTeamId()).orElse(null) : null;
        state.setStatus("COMPLETED");
        state.setResultType(type);
        // No winner for draw
        ChessEvent ev = build(match, team, null, type, state);
        ev.setMoveNumber(state.getTotalMoves());
        return chessEventInterface.save(ev);
    }

    /** Manual END_MATCH — more moves wins (unusual, but as fallback) */
    private void handleEndMatch(ChessMatchState state, Match match) {
        chessEventInterface.save(build(match, null, null, "END_MATCH", state));
        state.setStatus("COMPLETED");
        state.setResultType("END_MATCH");
        if (state.getTeam1Moves() >= state.getTeam2Moves()) match.setWinnerTeam(match.getTeam1());
        else                                                  match.setWinnerTeam(match.getTeam2());
        matchInterface.save(match);
    }

    // ─────────────────────────────────────────
    // UNDO
    // ─────────────────────────────────────────
    private ChessScoreDTO undoLast(Long matchId) {
        ChessEvent last = chessEventInterface.findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (ChessScoreDTO) getCurrentMatchState(matchId);

        ChessMatchState state = chessStateInterface.findByMatch_Id(matchId).get();
        Match match = last.getMatch();

        switch (last.getEventType().toUpperCase()) {
            case "MOVE" -> {
                boolean wasT1 = last.getTeam() != null && last.getTeam().getId().equals(match.getTeam1().getId());
                if (wasT1) state.setTeam1Moves(Math.max(0, state.getTeam1Moves() - 1));
                else       state.setTeam2Moves(Math.max(0, state.getTeam2Moves() - 1));
                state.setTotalMoves(Math.max(0, state.getTotalMoves() - 1));
                // Switch turn back
                state.setCurrentTurnTeam(wasT1 ? match.getTeam1() : match.getTeam2());
            }
            case "CHECK" -> {
                boolean wasT1 = last.getTeam() != null && last.getTeam().getId().equals(match.getTeam1().getId());
                if (wasT1) state.setTeam1Checks(Math.max(0, state.getTeam1Checks() - 1));
                else       state.setTeam2Checks(Math.max(0, state.getTeam2Checks() - 1));
            }
            case "CHECKMATE", "RESIGN", "TIMEOUT", "STALEMATE", "DRAW_AGREED", "END_MATCH" -> {
                state.setStatus("LIVE");
                state.setResultType(null);
                match.setWinnerTeam(null);
                matchInterface.save(match);
            }
        }

        chessEventInterface.delete(last);
        chessStateInterface.save(state);
        return toDTO(state, "UNDO");
    }

    // ─────────────────────────────────────────
    // UTIL
    // ─────────────────────────────────────────
    private ChessEvent build(Match match, Team team, Player player, String type, ChessMatchState state) {
        ChessEvent ev = new ChessEvent();
        ev.setMatch(match); ev.setTeam(team); ev.setPlayer(player); ev.setEventType(type);
        ev.setEventTimeSeconds(state.getMatchStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getMatchStartTime()) / 1000) : 0);
        return ev;
    }

    private ChessMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        ChessMatchState s = new ChessMatchState();
        s.setMatch(match);
        s.setTeam1Moves(0); s.setTeam2Moves(0);
        s.setTeam1Checks(0); s.setTeam2Checks(0);
        s.setTotalMoves(0); s.setStatus("LIVE");
        s.setMatchStartTime(System.currentTimeMillis());
        s.setCurrentMoveStartTime(System.currentTimeMillis());
        // White (team1) goes first by convention
        s.setCurrentTurnTeam(match.getTeam1());
        return chessStateInterface.save(s);
    }

    // ─────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────
    private ChessScoreDTO toDTO(ChessMatchState state, String comment) {
        ChessScoreDTO dto = new ChessScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1Moves(state.getTeam1Moves());
        dto.setTeam2Moves(state.getTeam2Moves());
        dto.setTeam1Checks(state.getTeam1Checks());
        dto.setTeam2Checks(state.getTeam2Checks());
        dto.setTotalMoves(state.getTotalMoves());
        dto.setStatus(state.getStatus());
        dto.setResultType(state.getResultType());
        dto.setMatchStartTime(state.getMatchStartTime());
        dto.setCurrentMoveStartTime(state.getCurrentMoveStartTime());

        boolean isDraw = "STALEMATE".equals(state.getResultType())
                || "DRAW_AGREED".equals(state.getResultType());
        dto.setIsDraw(isDraw);

        if (state.getCurrentTurnTeam() != null) {
            dto.setCurrentTurnTeamId(state.getCurrentTurnTeam().getId());
            dto.setCurrentTurnTeamName(state.getCurrentTurnTeam().getName());
        }

        dto.setComment(comment);
        dto.setChessEvents(chessEventInterface.findByMatch_IdOrderByIdAsc(state.getMatch().getId())
                .stream().map(this::toEventDTO).collect(Collectors.toList()));
        return dto;
    }

    private ChessEventDTO toEventDTO(ChessEvent ev) {
        ChessEventDTO dto = new ChessEventDTO();
        dto.setId(ev.getId()); dto.setEventType(ev.getEventType());
        dto.setMoveNotation(ev.getMoveNotation()); dto.setMoveNumber(ev.getMoveNumber());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        if (ev.getPlayer() != null) { dto.setPlayerId(ev.getPlayer().getId()); dto.setPlayerName(ev.getPlayer().getName()); }
        if (ev.getTeam()   != null) { dto.setTeamId(ev.getTeam().getId());     dto.setTeamName(ev.getTeam().getName()); }
        return dto;
    }
}
