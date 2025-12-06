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
    private Long id;


    @Column(nullable = false)
    private String name;


    private LocalDate startDate;
    private LocalDate endDate;


    private String playerType;
    private String tournamentType;
    private String tournamentStage;


    @ManyToOne
    @JoinColumn(name = "organizer_id")
    @JsonBackReference
    private Account organizer;


    @ManyToOne
    @JoinColumn(name = "sports_id")
    private Sports sport;


    @ManyToOne
    @JoinColumn(name = "season_id")
    @JsonBackReference
    private Season season;


    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Team> teams;


    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Match> matches;


    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Stats> stats;


    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Media> media;
}