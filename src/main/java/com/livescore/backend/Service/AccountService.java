package com.livescore.backend.Service;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Interface.AccountInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    @Autowired
    private AccountInterface accountInterface;
    
    public ResponseEntity<?> createAccount(Account account) {
        if(account.getUsername()==null||account.getPassword()==null){
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if(accountInterface.existsByUsername(account.getUsername())){
            return ResponseEntity.badRequest().body("Username already exists");
        }
        //if username is email then role = admin else user
        if(account.getUsername().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")){
            account.setRole("ADMIN");
        }else{
            account.setRole("USER");
        }

        account.setUsername(account.getUsername().toLowerCase());

        return ResponseEntity.ok(accountInterface.save(account));
    }

    public ResponseEntity<?> getAccountById(Long id) {
        if(accountInterface.findById(id).isPresent()){
            return ResponseEntity.ok(accountInterface.findById(id).get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> getAllAccounts() {
        return ResponseEntity.ok(accountInterface.findAll());
    }

    public ResponseEntity<?> updateAccount(Long id, Account account) {
        if(accountInterface.findById(id).isPresent()){
            Account ac=accountInterface.findById(id).get();
            ac.setUsername(account.getUsername().toLowerCase());
            ac.setPassword(account.getPassword());
            ac.setRole(account.getRole().toUpperCase());
            ac.setName(account.getName());
            return ResponseEntity.ok(accountInterface.save(ac));
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> deleteAccount(Long id) {
        if(accountInterface.findById(id).isPresent()){
            accountInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> loginAccount(Account account) {
        if(accountInterface.existsByUsername(account.getUsername())){
            return ResponseEntity.ok(accountInterface.findByUsernameAndPassword(account.getUsername(), account.getPassword()));
        }else{
            return ResponseEntity.notFound().build();
        }
    }
}
