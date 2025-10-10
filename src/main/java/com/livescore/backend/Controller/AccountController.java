package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class AccountController {
    @Autowired
    AccountService as;

    @PostMapping("/acc/login")
    public ResponseEntity<?> getAcc(@RequestBody Account a) {
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


}
