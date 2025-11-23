package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int oid;

    @ManyToOne
    @JoinColumn(
            name = "Aid",
            foreignKey = @ForeignKey(name = "FK_Org_Acc")
    )
    Account account;

    String name;
}
