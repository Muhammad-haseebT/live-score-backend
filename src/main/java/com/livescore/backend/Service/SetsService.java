package com.livescore.backend.Service;

import com.livescore.backend.Entity.Sets;
import com.livescore.backend.Interface.SetsInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class SetsService {
    @Autowired
    SetsInterface si;
    public List<Sets> get() {
        return  si.findAll();
    }
}
