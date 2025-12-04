package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;


@RestController
public class AccountController {
    @Autowired
    AccountService as;

    @PostMapping("/acc/login")
    public ResponseEntity<?> login(@RequestBody Account a) {
        return as.check(a);
    }

    @PostMapping("/acc/register")
    public ResponseEntity<?> addAcc(@RequestBody Account a) {
        return as.add(a);
    }

    @GetMapping("/acc/get")
    public ResponseEntity<?> getAcc() {
        return as.get();
    }

    @PutMapping("/acc/update")
    public ResponseEntity<?> updateAcc(@RequestBody Account a) {
        return as.update(a);
    }

    @GetMapping("/acc/getSeasons")
    public ResponseEntity<?> getSeasons(@RequestBody Account a) {
        return as.getSeasons(a);
    }
    


}
