package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.beans.Encoder;
import java.util.Base64;
import java.util.List;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {
        "seasonsCreated",
        "tournamentsCreated",
        "scoredMatches",
        "player"
})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    // Account -> Season (one-to-many)
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Season> seasonsCreated;

    // Account -> Tournament (one-to-many)
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Tournament> tournamentsCreated;

    // Account -> Match (as scorer) (one-to-many)
    @OneToMany(mappedBy = "scorer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Match> scoredMatches;
}
