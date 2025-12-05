package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PtsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @OneToOne
    @JoinColumn(name = "team_id")
    Team team;

    int matches;
    int points;
    int wins;
    int loss;
    double nrr;
}
