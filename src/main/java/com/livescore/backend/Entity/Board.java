package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference
    private Match match;


    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;


    @ManyToOne
    @JoinColumn(name = "loser_team_id")
    private Team loserTeam;


    private int pts;
}