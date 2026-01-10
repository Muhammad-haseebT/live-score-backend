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
        COUNT(t.id)
    )
    FROM Tournament t
    JOIN t.sport s
    WHERE t.season.id = :seasonId
    GROUP BY s.name
""")
    List<SportTournamentCount> findSportWiseTournamentCount(
            @Param("seasonId") Long seasonId
    );


}
