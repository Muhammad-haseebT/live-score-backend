package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountInterface extends JpaRepository<Account,Integer> {

    public Account findByAridAndPassword(String Arid,String Password);

    Account findByArid(String arid);
}
