package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

// Account.java
@Entity
@Data
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
    @JsonManagedReference("account-seasons")
    private List<Season> seasonsCreated;

    // Account -> Tournament (one-to-many)
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL)
    @JsonManagedReference("account-tournaments")
    private List<Tournament> tournamentsCreated;

    // Account -> Match (as scorer) (one-to-many)
    @OneToMany(mappedBy = "scorer", cascade = CascadeType.ALL)
    @JsonManagedReference("account-scoredMatches")
    private List<Match> scoredMatches;
}
