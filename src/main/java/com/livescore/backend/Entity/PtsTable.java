package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PtsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;


    @OneToOne
    @JoinColumn(name = "team_id")
    private Team team;


    private int played;
    private int wins;
    private int losses;
    private int draws;
    private int points;
    private double nrr;
}
