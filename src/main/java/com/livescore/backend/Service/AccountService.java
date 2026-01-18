package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDto;
import com.livescore.backend.DTO.accountDTO;
import com.livescore.backend.Entity.Account;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.PlayerInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private PlayerService playerService;
    @Autowired PlayerInterface playerInterface;


    public ResponseEntity<?> createAccount(Account account) {
        if (account.getUsername() == null || account.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (accountInterface.existsByUsername(account.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        //if username is email then role = admin else user
        if (account.getUsername().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            account.setRole("ADMIN");
        } else {
            account.setRole("USER");
        }
        account.setUsername(account.getUsername().toLowerCase());
        account.setPassword(Base64.getEncoder().encodeToString(account.getPassword().getBytes()));
        Account a=accountInterface.save(account);
        PlayerDto playerDto=new PlayerDto();
        playerDto.setUsername(account.getUsername());
        playerDto.setName(account.getName());
        playerDto.setPlayerRole("player");
        playerService.createPlayer(playerDto);

        return ResponseEntity.ok(a);
    }

    public ResponseEntity<?> getAccountById(Long id) {
        if (accountInterface.findById(id).isPresent()) {
            return ResponseEntity.ok(accountInterface.findById(id).get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> getAllAccounts() {
        return ResponseEntity.ok(accountInterface.findAll());
    }

    public ResponseEntity<?> updateAccount(Long id, Account account) {

        System.out.println(id);
        Optional<Account> optional = accountInterface.findById(id);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Account ac = optional.get();

        if (account.getUsername() != null)
            ac.setUsername(account.getUsername());

        if (account.getName() != null)
            ac.setName(account.getName());

        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            ac.setPassword(
                    Base64.getEncoder().encodeToString(account.getPassword().getBytes())
            );
        }

        if (account.getRole() != null) {
            ac.setRole(account.getRole().toUpperCase());
        }

        accountInterface.save(ac);
        return ResponseEntity.ok(ac);
    }

    public ResponseEntity<?> deleteAccount(Long id) {
        Account a=accountInterface.findById(id).get();
        if (a!=null) {

            accountInterface.delete(a);
//            a.softDelete();
//            accountInterface.save(a);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    public void restoreAccount(Long id) {
        // Deleted account find karne ke liye special query
        Account account = accountInterface.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.restore();
        accountInterface.save(account);
    }

    public ResponseEntity<?> loginAccount(accountDTO account) {
        if (accountInterface.existsByUsername(account.getUsername())) {
            Account ac = accountInterface.findByUsername(account.getUsername());
            if (ac.getPassword().equals(Base64.getEncoder().encodeToString(account.getPassword().getBytes()))) {

                accountDTO accountDTO=new accountDTO();
                accountDTO.setId(ac.getId());
                accountDTO.setName(ac.getName());
                accountDTO.setRole(ac.getRole());
                accountDTO.setPlayerId(playerInterface.findByAccount_Id(ac.getId()).get().getId());
                return ResponseEntity.ok(accountDTO);
            } else {
                System.out.println(account.getUsername() + account.getPassword());
                return ResponseEntity.badRequest().body("Invalid password");
            }

        } else {
            System.out.println(account.getUsername() + account.getPassword());
            return ResponseEntity.notFound().build();
        }
    }


}
