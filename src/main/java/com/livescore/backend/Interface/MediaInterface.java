package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaInterface extends JpaRepository<Media,Long> {

    @Query("SELECT m FROM Media m WHERE m.match.id = :matchId")
    List<Media> findByMatchId(@Param("matchId") Long ID);
    @Query("""
    SELECT m
    FROM Media m
    JOIN m.match ma
    JOIN ma.tournament t
    JOIN t.season s
    WHERE s.id = :seasonId
""")
    List<Media> findMediaBySeasonId(@Param("seasonId") Long seasonId);

    @Query("SELECT m FROM Media m JOIN m.match ma WHERE ma.tournament.id = :id")
    List<Media> findMediaByTournamentId(Long id);
}
