package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Sports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsInterface extends JpaRepository<Sports,Integer>{
}
