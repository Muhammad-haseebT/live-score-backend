package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int pid;

    String name;
    String aridno;
    String role;

    @ManyToOne
    @JoinColumn(name = "tmid")
    Team team;
}
