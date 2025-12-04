package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Sets;
import com.livescore.backend.Service.SetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sets")
public class SetsController {
    @Autowired
    SetsService ss;

    @GetMapping
    public List<Sets> getAllSets() {
        return ss.get();
    }
}
