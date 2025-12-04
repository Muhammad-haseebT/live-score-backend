package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CricketStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int stid;

    @ManyToOne
    @JoinColumn(name = "mtid")
    Match match;

    int runs;
    int teamid;
    Double nrr;
    String result;
    int points;
    int wickets;
    Double overs;
}
