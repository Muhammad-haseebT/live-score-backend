package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDto;
import com.livescore.backend.DTO.accountDTO;
import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Player;
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
        if (account == null) {
            return ResponseEntity.badRequest().body("Account details are required");
        }
        if (account.getUsername() == null || account.getUsername().isBlank() || account.getPassword() == null || account.getPassword().isBlank()) {
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
        return accountInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getAllAccounts() {
        return ResponseEntity.ok(accountInterface.findAll());
    }

    public ResponseEntity<?> updateAccount(Long id, Account account) {

        System.out.println(id);
        if (account == null) {
            return ResponseEntity.badRequest().body("Account details are required");
        }
        Optional<Account> optional = accountInterface.findById(id);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Account ac = optional.get();

        if (account.getUsername() != null && !account.getUsername().isBlank()) {
            ac.setUsername(account.getUsername());
        }

        if (account.getName() != null)
            ac.setName(account.getName());

        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            ac.setPassword(
                    Base64.getEncoder().encodeToString(account.getPassword().getBytes())
            );
        }

        if (account.getRole() != null && !account.getRole().isBlank()) {
            ac.setRole(account.getRole().toUpperCase());
        }

        accountInterface.save(ac);
        return ResponseEntity.ok(ac);
    }

    public ResponseEntity<?> deleteAccount(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("Account id is required");
        }
        Optional<Account> opt = accountInterface.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        accountInterface.delete(opt.get());
        return ResponseEntity.ok().build();
    }
    public void restoreAccount(Long id) {
        // Deleted account find karne ke liye special query
        if (id == null) {
            return;
        }
        Account account = accountInterface.findByIdIncludingDeleted(id).orElse(null);
        if (account == null) {
            return;
        }

        account.restore();
        accountInterface.save(account);
    }

    public ResponseEntity<?> loginAccount(accountDTO account) {
        if (account == null || account.getUsername() == null || account.getUsername().isBlank() || account.getPassword() == null || account.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (accountInterface.existsByUsername(account.getUsername())) {
            Account ac = accountInterface.findByUsername(account.getUsername());
            if (ac == null || ac.getPassword() == null) {
                return ResponseEntity.notFound().build();
            }
            String encoded = Base64.getEncoder().encodeToString(account.getPassword().getBytes());
            if (ac.getPassword().equals(encoded)) {

                accountDTO accountDTO=new accountDTO();
                accountDTO.setId(ac.getId());
                accountDTO.setName(ac.getName());
                accountDTO.setRole(ac.getRole());
                Optional<Player> pOpt = playerInterface.findByAccount_Id(ac.getId());
                if (pOpt.isPresent() && pOpt.get().getId() != null) {
                    accountDTO.setPlayerId(pOpt.get().getId());
                }
                accountDTO.setUsername(ac.getUsername());

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
