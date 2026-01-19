package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDto;
import com.livescore.backend.DTO.accountDTO;
import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Interface.TeamInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountService {
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private PlayerService playerService;
    @Autowired PlayerInterface playerInterface;
    @Autowired
    private TeamInterface teamInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;


    public ResponseEntity<?> createAccount(Account account) {
        if (account == null) {
            return ResponseEntity.badRequest().body("Account details are required");
        }
        if (account.getUsername() == null || account.getUsername().isBlank() || account.getPassword() == null || account.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        // username column is unique, so we must reject even if the account is soft-deleted
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
        return accountInterface.findActiveById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getAllAccounts() {
        return ResponseEntity.ok(accountInterface.findAllActive());
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
        Optional<Account> opt = accountInterface.findActiveById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account account = opt.get();
        account.softDelete();
        accountInterface.save(account);

        List<Player> players = playerInterface.findAllByAccount_Id(account.getId());
        if (players == null) {
            players = Collections.emptyList();
        }
        for (Player p : players) {
            if (p == null) continue;
            p.softDelete();
        }
        playerInterface.saveAll(players);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> restoreAccount(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("Account id is required");
        }
        Account account = accountInterface.findByIdIncludingDeleted(id).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        account.restore();
        accountInterface.save(account);

        List<Player> players = playerInterface.findAllByAccount_Id(account.getId());
        if (players != null) {
            for (Player p : players) {
                if (p == null) continue;
                p.restore();
            }
            playerInterface.saveAll(players);
        }

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> loginAccount(accountDTO account) {
        if (account == null || account.getUsername() == null || account.getUsername().isBlank() || account.getPassword() == null || account.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (accountInterface.existsActiveByUsername(account.getUsername())) {
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

    public ResponseEntity<List<accountDTO>> getAllPlayerAccounts(Long tournamentId) {
        // Single query to get all players (not filtering by deleted here for simplicity)
        List<Player> allPlayers = playerInterface.findAllWithAccounts();

        // Single query to get player IDs already in tournament
        Set<Long> alreadyInTournament = playerInterface.findPlayerIdsInTournament(tournamentId);

        // Single query to get ALL player requests for this tournament WITH players and accounts
        List<PlayerRequest> tournamentRequests = playerRequestInterface
                .findByTournamentIdWithPlayerAndAccount(tournamentId);

        // Build a map: playerId -> playerRequestId
        Map<Long, Long> playerIdToRequestId = new HashMap<>();
        for (PlayerRequest pr : tournamentRequests) {
            if (pr.getPlayer() != null) {
                playerIdToRequestId.put(pr.getPlayer().getId(), pr.getId());
            }
        }

        // Filter and map to DTOs
        List<accountDTO> accountDTOs = allPlayers.stream()
                .filter(player -> !Boolean.TRUE.equals(player.getIsDeleted()))
                .filter(player -> !alreadyInTournament.contains(player.getId()))
                .map(player -> {
                    Account account = player.getAccount();
                    if (account == null) return null;

                    accountDTO dto = new accountDTO();
                    dto.setId(playerIdToRequestId.get(player.getId())); // O(1) lookup
                    dto.setUsername(account.getUsername());
                    dto.setName(account.getName());
                    dto.setRole(account.getRole());
                    dto.setPlayerId(player.getId());
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(accountDTOs);
    }

}
