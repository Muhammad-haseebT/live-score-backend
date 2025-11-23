package com.livescore.backend.Service;

import com.livescore.backend.Entity.Organization;
import com.livescore.backend.Interface.OrganizationInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class OrganizationService
{
    @Autowired
    OrganizationInterface oi;
    public ResponseEntity<?> add(Organization o) {
        if(o==null){
            return ResponseEntity.badRequest().body(Map.of("error", "Organization is required"));
        }
        if(o.getName()==null || o.getName().isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        }
        if(o.getAccount()==null){
            return ResponseEntity.badRequest().body(Map.of("error", "Account is required"));
        }
        oi.save(o);
        return ResponseEntity.ok("Added");

    }

    public ResponseEntity<?> get() {
        return ResponseEntity.ok(oi.findAll());


    }

    public ResponseEntity<?> getById(int id) {
        return ResponseEntity.ok(oi.findById(id));

    }

    public ResponseEntity<?> delete(int id) {
        if(!oi.existsById(id)){
            return ResponseEntity.badRequest().body(Map.of("error", "Organization not found"));

        }
        oi.deleteById(id);
        return ResponseEntity.ok("Deleted");

    }

    public ResponseEntity<?> update(int id, Organization o) {
        if(!oi.existsById(id)){
            return ResponseEntity.badRequest().body(Map.of("error", "Organization not found"));

        }
        oi.save(o);
        return ResponseEntity.ok("Updated");
    }
}
