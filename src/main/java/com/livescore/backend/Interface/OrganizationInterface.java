package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationInterface extends JpaRepository<Organization,Integer> {

}
