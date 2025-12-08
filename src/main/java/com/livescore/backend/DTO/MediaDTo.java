package com.livescore.backend.DTO;

import jakarta.persistence.Column;
import lombok.Data;

import java.io.File;

@Data
public class MediaDTo {
    private Long id;
    private Long matchId;
}
