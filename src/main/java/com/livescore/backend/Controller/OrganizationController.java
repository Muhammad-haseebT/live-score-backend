package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Organization;
import com.livescore.backend.Service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController

public class OrganizationController
{

    @Autowired
    OrganizationService os;

    @PostMapping("/org/add")
    public ResponseEntity<?> addOrg(@RequestBody Organization o) {
        return os.add(o);
    }

    @GetMapping("/org/get")
    public ResponseEntity<?> getOrg() {
        return os.get();
    }

    @GetMapping("/org/get/{id}")
    public ResponseEntity<?> getOrgById(@PathVariable int id) {
        return os.getById(id);
    }

    @DeleteMapping("/org/delete/{id}")
    public ResponseEntity<?> deleteOrg(@PathVariable int id) {
        return os.delete(id);
    }

    @PutMapping("/org/update/{id}")
    public ResponseEntity<?> updateOrg(@PathVariable int id, @RequestBody Organization o) {
        return os.update(id, o);
    }




}
