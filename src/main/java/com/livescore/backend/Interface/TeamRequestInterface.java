package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.TeamRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRequestInterface extends JpaRepository<TeamRequest, Long> {


    boolean existsByTeamAndPlayerAccount(Team team, Account player);
}
