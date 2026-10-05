package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
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
    private final AuditLogService auditLogService;

    public UserManagementService(UserRepository userRepository,
                                 CurrentUserService currentUserService,
                                 PasswordEncoder passwordEncoder,
                                 AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
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
        String currentRole = currentUser.getRole();
        if (!"OWNER".equalsIgnoreCase(currentRole) && !"ADMIN".equalsIgnoreCase(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can add new team members");
        }

        if (req.email() == null || req.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (userRepository.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        UserRole requestedRole = UserRole.fromString(req.role());
        if (requestedRole == UserRole.OWNER && !"OWNER".equalsIgnoreCase(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an OWNER can assign the OWNER role");
        }

        Business business = currentUser.getBusiness();
        User user = new User();
        user.setName(req.name() != null ? req.name().trim() : "Team Member");
        user.setEmail(req.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.password() != null && !req.password().isBlank() ? req.password() : "Password@123"));
        user.setBusiness(business);
        user.setUserRole(requestedRole);

        User saved = userRepository.save(user);
        auditLogService.logAction("INVITE_USER", "USER", saved.getId(),
                "User invited: " + saved.getEmail() + " with role " + saved.getRole());
        return new UserDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserDto updateUserRole(Long userId, String newRole) {
        User currentUser = currentUserService.getCurrentUser();
        String currentRole = currentUser.getRole();
        if (!"OWNER".equalsIgnoreCase(currentRole) && !"ADMIN".equalsIgnoreCase(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can change user roles");
        }

        User user = userRepository.findByIdAndBusiness_Id(userId, currentUser.getBusiness().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in your business"));

        UserRole targetRole = UserRole.fromString(newRole);

        if (user.getUserRole() == UserRole.OWNER && !"OWNER".equalsIgnoreCase(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an OWNER can modify an OWNER's role");
        }

        if (targetRole == UserRole.OWNER && !"OWNER".equalsIgnoreCase(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an OWNER can assign the OWNER role");
        }

        user.setUserRole(targetRole);
        User saved = userRepository.save(user);
        auditLogService.logAction("UPDATE_USER_ROLE", "USER", saved.getId(),
                "User " + saved.getEmail() + " role updated to " + saved.getRole());
        return new UserDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }
}
