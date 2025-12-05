package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @ManyToOne
    @JoinColumn(name = "match_id")
    Match match;
    String url;
    String filetype;
}
