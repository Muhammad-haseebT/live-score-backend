package com.livescore.backend.Interface;

import com.livescore.backend.Entity.CricketBall;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CricketBallInterface extends JpaRepository<CricketBall, Long> {

    // ✅ MISSING METHOD - Add this for ball auto-increment
    @Query("SELECT cb FROM CricketBall cb WHERE cb.innings.id = :inningsId ORDER BY cb.id DESC LIMIT 1")
    CricketBall findLastBallInInnings(@Param("inningsId") Long inningsId);


    CricketBall findFirstByInnings_IdOrderByIdDesc(Long inningsId);

    // ✅ Alternative method using Spring Data naming convention (simpler)
    // Uncomment this if above doesn't work in your Spring version
    // CricketBall findFirstByInnings_IdOrderByIdDesc(Long inningsId);

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

    List<CricketBall> findByMatch_Id(Long matchId);

    List<CricketBall> findByMatch_IdAndInnings_Id(Long matchId, Long id);

    @Query("SELECT SUM(b.runs) FROM CricketBall b WHERE b.match.id = :matchId AND b.batsman.id = :playerId")
    Integer sumRunsByMatchAndBatsman(@Param("matchId") Long matchId, @Param("playerId") Long playerId);

    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.match.id = :matchId AND LOWER(b.dismissalType) IN ('bowled','caught','lbw','stumped','hit-wicket') AND b.bowler.id = :bowlerId AND b.bowler.isDeleted = false")
    Integer countWicketsByMatchAndBowler(@Param("matchId") Long matchId, @Param("bowlerId") Long bowlerId);

    @Query("""
SELECT
  SUM(CASE
    WHEN b.extraType IS NOT NULL AND (LOWER(b.extraType) LIKE '%wide%' OR LOWER(b.extraType) LIKE '%no%') THEN (COALESCE(b.runs,0) + COALESCE(b.extra,0))
    WHEN b.extraType IS NOT NULL AND (LOWER(b.extraType) LIKE '%bye%') THEN COALESCE(b.runs,0)
    ELSE COALESCE(b.runs,0)
  END)
FROM CricketBall b
WHERE b.match.id = :matchId
  AND b.bowler.id = :bowlerId
""")
    Integer sumRunsConcededByMatchAndBowler(@Param("matchId") Long matchId, @Param("bowlerId") Long bowlerId);

    @Query("SELECT b.batsman.id AS playerId, SUM(b.runs) AS runs FROM CricketBall b WHERE b.match.id = :matchId AND b.batsman.isDeleted = false GROUP BY b.batsman.id ORDER BY SUM(b.runs) DESC")
    List<Object[]> aggregateRunsByMatch(@Param("matchId") Long matchId);

    @Query("SELECT b.bowler.id AS playerId, COUNT(b) AS wickets FROM CricketBall b WHERE b.match.id = :matchId AND LOWER(b.dismissalType) IN ('bowled','caught','lbw','stumped','hit-wicket') AND b.bowler.isDeleted = false GROUP BY b.bowler.id ORDER BY COUNT(b) DESC")
    List<Object[]> aggregateWicketsByMatch(@Param("matchId") Long matchId);

    @Query("select b from CricketBall b where b.batsman.id = :playerId and b.match.tournament.id = :tournamentId and b.batsman.isDeleted = false")
    List<CricketBall> findBatsmanBallsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    @Query("select b from CricketBall b where b.bowler.id = :playerId and b.match.tournament.id = :tournamentId and b.bowler.isDeleted = false")
    List<CricketBall> findBowlerBallsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    List<CricketBall> findByBatsmanIdAndMatchId(Long batsmanId, Long matchId);
    List<CricketBall> findByBowlerIdAndMatchId(Long bowlerId, Long matchId);

    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.innings.id = :inningsId AND b.legalDelivery = true")
    long countLegalBallsByInningsId(@Param("inningsId") Long inningsId);

    @Query("""
      SELECT COALESCE(SUM(COALESCE(b.runs,0) + COALESCE(b.extra,0)), 0)
      FROM CricketBall b
      WHERE b.innings.id = :inningsId
    """)
    int sumRunsAndExtrasByInningsId(@Param("inningsId") Long inningsId);

    @Query("""
      SELECT COUNT(b)
      FROM CricketBall b
      WHERE b.innings.id = :inningsId
        AND b.dismissalType IS NOT NULL
        AND LOWER(b.dismissalType) NOT IN ('retired hurt', 'retired')
    """)
    long countWicketsByInningsId(@Param("inningsId") Long inningsId);

    @Query("SELECT cb.batsman.id, SUM(cb.runs) FROM CricketBall cb " +
            "WHERE cb.innings.match.tournament.id = :tournamentId AND cb.batsman.isDeleted = false " +
            "GROUP BY cb.batsman.id ORDER BY SUM(cb.runs) DESC")
    List<Object[]> sumRunsByBatsman(@Param("tournamentId") Long tournamentId, Pageable pageable);

    @Query("SELECT cb.bowler.id, SUM(CASE WHEN cb.dismissalType IS NOT NULL AND LOWER(cb.dismissalType) NOT IN ('runout','retired','retired hurt') THEN 1 ELSE 0 END) " +
            "FROM CricketBall cb " +
            "WHERE cb.innings.match.tournament.id = :tournamentId AND cb.bowler.isDeleted = false " +
            "GROUP BY cb.bowler.id ORDER BY SUM(CASE WHEN cb.dismissalType IS NOT NULL AND LOWER(cb.dismissalType) NOT IN ('runout','retired','retired hurt') THEN 1 ELSE 0 END) DESC")
    List<Object[]> countWicketsByBowlerId(@Param("tournamentId") Long tournamentId, Pageable pageable);

    @Query("SELECT cb FROM CricketBall cb WHERE cb.overNumber = :overNumber AND cb.ballNumber = :ballNumber AND cb.match.id = :matchId AND cb.innings.no = :inningsNo")
    List<CricketBall> findByOverNumberAndBallNumberAndMatch_Id(@Param("overNumber") Integer overNumber, @Param("ballNumber") Integer ballNumber, @Param("matchId") Long matchId, @Param("inningsNo") int inningsNo);

    CricketBall findFirstByMatch_IdAndInnings_NoOrderByIdDesc(Long matchId, int inningsNo);

    @Query("SELECT COUNT(DISTINCT cb.match.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId AND cb.innings.match.tournament.id = :tournamentId")
    int countDistinctMatchesByBatsmanIdAndTournamentId(@Param("playerId") Long playerId, @Param("tournamentId") Long tournamentId);

    @Query("SELECT COUNT(DISTINCT cb.innings.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId AND cb.match.tournament.id = :tournamentId")
    int countDistinctInningsBatted(@Param("playerId") Long playerId, @Param("tournamentId") Long tournamentId);

    @Query("""
SELECT COALESCE(SUM(cb.runs), 0)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.innings.id = :inningsId
""")
    Integer sumBatsmanRunsByInnings(@Param("playerId") Long playerId,
                                    @Param("inningsId") Long inningsId);

    @Query("""
SELECT COUNT(DISTINCT b.match.id)
FROM CricketBall b
WHERE (b.batsman.id = :playerId 
       OR b.bowler.id = :playerId 
       OR b.fielder.id = :playerId)
  AND b.match.tournament.id = :tournamentId
""")
    int countMatchesPlayedInTournament(Long playerId, Long tournamentId);

    // Optimized batting aggregate
    @Query("""
SELECT SUM(cb.runs) as totalRuns,
       SUM(CASE WHEN LOWER(cb.extraType) LIKE '%wide%' THEN 0 ELSE 1 END) as ballsFaced,
       SUM(CASE WHEN cb.isFour = true THEN 1 ELSE 0 END) as fours,
       SUM(CASE WHEN cb.isSix = true THEN 1 ELSE 0 END) as sixes
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.match.tournament.id = :tournamentId
""")
    Object[] getBattingAggregate(@Param("playerId") Long playerId,
                                 @Param("tournamentId") Long tournamentId);

    // Optimized bowling aggregate
    @Query("""
SELECT SUM(cb.runs + CASE WHEN LOWER(cb.extraType) LIKE '%wide%' OR LOWER(cb.extraType) LIKE '%no%' THEN cb.extra ELSE 0 END) as runsConceded,
       SUM(CASE WHEN cb.legalDelivery = true THEN 1 ELSE 0 END) as ballsBowled,
       SUM(CASE WHEN cb.dismissalType IS NOT NULL 
                AND LOWER(cb.dismissalType) IN ('bowled','lbw','stumped','caught','hit wicket','hitwicket')
           THEN 1 ELSE 0 END) as wickets
FROM CricketBall cb
WHERE cb.bowler.id = :playerId
  AND cb.match.tournament.id = :tournamentId
""")
    Object[] getBowlingAggregate(@Param("playerId") Long playerId,
                                 @Param("tournamentId") Long tournamentId);

    @Query("""
SELECT COUNT(DISTINCT cb.innings.id) 
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.match.tournament.id = :tournamentId
  AND cb.innings.id NOT IN (
    SELECT DISTINCT cb2.innings.id
    FROM CricketBall cb2
    WHERE cb2.batsman.id = :playerId
      AND cb2.match.tournament.id = :tournamentId
      AND cb2.dismissalType IS NOT NULL
  )
""")
    Integer countNotOutInnings(@Param("playerId") Long playerId,
                               @Param("tournamentId") Long tournamentId);

    @Query("""
SELECT SUM(cb.runs)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.match.tournament.id = :tournamentId
GROUP BY cb.innings.id
ORDER BY SUM(cb.runs) DESC
""")
    List<Integer> getRunsPerInningsDesc(@Param("playerId") Long playerId,
                                        @Param("tournamentId") Long tournamentId,
                                        Pageable pageable);

    // Overall stats (no tournament filter)
    @Query("""
SELECT SUM(cb.runs) as totalRuns,
       SUM(CASE WHEN LOWER(cb.extraType) LIKE '%wide%' THEN 0 ELSE 1 END) as ballsFaced,
       SUM(CASE WHEN cb.isFour = true THEN 1 ELSE 0 END) as fours,
       SUM(CASE WHEN cb.isSix = true THEN 1 ELSE 0 END) as sixes
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
""")
    Object[] getBattingAggregateOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT SUM(cb.runs + CASE WHEN LOWER(cb.extraType) LIKE '%wide%' OR LOWER(cb.extraType) LIKE '%no%' THEN cb.extra ELSE 0 END) as runsConceded,
       SUM(CASE WHEN cb.legalDelivery = true THEN 1 ELSE 0 END) as ballsBowled,
       SUM(CASE WHEN cb.dismissalType IS NOT NULL
                AND LOWER(cb.dismissalType) IN ('bowled','lbw','stumped','caught','hit wicket','hitwicket')
           THEN 1 ELSE 0 END) as wickets
FROM CricketBall cb
WHERE cb.bowler.id = :playerId
""")
    Object[] getBowlingAggregateOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT COUNT(DISTINCT cb.innings.id)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.innings.id NOT IN (
    SELECT DISTINCT cb2.innings.id
    FROM CricketBall cb2
    WHERE cb2.batsman.id = :playerId
      AND cb2.dismissalType IS NOT NULL
  )
""")
    Integer countNotOutInningsOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT SUM(cb.runs)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
GROUP BY cb.innings.id
ORDER BY SUM(cb.runs) DESC
""")
    List<Integer> getRunsPerInningsDescOverall(@Param("playerId") Long playerId,
                                               Pageable pageable);

    @Query("SELECT COUNT(DISTINCT cb.innings.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId")
    int countDistinctInningsBattedOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT COUNT(DISTINCT b.match.id)
FROM CricketBall b
WHERE (b.batsman.id = :playerId
       OR b.bowler.id = :playerId
       OR b.fielder.id = :playerId)
""")
    int countMatchesPlayedOverall(@Param("playerId") Long playerId);

    // Get all balls for an innings ordered by ID descending (for undo functionality)
    List<CricketBall> findByInnings_IdOrderByIdDesc(Long inningsId);


    List<CricketBall> findByBatsman_IdAndInnings_Id(Long playerId, Long inningsId);

    List<CricketBall> findByBowler_IdAndInnings_Id(Long playerId, Long inningsId);





    List<CricketBall> findByInningsIdOrderByIdDesc(Long inningsId);
}
