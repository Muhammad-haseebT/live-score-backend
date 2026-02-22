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

        // fetch all awards for this tournament
        List<Award> awards = awardInterface.findByTournamentId(tournamentId);

        List<AwardDTO> allAwardDtos = awards.stream()
                .map(this::toAwardDTO)
                .collect(Collectors.toList());
        dto.setAllAwards(allAwardDtos);

        // set specific awards
        awards.stream()
                .filter(a -> "MAN_OF_TOURNAMENT".equals(a.getAwardType()))
                .findFirst()
                .ifPresent(a -> dto.setManOfTournament(toAwardDTO(a)));

        awards.stream()
                .filter(a -> "BEST_BATSMAN".equals(a.getAwardType()))
                .findFirst()
                .ifPresent(a -> dto.setBestBatsman(toAwardDTO(a)));

        awards.stream()
                .filter(a -> "BEST_BOWLER".equals(a.getAwardType()))
                .findFirst()
                .ifPresent(a -> dto.setBestBowler(toAwardDTO(a)));

        awards.stream()
                .filter(a -> "BEST_FIELDER".equals(a.getAwardType()))
                .findFirst()
                .ifPresent(a -> dto.setBestFielder(toAwardDTO(a)));

        awards.stream()
                .filter(a -> "MOST_SIXES".equals(a.getAwardType()))
                .findFirst()
                .ifPresent(a -> dto.setMostSixes(toAwardDTO(a)));

        // fetch all player stats for this tournament
        List<Stats> allStats = statsInterface.findByTournamentId(tournamentId);

        // top 10 run scorers
        List<PlayerStatsRow> topBatsmen = allStats.stream()
                .filter(s -> s.getRuns() != null && s.getRuns() > 0)
                .sorted(Comparator.comparingInt(Stats::getRuns).reversed())
                .limit(10)
                .map(this::toPlayerStatsRow)
                .collect(Collectors.toList());
        dto.setTopRunScorers(topBatsmen);

        // top 10 wicket takers
        List<PlayerStatsRow> topBowlers = allStats.stream()
                .filter(s -> s.getWickets() != null && s.getWickets() > 0)
                .sorted(Comparator.comparingInt(Stats::getWickets).reversed())
                .limit(10)
                .map(this::toPlayerStatsRow)
                .collect(Collectors.toList());
        dto.setTopWicketTakers(topBowlers);

        return dto;
    }

    /**
     * Call this when tournament ends to generate all awards.
     */
    public TournamentAwardsDTO endTournamentAndGenerateAwards(Long tournamentId) {
        statsService.calculateEndOfTournamentAwards(tournamentId);
        return getTournamentStats(tournamentId);
    }

    private AwardDTO toAwardDTO(Award award) {
        AwardDTO dto = new AwardDTO();
        dto.setPlayerId(award.getPlayer().getId());
        dto.setPlayerName(award.getPlayer().getName());
        dto.setAwardType(award.getAwardType());
        dto.setPoints(award.getPointsEarned());
        dto.setReason(award.getReason());
        return dto;
    }

    private PlayerStatsRow toPlayerStatsRow(Stats s) {
        PlayerStatsRow row = new PlayerStatsRow();
        row.setPlayerId(s.getPlayer().getId());
        row.setPlayerName(s.getPlayer().getName());
        row.setRuns(s.getRuns());
        row.setWickets(s.getWickets());
        row.setBallsFaced(s.getBallsFaced());
        row.setBallsBowled(s.getBallsBowled());
        row.setFours(s.getFours());
        row.setSixes(s.getSixes());
        row.setHighest(s.getHighest());
        row.setStrikeRate(s.getStrikeRate());
        row.setCatches(s.getCatches());
        row.setRunouts(s.getRunouts());
        row.setStumpings(s.getStumpings());
        row.setFifties(s.getFifties());
        row.setHundreds(s.getHundreds());
        row.setMaidens(s.getMaidens());
        row.setDotBalls(s.getDotBalls());
        row.setPlayerOfMatchCount(s.getPlayerOfMatchCount());
        row.setTotalPoints(s.getPoints());
        return row;
    }
}