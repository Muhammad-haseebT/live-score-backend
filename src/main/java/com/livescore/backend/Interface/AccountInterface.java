package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountInterface extends JpaRepository<Account,Long> {
    boolean existsByUsername(String username);

    Account findByUsername(String username);

    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdIncludingDeleted(@Param("id") Long id);

}
