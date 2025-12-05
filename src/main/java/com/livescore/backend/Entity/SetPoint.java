package com.livescore.backend.Entity;

import jakarta.persistence.*;

public class SetPoint {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "match_set_id", nullable = false)   // FK
    private MatchSets matchSets;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)         // FK
    private Team team;

    private String pointType;
}
