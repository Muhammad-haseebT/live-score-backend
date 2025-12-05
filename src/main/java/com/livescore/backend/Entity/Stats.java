package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Stats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    Double strikeRate;
    int highest;
    int wickets;
    int runs;
    int points;
    int notOut;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    Tournament tournament;
}
