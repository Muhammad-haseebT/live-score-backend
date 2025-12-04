package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

    LocalDate startdate;
    LocalDate enddate;
    String venue;
    String playerType;
    String tournamentType;
    String tournamentStage;
    String Organizer;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Media> mediaList;

    @ManyToOne
    @JoinColumn(name = "Sportsid")

    Sports sports;
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Team> teams;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Match> matches;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Stats> statsList;
}
