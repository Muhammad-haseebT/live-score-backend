package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Season;
import com.livescore.backend.Service.SportTournamentCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonInterface extends JpaRepository<Season, Long> {

    boolean existsByName(String name);

    @Query("""
            SELECT new com.livescore.backend.Service.SportTournamentCount(
                s.name,
                COUNT(t.id),
                s.id
            )
            FROM Sports s
            LEFT JOIN s.tournaments t
            WHERE t.season.id = :seasonId OR t.id IS NULL
            GROUP BY s.name,s.id
            """)
    List<SportTournamentCount> findSportWiseTournamentCount(@Param("seasonId") Long seasonId);

}
