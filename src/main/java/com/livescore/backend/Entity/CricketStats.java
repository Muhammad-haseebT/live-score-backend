package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CricketStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer stid;

    @ManyToOne
    @JoinColumn(name = "mtid")
    Match match;

    Integer runs;
    Integer teamid;
    Double nrr;
    String result;
    Integer points;
    Integer wickets;
    Double overs;
}
