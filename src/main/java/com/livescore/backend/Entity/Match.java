package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = {"tournament", "team1", "team2", "scorer", "tossWinner", "winnerTeam",
        "manOfMatch", "bestBatsman", "bestBowler", "cricketInnings",
        "footballEvents", "matchSets", "boards", "mediaList", "cricketBalls"})
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonBackReference("tournament-matches")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "team1_id")
    @JsonIgnore
    private Team team1;

    @ManyToOne
    @JoinColumn(name = "team2_id")
    @JsonIgnore
    private Team team2;

    @ManyToOne
    @JoinColumn(name = "scorer_id")
    @JsonBackReference("account-scoredMatches")
    private Account scorer;

    private String status; // UPCOMING / LIVE / FINISHED
    private String venue;
    private LocalDate date;
    private LocalTime time;

    @PrePersist
    public void prePersist() {
        if (this.status == null || this.status.isBlank()) {
            this.status = "UPCOMING";
        } else {
            this.status = this.status.toUpperCase();
        }
        if (this.decision != null) {
            this.decision = this.decision.toUpperCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.status != null) {
            this.status = this.status.toUpperCase();
        }
        if (this.decision != null) {
            this.decision = this.decision.toUpperCase();
        }
    }

    @ManyToOne
    @JoinColumn(name = "toss_winner_id")
    @JsonIgnore
    private Team tossWinner;

    private int overs;
    private int sets;

    private String decision;

    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    @JsonIgnore
    private Team winnerTeam;

    @ManyToOne
    @JoinColumn(name = "man_of_match_id")
    @JsonIgnore
    private Player manOfMatch;

    @ManyToOne
    @JoinColumn(name = "best_batsman_id")
    @JsonIgnore
    private Player bestBatsman;

    @ManyToOne
    @JoinColumn(name = "best_bowler_id")
    @JsonIgnore
    private Player bestBowler;

    // Match -> CricketInnings
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-innings")
    private List<CricketInnings> cricketInnings = new ArrayList<>();

    // Match -> GoalsType (football events)
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-goals")
    private List<GoalsType> footballEvents = new ArrayList<>();

    // Match -> MatchSets (set-based sports)
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-sets")
    private List<MatchSets> matchSets = new ArrayList<>();

    // Match -> Board (board games)
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-boards")
    private List<Board> boards = new ArrayList<>();

    // Match -> Media
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-media")
    @JsonIgnore
    private List<Media> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-balls")
    @JsonIgnore
    private List<CricketBall> cricketBalls = new ArrayList<>();
}
