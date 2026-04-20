package com.livescore.backend.Sport.Ludo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.ScoringDTOs.LudoEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.LudoScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Ludo.LudoEvent;
import com.livescore.backend.Entity.Ludo.LudoMatchState;
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

@Service("LUDO")
@RequiredArgsConstructor
public class LudoScoringService implements ScoringServiceInterface {

    private final LudoEventInterface      ludoEventInterface;
    private final LudoMatchStateInterface ludoStateInterface;
    private final MatchInterface          matchInterface;
    private final PlayerInterface         playerInterface;
    private final TeamInterface           teamInterface;
    private final LudoStatsService        ludoStatsService;
    private final LudoPtsTableService     ludoPtsTableService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────
    @Override
    @Cacheable(value = "ludoStates", key = "#matchId")
    public Object getCurrentMatchState(Long matchId) {
        LudoMatchState state = ludoStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "ludoStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        LudoScoreDTO req = objectMapper.convertValue(rawPayload, LudoScoreDTO.class);
        return process(req);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "ludoStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long unused) {
        return undoLast(matchId);
    }

    // ─────────────────────────────────────────
    private LudoScoreDTO process(LudoScoreDTO req) {
        LudoMatchState state = ludoStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();
        LudoEvent saved = null;

        switch (req.getEventType().toUpperCase()) {
            case "HOME_RUN" -> saved = handleHomeRun(req, state, match);
            case "CAPTURE"  -> saved = handleCapture(req, state, match);
            case "WIN"      -> handleWin(req, state, match);
            case "END_MATCH"-> handleEndMatch(state, match);
            default -> throw new IllegalArgumentException("Unknown event: " + req.getEventType());
        }

        ludoStateInterface.save(state);
        if (saved != null) ludoStatsService.onEventSaved(saved);
        if ("COMPLETED".equals(state.getStatus())) {
            ludoPtsTableService.updateAfterMatch(match.getId());
            ludoStatsService.onMatchEnd(match.getId());
        }
        return toDTO(state, "");
    }

    // ── HOME_RUN: team gets a piece to home ──────────────────────
    private LudoEvent handleHomeRun(LudoScoreDTO req, LudoMatchState state, Match match) {
        Team team   = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        boolean t1  = team.getId().equals(match.getTeam1().getId());
        if (t1) state.setTeam1HomeRuns(state.getTeam1HomeRuns() + 1);
        else    state.setTeam2HomeRuns(state.getTeam2HomeRuns() + 1);
        return ludoEventInterface.save(build(match, team, plyr, "HOME_RUN", state));
    }

    // ── CAPTURE: team sends opponent piece back ───────────────────
    private LudoEvent handleCapture(LudoScoreDTO req, LudoMatchState state, Match match) {
        Team team   = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        boolean t1  = team.getId().equals(match.getTeam1().getId());
        if (t1) state.setTeam1Captures(state.getTeam1Captures() + 1);
        else    state.setTeam2Captures(state.getTeam2Captures() + 1);
        return ludoEventInterface.save(build(match, team, plyr, "CAPTURE", state));
    }

    // ── WIN: team declared winner ─────────────────────────────────
    private void handleWin(LudoScoreDTO req, LudoMatchState state, Match match) {
        Team winner = teamInterface.findById(req.getTeamId()).get();
        ludoEventInterface.save(build(match, winner, null, "WIN", state));
        state.setStatus("COMPLETED");
        match.setWinnerTeam(winner);
        matchInterface.save(match);
    }

    // ── END_MATCH: manual end — more home runs wins ───────────────
    private void handleEndMatch(LudoMatchState state, Match match) {
        ludoEventInterface.save(build(match, null, null, "END_MATCH", state));
        state.setStatus("COMPLETED");
        if (state.getTeam1HomeRuns() >= state.getTeam2HomeRuns())
            match.setWinnerTeam(match.getTeam1());
        else
            match.setWinnerTeam(match.getTeam2());
        matchInterface.save(match);
    }

    // ─────────────────────────────────────────
    private LudoScoreDTO undoLast(Long matchId) {
        LudoEvent last = ludoEventInterface.findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (LudoScoreDTO) getCurrentMatchState(matchId);

        LudoMatchState state = ludoStateInterface.findByMatch_Id(matchId).get();
        Match match = last.getMatch();

        switch (last.getEventType().toUpperCase()) {
            case "HOME_RUN" -> {
                boolean t1 = last.getTeam() != null && last.getTeam().getId().equals(match.getTeam1().getId());
                if (t1) state.setTeam1HomeRuns(Math.max(0, state.getTeam1HomeRuns() - 1));
                else    state.setTeam2HomeRuns(Math.max(0, state.getTeam2HomeRuns() - 1));
            }
            case "CAPTURE" -> {
                boolean t1 = last.getTeam() != null && last.getTeam().getId().equals(match.getTeam1().getId());
                if (t1) state.setTeam1Captures(Math.max(0, state.getTeam1Captures() - 1));
                else    state.setTeam2Captures(Math.max(0, state.getTeam2Captures() - 1));
            }
            case "WIN", "END_MATCH" -> {
                state.setStatus("LIVE");
                match.setWinnerTeam(null);
                matchInterface.save(match);
            }
        }

        ludoEventInterface.delete(last);
        ludoStateInterface.save(state);
        if (last.getPlayer() != null && match.getTournament() != null)
            ludoStatsService.recalculatePlayerStats(last.getPlayer().getId(), match.getTournament().getId());
        return toDTO(state, "UNDO");
    }

    // ─────────────────────────────────────────
    private LudoEvent build(Match match, Team team, Player player, String type, LudoMatchState state) {
        LudoEvent ev = new LudoEvent();
        ev.setMatch(match); ev.setTeam(team); ev.setPlayer(player); ev.setEventType(type);
        ev.setEventTimeSeconds(state.getMatchStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getMatchStartTime()) / 1000) : 0);
        return ev;
    }

    private LudoMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        LudoMatchState s = new LudoMatchState();
        s.setMatch(match);
        s.setTeam1HomeRuns(0); s.setTeam2HomeRuns(0);
        s.setTeam1Captures(0); s.setTeam2Captures(0);
        s.setStatus("LIVE"); s.setMatchStartTime(System.currentTimeMillis());
        return ludoStateInterface.save(s);
    }

    private LudoScoreDTO toDTO(LudoMatchState state, String comment) {
        LudoScoreDTO dto = new LudoScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1HomeRuns(state.getTeam1HomeRuns()); dto.setTeam2HomeRuns(state.getTeam2HomeRuns());
        dto.setTeam1Captures(state.getTeam1Captures()); dto.setTeam2Captures(state.getTeam2Captures());
        dto.setStatus(state.getStatus()); dto.setMatchStartTime(state.getMatchStartTime());
        dto.setComment(comment);
        dto.setLudoEvents(ludoEventInterface.findByMatch_IdOrderByIdAsc(state.getMatch().getId())
                .stream().map(this::toEventDTO).collect(Collectors.toList()));
        return dto;
    }

    private LudoEventDTO toEventDTO(LudoEvent ev) {
        LudoEventDTO dto = new LudoEventDTO();
        dto.setId(ev.getId()); dto.setEventType(ev.getEventType());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        if (ev.getPlayer() != null) { dto.setPlayerId(ev.getPlayer().getId()); dto.setPlayerName(ev.getPlayer().getName()); }
        if (ev.getTeam()   != null) { dto.setTeamId(ev.getTeam().getId());     dto.setTeamName(ev.getTeam().getName()); }
        return dto;
    }
}
