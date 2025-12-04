package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int tid;

    String name;
    String sport;
    LocalDate startdate;
    LocalDate enddate;
    String venue;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Media> mediaList;

    @ManyToOne
    @JoinColumn(name = "sid")
    Season season;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    private List<Team> teams;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    private List<Match> matches;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    private List<Stats> statsList; // generic Stats table (tournament-level)
}
