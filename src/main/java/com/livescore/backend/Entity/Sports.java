package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Sports {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int Sportsid;
    @Column(unique = true)
    String name;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "sid")
    Season season;


    @OneToMany(mappedBy = "sports", cascade = CascadeType.ALL)
    List<Tournament> tournaments;
}