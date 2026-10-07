package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.*;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import com.gstbilling.gst_billing.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository users;
    private final BusinessRepository businesses;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository users,
            BusinessRepository businesses,
            PasswordEncoder encoder,
            JwtService jwt,
            EmailService emailService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.users = users;
        this.businesses = businesses;
        this.encoder = encoder;
        this.jwt = jwt;
        this.emailService = emailService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        Business business = new Business();
        business.setName(request.businessName().trim());
        business.setInvoicePrefix("INV");
        businesses.save(business);

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(cleanEmail);
        user.setPassword(encoder.encode(request.password()));
        user.setBusiness(business);
        user.setUserRole(UserRole.OWNER);
        users.save(user);

        return response(user);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Account is temporarily locked due to multiple failed login attempts. Please try again after 15 minutes.");
        }

        if (!encoder.matches(request.password(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                users.save(user);

                auditLogService.log(
                        user.getBusiness(),
                        user.getId(),
                        user.getEmail(),
                        "ACCOUNT_LOCKED",
                        "USER",
                        user.getId(),
                        "Account locked due to 5 consecutive failed login attempts",
                        null, null, null
                );

                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account is temporarily locked due to multiple failed login attempts. Please try again after 15 minutes.");
            }
            users.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            users.save(user);
        }

        return response(user);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(cleanEmail).ifPresent(user -> {
            // Generate a cryptographically secure 8-character high-entropy verification code (>1 trillion possibilities)
            SecureRandom random = new SecureRandom();
            String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String token = sb.toString();

            user.setResetPasswordToken(token);
            user.setResetPasswordExpiresAt(LocalDateTime.now().plusMinutes(15));
            users.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        });

        // Always return generic message to avoid email enumeration
        return new MessageResponse("If your email is registered in our system, a password reset code has been sent.", true);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String token = request.token().trim();
        User user;

        if (request.email() != null && !request.email().isBlank()) {
            String cleanEmail = request.email().trim().toLowerCase();
            user = users.findByEmailIgnoreCase(cleanEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset code. Please check and try again."));

            if (user.isAccountLocked()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account is temporarily locked due to multiple failed attempts. Please try again after 15 minutes.");
            }

            if (user.getResetPasswordToken() == null || !user.getResetPasswordToken().equals(token)) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= 5) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                    user.setResetPasswordToken(null);
                    user.setResetPasswordExpiresAt(null);
                }
                users.save(user);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset code. Please check and try again.");
            }
        } else {
            user = users.findByResetPasswordToken(token)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset code. Please check and try again."));
        }

        if (user.getResetPasswordExpiresAt() == null || user.getResetPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code has expired. Please request a new one.");
        }

        user.setPassword(encoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiresAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        users.save(user);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());
        return new MessageResponse("Password has been reset successfully! You can now log in with your new password.", true);
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        User user = currentUserService.getCurrentUser();

        if (!encoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password does not match");
        }

        if (encoder.matches(request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as the current password");
        }

        user.setPassword(encoder.encode(request.newPassword()));
        users.save(user);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());
        return new MessageResponse("Your password has been changed successfully! A confirmation email has been sent.", true);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = currentUserService.getCurrentUser();
        Business business = user.getBusiness();
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                business != null ? business.getId() : null,
                business != null ? business.getName() : null,
                business != null ? business.getGstin() : null
        );
    }

    private AuthResponse response(User user) {
        Long tenantId = user.getBusiness() != null ? user.getBusiness().getId() : null;
        return new AuthResponse(
                jwt.generateToken(user.getEmail(), user.getId(), tenantId, user.getRole()),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
