package com.livescore.backend.Entity;

import jakarta.persistence.*;

import java.util.List;

public class MatchSets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)   // FK
    private Match match;

    private int setNumber;

    @ManyToOne
    @JoinColumn(name = "winner_team_id", nullable = false)   // FK
    private Team winnerTeam;

    private int team1Score;
    private int team2Score;

    @OneToMany(mappedBy = "matchSet")
    private List<SetPoint> points;
}
