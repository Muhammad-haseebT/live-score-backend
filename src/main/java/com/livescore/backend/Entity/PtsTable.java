package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PtsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer ptsid;

    @OneToOne
    @JoinColumn(name = "tmid")
    Team team;

    Integer matches;
    Integer points;
    Integer wins;
    Integer loss;
}
