package com.livescore.backend.Interface;

import com.livescore.backend.Entity.PtsTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PtsTableInterface extends JpaRepository<PtsTable,Long> {

    boolean existsByTeamIdAndTournamentId(Long id, Long id1);


    @Query("SELECT pt FROM PtsTable pt WHERE pt.tournament.id = :tournamentId AND pt.team.id = :teamId")

    PtsTable findByTournamentIdAndTeamId(@Param("tournamentId") Long tournamentId, @Param("teamId") Long teamId);


    @Query("SELECT pt FROM PtsTable pt WHERE pt.tournament.id = :tournamentId")
    Object findByTournamentId(@Param("tournamentId") Long tournamentId);
}
