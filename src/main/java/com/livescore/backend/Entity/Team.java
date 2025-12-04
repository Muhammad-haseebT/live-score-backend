package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer tmid;

    String name;
    String logo;

    @ManyToOne
    @JoinColumn(name = "tid")
    Tournament tournament;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    List<Player> players;

    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL)
    PtsTable ptsTable;
}
