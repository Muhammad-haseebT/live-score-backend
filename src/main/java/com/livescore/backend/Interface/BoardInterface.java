package com.livescore.backend.Interface;


import com.livescore.backend.Entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardInterface extends JpaRepository<Board,Integer> {
}
