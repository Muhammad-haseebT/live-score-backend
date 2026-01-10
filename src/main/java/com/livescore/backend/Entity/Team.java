package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@ToString(exclude = {"players", "playerRequests", "pointsTableEntry"})
@EqualsAndHashCode(exclude = {"players", "playerRequests", "pointsTableEntry"})

public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;


    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonBackReference("tournament-teams")
    private Tournament tournament;


    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    @JsonManagedReference("team-playerTeams")
    private List<PlayerRequest> playerRequests;
    //players
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    @JsonManagedReference("team-players")
    private List<Player> players;


    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL)
    @JsonManagedReference("team-pointsTableEntry")
    private PtsTable pointsTableEntry;

    private String status;
    @PrePersist
    public void prePersist() {
        this.status = "draft";
    }
}
