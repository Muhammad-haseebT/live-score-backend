package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerDto {
    private Long id;
    private String name;
    private String playerRole;
    private String username;
    private String status;
}
