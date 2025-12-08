package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
// Board.java
@Entity
@Data
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Board -> Match
    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-boards")
    private Match match;

    // winner/loser teams (no managed collection on Team side)
    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    @JsonIgnore
    private Team winnerTeam;

    @ManyToOne
    @JoinColumn(name = "loser_team_id")
    @JsonIgnore
    private Team loserTeam;

    private int pts;
}
