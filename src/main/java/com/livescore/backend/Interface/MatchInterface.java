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
    Object findByTournamentId(@Param("tournamentId") Long tournamentId);




    List<Match> findByStatus(String status);



    Object findByDate(LocalDate date);

    @Query("SELECT m FROM Match m WHERE m.time = :time")
    List<Match> findByTime(@Param("time") LocalTime time);



    @Query("SELECT m FROM Match m WHERE m.team1.id = :team1 OR m.team2.id = :team2")
    Object findByTeam1IdOrTeam2Id(@Param("team1") Long team1, @Param("team2") Long team2);
}
