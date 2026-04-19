package com.livescore.backend.Service;

import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO.AwardDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO.PlayerStatsRow;
import com.livescore.backend.Entity.Award;
import com.livescore.backend.Entity.Stats;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.AwardInterface;
import com.livescore.backend.Interface.StatsInterface;
import com.livescore.backend.Interface.TournamentInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AwardService {

    private final AwardInterface      awardInterface;
    private final StatsInterface      statsInterface;
    private final TournamentInterface tournamentInterface;
    private final StatsService        statsService;

    public TournamentAwardsDTO getTournamentStats(Long tournamentId) {
        Tournament tournament = tournamentInterface.findById(tournamentId).orElse(null);
        if (tournament == null) return null;

        TournamentAwardsDTO dto = new TournamentAwardsDTO();
        dto.setTournamentId(tournamentId);
        dto.setTournamentName(tournament.getName());

        String sport = tournament.getSport() != null
                ? tournament.getSport().getName().toLowerCase() : "cricket";
        dto.setSport(sport);

        List<Award> awards = awardInterface.findByTournamentId(tournamentId);
        dto.setAllAwards(awards.stream()
                .filter(a -> "PLAYER_OF_MATCH".equals(a.getAwardType()))
                .map(this::toAwardDTO).collect(Collectors.toList()));

        awards.stream().filter(a -> "MAN_OF_TOURNAMENT".equals(a.getAwardType()))
                .findFirst().ifPresent(a -> dto.setManOfTournament(toAwardDTO(a)));

        List<Stats> allStats = statsInterface.findAllByTournamentId(tournamentId);

        switch (sport) {
            case "futsal"     -> buildFutsalStats(dto, awards, allStats);
            case "volleyball" -> buildVolleyballStats(dto, awards, allStats);
            case "badminton"  -> buildBadmintonStats(dto, awards, allStats);
            default           -> buildCricketStats(dto, awards, allStats);
        }

        return dto;
    }

    // ── Cricket ──────────────────────────────────────────────────

    private void buildCricketStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a -> "BEST_BATSMAN".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setBestBatsman(toAwardDTO(a)));
        awards.stream().filter(a -> "BEST_BOWLER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setBestBowler(toAwardDTO(a)));
        awards.stream().filter(a -> "BEST_FIELDER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setBestFielder(toAwardDTO(a)));
        awards.stream().filter(a -> "MOST_SIXES".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setMostSixes(toAwardDTO(a)));
        dto.setTopRunScorers(s.stream().filter(x -> x.getRuns() != null && x.getRuns() > 0)
                .sorted(Comparator.comparingInt(Stats::getRuns).reversed()).limit(5)
                .map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopBowlers(s.stream().filter(x -> x.getBallsBowled() != null && x.getBallsBowled() > 0)
                .sorted(Comparator.comparingInt(Stats::getWickets).reversed()
                        .thenComparingDouble(x -> x.getEconomy() != null ? x.getEconomy() : 999))
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

    // ── Futsal ───────────────────────────────────────────────────

    private void buildFutsalStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a -> "TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a -> "TOP_ASSIST".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(s.stream().filter(x -> x.getGoals() != null && x.getGoals() > 0)
                .sorted(Comparator.comparingInt(Stats::getGoals).reversed()
                        .thenComparingInt(x -> x.getYellowCards() != null ? x.getYellowCards() : 0)
                        .thenComparingInt(x -> x.getRedCards() != null ? x.getRedCards() : 0))
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopAssisters(s.stream().filter(x -> x.getAssists() != null && x.getAssists() > 0)
                .sorted(Comparator.comparingInt(Stats::getAssists).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

    // ── Volleyball (goals=pts, assists=aces, fouls=blocks) ───────

    private void buildVolleyballStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a -> "TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a -> "BEST_SERVER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(s.stream().filter(x -> x.getGoals() != null && x.getGoals() > 0)
                .sorted(Comparator.comparingInt(Stats::getGoals).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopAssisters(s.stream().filter(x -> x.getAssists() != null && x.getAssists() > 0)
                .sorted(Comparator.comparingInt(Stats::getAssists).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

    // ── Badminton (goals=points, assists=smashes+aces, fouls=faults) ─

    private void buildBadmintonStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a -> "TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a -> "TOP_ATTACKER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(s.stream().filter(x -> x.getGoals() != null && x.getGoals() > 0)
                .sorted(Comparator.comparingInt(Stats::getGoals).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopAssisters(s.stream().filter(x -> x.getAssists() != null && x.getAssists() > 0)
                .sorted(Comparator.comparingInt(Stats::getAssists).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

    // ── Common ───────────────────────────────────────────────────

    public TournamentAwardsDTO recalculateAndGetStats(Long tournamentId) {
        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) return null;
        statsService.checkAndHandleTournamentEnd(t);
        return getTournamentStats(tournamentId);
    }

    private AwardDTO toAwardDTO(Award award) {
        AwardDTO dto = new AwardDTO();
        dto.setPlayerId(award.getPlayer().getId());
        dto.setPlayerName(award.getPlayer().getName());
        dto.setAwardType(award.getAwardType());
        dto.setPoints(award.getPointsEarned() != null ? award.getPointsEarned() : 0);
        dto.setReason(award.getReason());
        return dto;
    }

    private PlayerStatsRow toPlayerStatsRow(Stats s) {
        PlayerStatsRow r = new PlayerStatsRow();
        r.setPlayerId(s.getPlayer().getId()); r.setPlayerName(s.getPlayer().getName());
        r.setRuns(safe(s.getRuns())); r.setWickets(safe(s.getWickets()));
        r.setBallsFaced(safe(s.getBallsFaced())); r.setBallsBowled(safe(s.getBallsBowled()));
        r.setFours(safe(s.getFours())); r.setSixes(safe(s.getSixes()));
        r.setHighest(safe(s.getHighest())); r.setStrikeRate(s.getStrikeRate());
        r.setCatches(safe(s.getCatches())); r.setRunouts(safe(s.getRunouts()));
        r.setStumpings(safe(s.getStumpings())); r.setFifties(safe(s.getFifties()));
        r.setHundreds(safe(s.getHundreds())); r.setMaidens(safe(s.getMaidens()));
        r.setDotBalls(safe(s.getDotBalls())); r.setRunsConceded(safe(s.getRunsConceded()));
        r.setEconomy(s.getEconomy());
        r.setGoals(safe(s.getGoals())); r.setAssists(safe(s.getAssists()));
        r.setFutsalFouls(safe(s.getFouls())); r.setYellowCards(safe(s.getYellowCards()));
        r.setRedCards(safe(s.getRedCards()));
        r.setPlayerOfMatchCount(safe(s.getPlayerOfMatchCount()));
        r.setTotalPoints(safe(s.getPoints()));
        return r;
    }

    private int safe(Integer v) { return v != null ? v : 0; }
}