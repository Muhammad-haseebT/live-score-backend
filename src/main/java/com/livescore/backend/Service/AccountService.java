package com.livescore.backend.Service;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Interface.AccountInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AccountService {
    @Autowired
    AccountInterface ai;

    public ResponseEntity<?> check(Account a) {

        if(ai.findByAridAndPassword(a.getArid().toUpperCase(), a.getPassword())==null){
            return ResponseEntity.badRequest().body(Map.of("error", "Not Found"));
        }

        System.out.println(ai.findByAridAndPassword(a.getArid(), a.getPassword()));
        return ResponseEntity.ok("Found");

    }

    public ResponseEntity<?> add(Account a) {


        if (a.getArid() == null || a.getArid().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arid is required"));
        } else if (a.getPassword() == null || a.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        } else if (!Pattern.matches("^(?=.*[a-zA-Z0-9])(?=.*[!@#$%^&*])(?=.*[0-9]).{8,}$", a.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter, one number, and one special character"));
        }

        else if (!Pattern.matches("(?i)^\\d{4}-arid-\\d{4}$", a.getArid())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arid must be in this format YYYY-ARID-XXXX"));
        } else if (ai.findByArid(a.getArid().toUpperCase()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already Exists"));
        }
        System.out.println(a.getArid().toUpperCase());
        ai.save(a);
        return ResponseEntity.ok("Added");
    }

    public ResponseEntity<?> get() {
        return ResponseEntity.ok(ai.findAll());
    }
}
