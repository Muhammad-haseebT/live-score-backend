package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, nullable = false)
    private String username;


    @Column(nullable = false)
    private String password;


    @Column(nullable = false)
    private String role;


    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Season> seasonsCreated;


    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Tournament> tournamentsCreated;


    @OneToMany(mappedBy = "scorer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Match> scoredMatches;
}

