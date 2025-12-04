package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PtsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int ptsid;

    @OneToOne
    @JoinColumn(name = "tmid")
    Team team;

    int matches;
    int points;
    int wins;
    int loss;
}
