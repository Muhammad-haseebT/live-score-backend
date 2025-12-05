package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String name;
    String logo;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "tournament_id")
    Tournament tournament;

    @JsonManagedReference
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    List<Player> players;

    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL)
    PtsTable ptsTable;
}
