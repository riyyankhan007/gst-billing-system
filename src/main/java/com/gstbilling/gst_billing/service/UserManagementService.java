package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository,
                                 CurrentUserService currentUserService,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    public record UserDto(Long id, String name, String email, String role) {}
    public record CreateUserRequest(String name, String email, String password, String role) {}

    @Transactional(readOnly = true)
    public List<UserDto> getBusinessUsers() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return userRepository.findByBusiness_Id(businessId)
                .stream()
                .map(u -> new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole()))
                .toList();
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req) {
        User currentUser = currentUserService.getCurrentUser();
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMINs can add new team members");
        }

        if (req.email() == null || req.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (userRepository.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        Business business = currentUser.getBusiness();
        User user = new User();
        user.setName(req.name() != null ? req.name().trim() : "Team Member");
        user.setEmail(req.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.password() != null && !req.password().isBlank() ? req.password() : "Password@123"));
        user.setBusiness(business);

        String role = (req.role() != null && !req.role().isBlank()) ? req.role().trim().toUpperCase() : "VIEWER";
        if (!List.of("ADMIN", "ACCOUNTANT", "SALES", "VIEWER").contains(role)) {
            role = "VIEWER";
        }
        user.setRole(role);

        User saved = userRepository.save(user);
        return new UserDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserDto updateUserRole(Long userId, String newRole) {
        User currentUser = currentUserService.getCurrentUser();
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMINs can change roles");
        }

        User user = userRepository.findByIdAndBusiness_Id(userId, currentUser.getBusiness().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in your business"));

        String role = (newRole != null && !newRole.isBlank()) ? newRole.trim().toUpperCase() : "VIEWER";
        if (!List.of("ADMIN", "ACCOUNTANT", "SALES", "VIEWER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + newRole);
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        return new UserDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }
}
