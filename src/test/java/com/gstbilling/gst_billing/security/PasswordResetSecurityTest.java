package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.dto.ForgotPasswordRequest;
import com.gstbilling.gst_billing.dto.ResetPasswordRequest;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import com.gstbilling.gst_billing.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PasswordResetSecurityTest {

    static {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        Business business = new Business();
        business.setName("Security Test Business");
        business = businessRepository.save(business);

        testUser = new User();
        testUser.setName("Security Tester");
        testUser.setEmail("tester@securitytest.com");
        testUser.setPassword(passwordEncoder.encode("OldPassword123!"));
        testUser.setBusiness(business);
        testUser.setUserRole(UserRole.OWNER);
        testUser.setResetPasswordToken("123456");
        testUser.setResetPasswordExpiresAt(LocalDateTime.now().plusMinutes(15));
        testUser.setFailedLoginAttempts(0);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should successfully reset password when correct email and token are provided")
    void shouldResetPasswordWithValidEmailAndToken() {
        ResetPasswordRequest request = new ResetPasswordRequest("tester@securitytest.com", "123456", "NewSecretPassword123!");
        var response = authService.resetPassword(request);

        assertTrue(response.success());
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertNull(updated.getResetPasswordToken());
        assertNull(updated.getResetPasswordExpiresAt());
        assertTrue(passwordEncoder.matches("NewSecretPassword123!", updated.getPassword()));
    }

    @Test
    @DisplayName("Should reject and increment failed attempts when wrong token is provided for email")
    void shouldRejectAndLockOnRepeatedInvalidTokens() {
        // 4 failed attempts
        for (int i = 0; i < 4; i++) {
            final String wrongToken = "99999" + i;
            assertThrows(ResponseStatusException.class, () ->
                    authService.resetPassword(new ResetPasswordRequest("tester@securitytest.com", wrongToken, "NewPassword123!"))
            );
        }

        User midCheck = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(4, midCheck.getFailedLoginAttempts());
        assertFalse(midCheck.isAccountLocked());

        // 5th failed attempt triggers account lock & token invalidation
        assertThrows(ResponseStatusException.class, () ->
                authService.resetPassword(new ResetPasswordRequest("tester@securitytest.com", "000000", "NewPassword123!"))
        );

        User lockedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(lockedUser.isAccountLocked());
        assertNull(lockedUser.getResetPasswordToken(), "Reset token must be wiped upon repeated failed attempts to prevent brute-forcing");
    }

    @Test
    @DisplayName("Should reject expired reset token")
    void shouldRejectExpiredResetToken() {
        testUser.setResetPasswordExpiresAt(LocalDateTime.now().minusMinutes(5));
        userRepository.save(testUser);

        assertThrows(ResponseStatusException.class, () ->
                authService.resetPassword(new ResetPasswordRequest("tester@securitytest.com", "123456", "NewPassword123!"))
        );
    }
}
