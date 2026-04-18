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

    private final AwardInterface awardInterface;
    private final StatsInterface statsInterface;
    private final TournamentInterface tournamentInterface;
    private final StatsService statsService;

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
            default           -> buildCricketStats(dto, awards, allStats);
        }

        return dto;
    }

    private void buildCricketStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> allStats) {
        awards.stream().filter(a -> "BEST_BATSMAN".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setBestBatsman(toAwardDTO(a)));
        awards.stream().filter(a -> "BEST_BOWLER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setBestBowler(toAwardDTO(a)));
        awards.stream().filter(a -> "BEST_FIELDER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setBestFielder(toAwardDTO(a)));
        awards.stream().filter(a -> "MOST_SIXES".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setMostSixes(toAwardDTO(a)));
        dto.setTopRunScorers(allStats.stream().filter(s -> s.getRuns() != null && s.getRuns() > 0)
                .sorted(Comparator.comparingInt(Stats::getRuns).reversed()).limit(5)
                .map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopBowlers(allStats.stream().filter(s -> s.getBallsBowled() != null && s.getBallsBowled() > 0)
                .sorted(Comparator.comparingInt(Stats::getWickets).reversed()
                        .thenComparingDouble(s -> s.getEconomy() != null ? s.getEconomy() : 999))
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

    private void buildFutsalStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> allStats) {
        awards.stream().filter(a -> "TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a -> "TOP_ASSIST".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(allStats.stream().filter(s -> s.getGoals() != null && s.getGoals() > 0)
                .sorted(Comparator.comparingInt(Stats::getGoals).reversed()
                        .thenComparingInt(s -> s.getYellowCards() != null ? s.getYellowCards() : 0)
                        .thenComparingInt(s -> s.getRedCards() != null ? s.getRedCards() : 0))
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopAssisters(allStats.stream().filter(s -> s.getAssists() != null && s.getAssists() > 0)
                .sorted(Comparator.comparingInt(Stats::getAssists).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

    // Volleyball: goals=points, assists=aces, fouls=blocks
    private void buildVolleyballStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> allStats) {
        awards.stream().filter(a -> "TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a -> "BEST_SERVER".equals(a.getAwardType())).findFirst().ifPresent(a -> dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(allStats.stream().filter(s -> s.getGoals() != null && s.getGoals() > 0)
                .sorted(Comparator.comparingInt(Stats::getGoals).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
        dto.setTopAssisters(allStats.stream().filter(s -> s.getAssists() != null && s.getAssists() > 0)
                .sorted(Comparator.comparingInt(Stats::getAssists).reversed())
                .limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList()));
    }

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
        PlayerStatsRow row = new PlayerStatsRow();
        row.setPlayerId(s.getPlayer().getId());
        row.setPlayerName(s.getPlayer().getName());
        row.setRuns(safe(s.getRuns())); row.setWickets(safe(s.getWickets()));
        row.setBallsFaced(safe(s.getBallsFaced())); row.setBallsBowled(safe(s.getBallsBowled()));
        row.setFours(safe(s.getFours())); row.setSixes(safe(s.getSixes()));
        row.setHighest(safe(s.getHighest())); row.setStrikeRate(s.getStrikeRate());
        row.setCatches(safe(s.getCatches())); row.setRunouts(safe(s.getRunouts()));
        row.setStumpings(safe(s.getStumpings())); row.setFifties(safe(s.getFifties()));
        row.setHundreds(safe(s.getHundreds())); row.setMaidens(safe(s.getMaidens()));
        row.setDotBalls(safe(s.getDotBalls())); row.setRunsConceded(safe(s.getRunsConceded()));
        row.setEconomy(s.getEconomy());
        row.setGoals(safe(s.getGoals())); row.setAssists(safe(s.getAssists()));
        row.setFutsalFouls(safe(s.getFouls())); row.setYellowCards(safe(s.getYellowCards()));
        row.setRedCards(safe(s.getRedCards()));
        row.setPlayerOfMatchCount(safe(s.getPlayerOfMatchCount()));
        row.setTotalPoints(safe(s.getPoints()));
        return row;
    }

    private int safe(Integer v) { return v != null ? v : 0; }
}