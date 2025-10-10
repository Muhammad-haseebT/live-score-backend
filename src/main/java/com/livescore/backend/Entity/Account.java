package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(unique = true)
    String arid;

    String password;

}
