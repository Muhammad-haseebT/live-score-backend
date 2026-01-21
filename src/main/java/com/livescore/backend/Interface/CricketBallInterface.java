package com.livescore.backend.Interface;

import com.livescore.backend.Entity.CricketBall;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // use property path style - returns balls for a match
    List<CricketBall> findByMatch_Id(Long matchId);

    List<CricketBall> findByMatch_IdAndInnings_Id(Long matchId, Long id);

    @Query("SELECT SUM(b.runs) FROM CricketBall b WHERE b.match.id = :matchId AND b.batsman.id = :playerId")
    Integer sumRunsByMatchAndBatsman(@Param("matchId") Long matchId, @Param("playerId") Long playerId);

    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.match.id = :matchId AND LOWER(b.dismissalType) IN ('bowled','caught','lbw','stumped','hit-wicket') AND b.bowler.id = :bowlerId AND b.bowler.isDeleted = false")
    Integer countWicketsByMatchAndBowler(@Param("matchId") Long matchId, @Param("bowlerId") Long bowlerId);

    @Query("SELECT b.batsman.id AS playerId, SUM(b.runs) AS runs FROM CricketBall b WHERE b.match.id = :matchId AND b.batsman.isDeleted = false GROUP BY b.batsman.id ORDER BY SUM(b.runs) DESC")
    List<Object[]> aggregateRunsByMatch(@Param("matchId") Long matchId);

    @Query("SELECT b.bowler.id AS playerId, COUNT(b) AS wickets FROM CricketBall b WHERE b.match.id = :matchId AND LOWER(b.dismissalType) IN ('bowled','caught','lbw','stumped','hit-wicket') AND b.bowler.isDeleted = false GROUP BY b.bowler.id ORDER BY COUNT(b) DESC")
    List<Object[]> aggregateWicketsByMatch(@Param("matchId") Long matchId);

    // tournament-level helpers
    @Query("select b from CricketBall b where b.batsman.id = :playerId and b.match.tournament.id = :tournamentId and b.batsman.isDeleted = false")
    List<CricketBall> findBatsmanBallsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    @Query("select b from CricketBall b where b.bowler.id = :playerId and b.match.tournament.id = :tournamentId and b.bowler.isDeleted = false")
    List<CricketBall> findBowlerBallsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    List<CricketBall> findByBatsmanIdAndMatchId(Long batsmanId, Long matchId);
    List<CricketBall> findByBowlerIdAndMatchId(Long bowlerId, Long matchId);

    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.innings.id = :inningsId AND b.legalDelivery = true")
    long countLegalBallsByInningsId(@Param("inningsId") Long inningsId);

    @Query("""
      SELECT COUNT(b)
      FROM CricketBall b
      WHERE b.innings.id = :inningsId
        AND b.dismissalType IS NOT NULL
        AND LOWER(b.dismissalType) NOT IN ('retired hurt', 'retired')
    """)
    long countWicketsByInningsId(@Param("inningsId") Long inningsId);

    // tournament-wide: top scorers (paginated)
    @Query("SELECT cb.batsman.id, SUM(cb.runs) FROM CricketBall cb " +
            "WHERE cb.innings.match.tournament.id = :tournamentId AND cb.batsman.isDeleted = false " +
            "GROUP BY cb.batsman.id ORDER BY SUM(cb.runs) DESC")
    List<Object[]> sumRunsByBatsman(@Param("tournamentId") Long tournamentId, Pageable pageable);

    /**
     * Count wickets credited to bowlers across a tournament.
     * Excludes runouts/retired variants.
     * Returns [bowlerId, wicketsCount]
     */
    @Query("SELECT cb.bowler.id, SUM(CASE WHEN cb.dismissalType IS NOT NULL AND LOWER(cb.dismissalType) NOT IN ('runout','retired','retired hurt') THEN 1 ELSE 0 END) " +
            "FROM CricketBall cb " +
            "WHERE cb.innings.match.tournament.id = :tournamentId AND cb.bowler.isDeleted = false " +
            "GROUP BY cb.bowler.id ORDER BY SUM(CASE WHEN cb.dismissalType IS NOT NULL AND LOWER(cb.dismissalType) NOT IN ('runout','retired','retired hurt') THEN 1 ELSE 0 END) DESC")
    List<Object[]> countWicketsByBowlerId(@Param("tournamentId") Long tournamentId, Pageable pageable);

    @Query("SELECT cb FROM CricketBall cb WHERE cb.overNumber = :overNumber AND cb.ballNumber = :ballNumber AND cb.match.id = :matchId AND cb.innings.no = :inningsNo")
    List<CricketBall> findByOverNumberAndBallNumberAndMatch_Id(@Param("overNumber") Integer overNumber, @Param("ballNumber") Integer ballNumber, @Param("matchId") Long matchId, @Param("inningsNo") int inningsNo);
//select top 1 from cricket ball where match id and innings no order by id desc
    @Query("SELECT cb FROM CricketBall cb WHERE cb.match.id = :matchId AND cb.innings.no = :inningsNo ORDER BY cb.id Desc LIMIT 1")
    CricketBall findFirstbyMatch_IdAndInnings_no(@Param("matchId") Long matchId, @Param("inningsNo") int inningsNo);

    @Query("SELECT COUNT(DISTINCT cb.match.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId AND cb.innings.match.tournament.id = :tournamentId")
    int countDistinctMatchesByBatsmanIdAndTournamentId(Long playerId, Long tournamentId);


    @Query("SELECT COUNT(DISTINCT cb.match.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId AND cb.match.id = :matchId")
    int countDistinctMatchesByBatsmanIdAndMatchId(Long playerId, Long matchId);
}
