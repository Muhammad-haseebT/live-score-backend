package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Stats;
import com.livescore.backend.Service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StatsController {
    @Autowired
    private StatsService statsService;

    @GetMapping("/stats")
    public ResponseEntity<?> getAllStats() {
        return statsService.getAllStats();
    }





}
