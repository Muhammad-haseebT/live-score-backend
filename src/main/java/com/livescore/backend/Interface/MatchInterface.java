package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface MatchInterface extends JpaRepository<Match,Long> {

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId")
    List<Object> findByTournamentId(@Param("tournamentId") Long tournamentId);




    List<Match> findByStatus(String status);



    List<Match> findByDate(LocalDate date);

    @Query("SELECT m FROM Match m WHERE m.time = :time")
    List<Match> findByTime(@Param("time") LocalTime time);



    @Query("SELECT m FROM Match m WHERE m.team1.id = :team1 OR m.team2.id = :team2")
    List<Match> findByTeam1IdOrTeam2Id(@Param("team1") Long team1, @Param("team2") Long team2);

    @Query("""
        SELECT m FROM Match m
        WHERE m.tournament.id = :tournamentId
          AND m.status = 'FINISHED'
          AND (m.team1.id = :teamId OR m.team2.id = :teamId)
    """)
    List<Match> findCompletedMatchesByTeam(
            @Param("teamId") Long teamId,
            @Param("tournamentId") Long tournamentId
    );


    @Query("SELECT m.manOfMatch.id, COUNT(m) FROM Match m WHERE m.tournament.id = :tournamentId AND m.manOfMatch IS NOT NULL GROUP BY m.manOfMatch.id ORDER BY COUNT(m) DESC")
    List<Object[]> countPlayerOfMatchByPlayer(@Param("tournamentId") Long tournamentId);

    List<Match> findByTournament_Id(Long tournamentId);


    @Query("SELECT m FROM Match m WHERE m.tournament.sport.name = :name AND m.status = :status")
    List<Match> findByTournament_SportName(@Param("name") String name,@Param("status") String status);

    @Query("SELECT m FROM Match m WHERE m.tournament.sport.name = :name")
    List<Match> findByTournament_SportName(@Param("name") String name);

    @Query("Select m from Match m where m.scorer.id=:id and m.status in ('LIVE','UPCOMING')")
    List<Match> findByScorerID(@Param("id") Long id);
}
