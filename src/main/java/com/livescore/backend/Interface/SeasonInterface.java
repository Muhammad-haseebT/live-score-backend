package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonInterface extends JpaRepository<Season, Long> {

    boolean existsByName(String name);
}
