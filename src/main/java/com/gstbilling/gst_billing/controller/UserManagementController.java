package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.service.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserManagementService.UserDto> getBusinessUsers() {
        return userManagementService.getBusinessUsers();
    }

    @PostMapping
    public ResponseEntity<UserManagementService.UserDto> createUser(@RequestBody UserManagementService.CreateUserRequest request) {
        UserManagementService.UserDto created = userManagementService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/role")
    public UserManagementService.UserDto updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return userManagementService.updateUserRole(id, role);
    }
}
