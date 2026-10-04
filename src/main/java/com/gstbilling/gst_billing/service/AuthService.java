package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.*;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.User;
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

    public AuthService(
            UserRepository users,
            BusinessRepository businesses,
            PasswordEncoder encoder,
            JwtService jwt,
            EmailService emailService,
            CurrentUserService currentUserService
    ) {
        this.users = users;
        this.businesses = businesses;
        this.encoder = encoder;
        this.jwt = jwt;
        this.emailService = emailService;
        this.currentUserService = currentUserService;
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
        users.save(user);

        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return response(user);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(cleanEmail).ifPresent(user -> {
            // Generate a 6-digit verification code
            SecureRandom random = new SecureRandom();
            String token = String.format("%06d", random.nextInt(1000000));

            user.setResetPasswordToken(token);
            user.setResetPasswordExpiresAt(LocalDateTime.now().plusMinutes(30));
            users.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        });

        // Always return generic message to avoid email enumeration
        return new MessageResponse("If your email is registered in our system, a password reset code has been sent.", true);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String token = request.token().trim();
        User user = users.findByResetPasswordToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset code. Please check and try again."));

        if (user.getResetPasswordExpiresAt() == null || user.getResetPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code has expired. Please request a new one.");
        }

        user.setPassword(encoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiresAt(null);
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
        return new AuthResponse(
                jwt.generateToken(user.getEmail(), user.getId(), user.getRole()),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
