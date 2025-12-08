package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
// Tournament.java
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

    private String playerType;//male/female
    private String tournamentType;//hard,tennis etc
    private String tournamentStage;//round-robin,knock out

    // Tournament -> Account (organizer) many-to-one
    @ManyToOne
    @JoinColumn(name = "organizer_id")
    @JsonBackReference("account-tournaments")
    private Account organizer;

    // Tournament -> Sports (many-to-one)
    @ManyToOne
    @JoinColumn(name = "sports_id")
    @JsonBackReference("sport-tournaments")
    private Sports sport;

    // Tournament -> Season (many-to-one)
    @ManyToOne
    @JoinColumn(name = "season_id")
    @JsonBackReference("season-tournaments")
    private Season season;

    // Tournament -> Team (one-to-many)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference("tournament-teams")
    private List<Team> teams;

    // Tournament -> Match (one-to-many)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference("tournament-matches")
    private List<Match> matches;

    // Tournament -> Stats (one-to-many)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonManagedReference("tournament-stats")
    private List<Stats> stats;


}
