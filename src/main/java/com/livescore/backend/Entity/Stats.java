package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Stats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer statsId;

    Double strikerate;
    Integer highest;
    Integer wickets;
    Integer runs;
    Integer points;
    Integer notout;

    @ManyToOne
    @JoinColumn(name = "tid")
    Tournament tournament;
}
