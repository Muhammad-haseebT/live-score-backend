package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Stats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int statsId;

    Double strikerate;
    int highest;
    int wickets;
    int runs;
    int points;
    int notout;

    @ManyToOne
    @JoinColumn(name = "tid")
    Tournament tournament;
}
