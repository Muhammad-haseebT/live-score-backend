package com.livescore.backend.Interface;

import com.livescore.backend.Entity.CricketBall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository

public interface CricketBallInterface extends JpaRepository<CricketBall, Long> {

    @Query("""
        SELECT cb FROM CricketBall cb
        WHERE cb.batsman.id = :playerId
          AND cb.innings.match.tournament.id = :tournamentId
    """)
    List<CricketBall> findBatsmanBalls(@Param("tournamentId") Long tournamentId,
                                       @Param("playerId") Long playerId);

    @Query("""
        SELECT cb FROM CricketBall cb
        WHERE cb.bowler.id = :playerId
          AND cb.innings.match.tournament.id = :tournamentId
    """)
    List<CricketBall> findBowlerBalls(@Param("tournamentId") Long tournamentId,
                                      @Param("playerId") Long playerId);

    @Query("""
        SELECT cb FROM CricketBall cb
        WHERE cb.fielder.id = :playerId
          AND cb.innings.match.tournament.id = :tournamentId
    """)
    List<CricketBall> findFielderBalls(@Param("tournamentId") Long tournamentId,
                                       @Param("playerId") Long playerId);

    List<CricketBall> findByMatchId(Long matchId);

    List<CricketBall> findByMatch_IdAndInnings_Id(Long matchId, Long id);
    // in CricketBallInterface
    @Query("SELECT SUM(b.runs) FROM CricketBall b WHERE b.match.id = :matchId AND b.batsman.id = :playerId")
    Integer sumRunsByMatchAndBatsman(@Param("matchId") Long matchId, @Param("playerId") Long playerId);

    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.match.id = :matchId AND b.dismissalType IN ('bowled','caught','lbw','stumped','hit-wicket') AND b.bowler.id = :bowlerId")
    Integer countWicketsByMatchAndBowler(@Param("matchId") Long matchId, @Param("bowlerId") Long bowlerId);

    @Query("SELECT b.batsman.id AS playerId, SUM(b.runs) AS runs FROM CricketBall b WHERE b.match.id = :matchId GROUP BY b.batsman.id ORDER BY SUM(b.runs) DESC")
    List<Object[]> aggregateRunsByMatch(@Param("matchId") Long matchId);

    @Query("SELECT b.bowler.id AS playerId, COUNT(b) AS wickets FROM CricketBall b WHERE b.match.id = :matchId AND b.dismissalType IN ('bowled','caught','lbw','stumped','hit-wicket') GROUP BY b.bowler.id ORDER BY COUNT(b) DESC")
    List<Object[]> aggregateWicketsByMatch(@Param("matchId") Long matchId);

    List<CricketBall> findByMatch_Id(Long matchId);


    @Query("select b from CricketBall b where b.batsman.id = :playerId and b.match.tournament.id = :tournamentId")
    List<CricketBall> findBatsmanBallsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    @Query("select b from CricketBall b where b.bowler.id = :playerId and b.match.tournament.id = :tournamentId")
    List<CricketBall> findBowlerBallsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    List<CricketBall> findByBatsmanIdAndMatchId(Long batsmanId, Long matchId);
    List<CricketBall> findByBowlerIdAndMatchId(Long bowlerId, Long matchId);

    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.innings.id = :inningsId AND b.legalDelivery = true")
    long countLegalBallsByInningsId(@Param("inningsId") Long inningsId);


}

