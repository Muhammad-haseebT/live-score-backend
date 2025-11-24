package com.livescore.backend.Service;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Interface.AccountInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AccountService {
    @Autowired
    AccountInterface ai;

    public ResponseEntity<?> check(Account a) {
        Map<String, Object> response = new HashMap<>();

        if(ai.findByAridAndPassword(a.getArid().toUpperCase(), a.getPassword())==null){
            response.put("success", false);
            response.put("message", "Invalid ARID or Password");
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println(ai.findByAridAndPassword(a.getArid(), a.getPassword()));
        response.put("success", true);
        response.put("message", "Login successful");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> add(Account a) {
        Map<String, Object> response = new HashMap<>();

        if (a.getArid() == null || a.getArid().isEmpty()) {
            response.put("success", false);
            response.put("message", "Arid is required");
            return ResponseEntity.badRequest().body(response);
        } else if (a.getPassword() == null || a.getPassword().isEmpty()) {
            response.put("success", false);
            response.put("message", "Password is required");
            return ResponseEntity.badRequest().body(response);
        } else if (!Pattern.matches("^(?=.*[a-zA-Z0-9])(?=.*[!@#$%^&*])(?=.*[0-9]).{8,}$", a.getPassword())) {
            response.put("success", false);
            response.put("message", "Password must be at least 8 characters long and contain at least one letter, one number, and one special character");
            return ResponseEntity.badRequest().body(response);
        }
        else if (!Pattern.matches("(?i)^\\d{4}-arid-\\d{4}$", a.getArid())) {
            response.put("success", false);
            response.put("message", "Arid must be in this format YYYY-ARID-XXXX");
            return ResponseEntity.badRequest().body(response);
        } else if (ai.findByArid(a.getArid().toUpperCase()) != null) {
            response.put("success", false);
            response.put("message", "Already Exists");
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println(a.getArid().toUpperCase());
        a.setArid(a.getArid().toUpperCase());
        a.setRole("user");
        ai.save(a);

        response.put("success", true);
        response.put("message", "Account registered successfully");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> get() {
        return ResponseEntity.ok(ai.findAll());
    }
}