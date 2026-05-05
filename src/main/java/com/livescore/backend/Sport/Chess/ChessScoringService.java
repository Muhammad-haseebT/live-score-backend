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
    private final TeamInterface            teamInterface;
    private final ChessStatsService        chessStatsService;
    private final ChessPtsTableService     chessPtsTableService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        if (!"CHECKMATE".equalsIgnoreCase(req.getEventType()))
            throw new IllegalArgumentException("Unknown event: " + req.getEventType());

        handleCheckmate(req, state, match);

        chessStateInterface.save(state);
        if ("COMPLETED".equals(state.getStatus())) {
            chessPtsTableService.updateAfterMatch(match.getId());
            chessStatsService.onMatchEnd(match.getId());
        }
        return toDTO(state, "");
    }

    // ── CHECKMATE: teamId = winner ────────────────────────────────
    private void handleCheckmate(ChessScoreDTO req, ChessMatchState state, Match match) {
        Team winner = teamInterface.findById(req.getTeamId()).get();
        state.setStatus("COMPLETED");
        state.setResultType("CHECKMATE");
        match.setWinnerTeam(winner);
        matchInterface.save(match);
        chessEventInterface.save(build(match, winner, "CHECKMATE", state));
    }

    // ── UNDO ─────────────────────────────────────────────────────
    private ChessScoreDTO undoLast(Long matchId) {
        ChessEvent last = chessEventInterface.findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (ChessScoreDTO) getCurrentMatchState(matchId);

        ChessMatchState state = chessStateInterface.findByMatch_Id(matchId).get();
        Match match = last.getMatch();

        // Only CHECKMATE events exist now — always revert to LIVE
        state.setStatus("LIVE");
        state.setResultType(null);
        match.setWinnerTeam(null);
        matchInterface.save(match);

        chessEventInterface.delete(last);
        chessStateInterface.save(state);
        return toDTO(state, "UNDO");
    }

    // ── UTIL ─────────────────────────────────────────────────────
    private ChessMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        ChessMatchState s = new ChessMatchState();
        s.setMatch(match);
        s.setStatus("LIVE");
        s.setMatchStartTime(System.currentTimeMillis());
        return chessStateInterface.save(s);
    }

    private ChessEvent build(Match match, Team team, String type, ChessMatchState state) {
        ChessEvent ev = new ChessEvent();
        ev.setMatch(match); ev.setTeam(team); ev.setEventType(type);
        ev.setEventTimeSeconds(state.getMatchStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getMatchStartTime()) / 1000) : 0);
        return ev;
    }

    private ChessScoreDTO toDTO(ChessMatchState state, String comment) {
        ChessScoreDTO dto = new ChessScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setStatus(state.getStatus());
        dto.setResultType(state.getResultType());
        dto.setMatchStartTime(state.getMatchStartTime());
        dto.setIsDraw(false); // draw removed
        dto.setComment(comment);
        dto.setChessEvents(chessEventInterface.findByMatch_IdOrderByIdAsc(state.getMatch().getId())
                .stream().map(this::toEventDTO).collect(Collectors.toList()));
        return dto;
    }

    private ChessEventDTO toEventDTO(ChessEvent ev) {
        ChessEventDTO dto = new ChessEventDTO();
        dto.setId(ev.getId());
        dto.setEventType(ev.getEventType());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        if (ev.getTeam() != null) { dto.setTeamId(ev.getTeam().getId()); dto.setTeamName(ev.getTeam().getName()); }
        return dto;
    }
}