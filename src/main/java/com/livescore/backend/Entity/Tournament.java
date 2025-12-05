package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String name;

    LocalDate startdate;
    LocalDate enddate;

    String playerType;
    String tournamentType;
    String tournamentStage;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "organizer_id")
    Account account;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Media> mediaList;

    @ManyToOne
    @JoinColumn(name = "sports_id")
    Sports sports;

    @JsonManagedReference
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Team> teams;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Match> matches;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    List<Stats> statsList;

    @ManyToOne
    @JoinColumn(name = "season_id")
    @JsonBackReference
    Season season;

}
