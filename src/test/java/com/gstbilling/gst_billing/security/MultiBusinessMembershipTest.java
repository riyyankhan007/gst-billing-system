package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.dto.BusinessMembershipDto;
import com.gstbilling.gst_billing.dto.CreateBusinessRequest;
import com.gstbilling.gst_billing.dto.InviteMemberRequest;
import com.gstbilling.gst_billing.dto.SwitchBusinessResponse;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.BusinessMembership;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.repository.BusinessMembershipRepository;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import com.gstbilling.gst_billing.service.BusinessMembershipService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MultiBusinessMembershipTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BusinessMembershipRepository membershipRepository;
    @Autowired private BusinessMembershipService membershipService;
    @Autowired private JwtService jwtService;

    private User user1;
    private User user2;
    private Business business1;
    private Business business2;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Business 1 & User 1
        business1 = new Business();
        business1.setName("Alpha Retail " + runId);
        business1.setInvoicePrefix("ALP");
        business1.setState("Maharashtra");
        business1 = businessRepository.save(business1);

        user1 = new User();
        user1.setName("Alice Owner");
        user1.setEmail("alice_" + runId + "@alpha.com");
        user1.setPassword("Password@123");
        user1.setBusiness(business1);
        user1.setRole("OWNER");
        user1 = userRepository.save(user1);

        BusinessMembership m1 = new BusinessMembership(user1, business1, UserRole.OWNER, "ACTIVE");
        membershipRepository.save(m1);

        // Business 2 & User 2
        business2 = new Business();
        business2.setName("Beta Logistics " + runId);
        business2.setInvoicePrefix("BET");
        business2.setState("Karnataka");
        business2 = businessRepository.save(business2);

        user2 = new User();
        user2.setName("Bob Owner");
        user2.setEmail("bob_" + runId + "@beta.com");
        user2.setPassword("Password@123");
        user2.setBusiness(business2);
        user2.setRole("OWNER");
        user2 = userRepository.save(user2);

        BusinessMembership m2 = new BusinessMembership(user2, business2, UserRole.OWNER, "ACTIVE");
        membershipRepository.save(m2);
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
        if (user1 != null && user1.getId() != null) {
            try { membershipRepository.deleteAll(membershipRepository.findActiveMembershipsByUserId(user1.getId())); } catch (Exception ignored) {}
            try { userRepository.deleteById(user1.getId()); } catch (Exception ignored) {}
        }
        if (user2 != null && user2.getId() != null) {
            try { membershipRepository.deleteAll(membershipRepository.findActiveMembershipsByUserId(user2.getId())); } catch (Exception ignored) {}
            try { userRepository.deleteById(user2.getId()); } catch (Exception ignored) {}
        }
        if (business1 != null && business1.getId() != null) {
            try { businessRepository.deleteById(business1.getId()); } catch (Exception ignored) {}
        }
        if (business2 != null && business2.getId() != null) {
            try { businessRepository.deleteById(business2.getId()); } catch (Exception ignored) {}
        }
    }

    private void authenticateAs(User user, Business business, String role) {
        TenantContext.setTenantId(business.getId());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("User can create a second business and switch between authorized businesses")
    void testCreateSecondBusinessAndSwitch() {
        authenticateAs(user1, business1, "OWNER");

        String gammaName = "Gamma Consulting " + UUID.randomUUID().toString().substring(0, 6);
        CreateBusinessRequest createReq = new CreateBusinessRequest(
                gammaName,
                null,
                null,
                "Nariman Point",
                "Maharashtra",
                "27",
                null,
                null,
                "GAM"
        );
        SwitchBusinessResponse createResp = membershipService.createBusiness(createReq);
        assertNotNull(createResp.token());
        assertEquals("OWNER", createResp.role());
        assertEquals(gammaName, createResp.businessName());

        // Verify User 1 now has 2 business memberships
        List<BusinessMembershipDto> memberships = membershipService.getUserBusinesses();
        assertEquals(2, memberships.size(), "User 1 should belong to 2 businesses");

        // Switch back to Business 1
        SwitchBusinessResponse switchResp = membershipService.switchBusiness(business1.getId());
        assertNotNull(switchResp.token());
        assertEquals(business1.getId(), jwtService.extractTenantId(switchResp.token()));
        assertEquals(user1.getId(), jwtService.extractUserId(switchResp.token()));
        assertEquals("OWNER", jwtService.extractRole(switchResp.token()));
    }

    @Test
    @DisplayName("User cannot switch to a business they are not a member of")
    void testCannotSwitchToUnauthorizedBusiness() {
        authenticateAs(user1, business1, "OWNER");

        // User 1 attempts to switch to Business 2 (Bob's business)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                membershipService.switchBusiness(business2.getId())
        );
        assertEquals(403, ex.getStatusCode().value(), "Must return 403 Forbidden for unauthorized business switch");
    }

    @Test
    @DisplayName("Inviting a user gives them active membership and role in the new business")
    void testInviteUserToBusiness() {
        // Bob (Business 2 OWNER) invites Alice (User 1) as ACCOUNTANT
        authenticateAs(user2, business2, "OWNER");

        InviteMemberRequest inviteReq = new InviteMemberRequest(user1.getEmail(), UserRole.ACCOUNTANT.name());
        membershipService.inviteMember(inviteReq);

        // Now authenticate as Alice
        authenticateAs(user1, business1, "OWNER");

        // Alice switches to Business 2
        SwitchBusinessResponse switchResp = membershipService.switchBusiness(business2.getId());
        assertNotNull(switchResp.token());
        assertEquals(business2.getId(), jwtService.extractTenantId(switchResp.token()));
        assertEquals("ACCOUNTANT", switchResp.role());
    }
}
