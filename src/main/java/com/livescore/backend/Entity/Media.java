package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int mid;
    @ManyToOne
    @JoinColumn(name = "parentid")
    Tournament tournament;
    @ManyToOne
    @JoinColumn(name = "childid")
    Match match;

    String url;
    String filetype;
}
