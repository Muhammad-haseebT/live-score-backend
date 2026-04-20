// ══ LudoStatsService.java ════════════════════════════════════════
// Stats mapping: goals=homeRuns, assists=captures
package com.livescore.backend.Sport.Ludo;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Ludo.LudoEvent;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LudoStatsService {

    private final StatsInterface      statsInterface;
    private final PlayerInterface     playerInterface;
    private final AwardInterface      awardInterface;
    private final MatchInterface      matchInterface;
    private final LudoEventInterface  ludoEventInterface;

    @Transactional
    public void onEventSaved(LudoEvent event) {
        if (event == null || event.getMatch() == null
                || event.getMatch().getTournament() == null || event.getPlayer() == null) return;
        Long tid = event.getMatch().getTournament().getId();
        ensureStats(event.getPlayer().getId(), tid, event.getMatch().getTournament());
        recalculatePlayerStats(event.getPlayer().getId(), tid);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    public void onMatchEnd(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;
        Long tid = match.getTournament().getId();

        Set<Long> pids = ludoEventInterface.findByMatch_IdOrderByIdAsc(matchId)
                .stream().filter(e -> e.getPlayer() != null)
                .map(e -> e.getPlayer().getId()).collect(Collectors.toSet());

        for (Long pid : pids) {
            ensureStats(pid, tid, match.getTournament());
            recalculatePlayerStats(pid, tid);
        }
        calculatePOM(matchId, match);
    }

    @Transactional
    public void recalculatePlayerStats(Long playerId, Long tournamentId) {
        Stats stats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).orElse(null);
        if (stats == null) return;

        List<LudoEvent> events = ludoEventInterface.findByPlayerIdAndTournamentId(playerId, tournamentId);
        int homeRuns = 0, captures = 0;

        for (LudoEvent ev : events) {
            if (ev.getPlayer() == null || !ev.getPlayer().getId().equals(playerId)) continue;
            switch (ev.getEventType().toUpperCase()) {
                case "HOME_RUN" -> homeRuns++;
                case "CAPTURE"  -> captures++;
            }
        }

        stats.setGoals(homeRuns);
        stats.setAssists(captures);
        stats.setFouls(0); stats.setYellowCards(0); stats.setRedCards(0);

        int pom = awardInterface.countPomByPlayerIdAndSport(playerId, "ludo");
        stats.setPlayerOfMatchCount(pom);
        stats.setPoints((homeRuns * 3) + (captures * 2) + (pom * 25));
        statsInterface.save(stats);
    }

    private void calculatePOM(Long matchId, Match match) {
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;
        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<Long, Player>  playerMap = new HashMap<>();
        ludoEventInterface.findByMatch_IdOrderByIdAsc(matchId).forEach(ev -> {
            if (ev.getPlayer() == null) return;
            Long pid = ev.getPlayer().getId();
            playerMap.put(pid, ev.getPlayer());
            int pts = switch (ev.getEventType().toUpperCase()) {
                case "HOME_RUN" -> 3;
                case "CAPTURE"  -> 2;
                default         -> 0;
            };
            scoreMap.merge(pid, pts, Integer::sum);
        });
        if (scoreMap.isEmpty()) return;
        Long bestPid = scoreMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (bestPid == null) return;
        Award a = new Award();
        a.setMatch(match); a.setTournament(match.getTournament());
        a.setPlayer(playerMap.get(bestPid)); a.setAwardType("PLAYER_OF_MATCH");
        a.setPointsEarned(scoreMap.get(bestPid));
        a.setReason("Best Ludo performance: " + scoreMap.get(bestPid) + " pts");
        awardInterface.save(a);
        match.setManOfMatch(playerMap.get(bestPid));
        matchInterface.save(match);
    }

    private void ensureStats(Long pid, Long tid, Tournament t) {
        if (statsInterface.findByPlayerIdAndTournamentId(pid, tid).isPresent()) return;
        Player p = playerInterface.findActiveById(pid).orElse(null);
        if (p == null) return;
        Stats s = new Stats();
        s.setPlayer(p); s.setTournament(t); s.setSportType(t.getSport());
        s.setGoals(0); s.setAssists(0); s.setFouls(0); s.setYellowCards(0); s.setRedCards(0); s.setPoints(0);
        s.setRuns(0); s.setWickets(0); s.setBallsFaced(0); s.setBallsBowled(0); s.setRunsConceded(0);
        s.setFours(0); s.setSixes(0); s.setFifties(0); s.setHundreds(0); s.setHighest(0);
        s.setNotOut(0); s.setInningsPlayed(0); s.setMaidens(0); s.setThreeWicketHauls(0);
        s.setFiveWicketHauls(0); s.setDotBalls(0); s.setCatches(0); s.setRunouts(0); s.setStumpings(0);
        s.setPlayerOfMatchCount(0); s.setStrikeRate(0);
        s.setBattingAverage(0.0); s.setEconomy(0.0); s.setBowlingAverage(0.0); s.setBowlingStrikeRate(0.0);
        statsInterface.save(s);
    }
}
