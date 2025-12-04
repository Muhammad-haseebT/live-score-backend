package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.GoalsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalsTypeInterface extends JpaRepository<GoalsType,Integer> {
}
