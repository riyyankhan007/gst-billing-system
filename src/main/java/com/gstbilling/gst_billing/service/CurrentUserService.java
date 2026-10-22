package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.BusinessMembership;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.repository.BusinessMembershipRepository;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import com.gstbilling.gst_billing.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserRepository users;
    private final BusinessRepository businesses;
    private final BusinessMembershipRepository memberships;

    public CurrentUserService(UserRepository users, BusinessRepository businesses, BusinessMembershipRepository memberships) {
        this.users = users;
        this.businesses = businesses;
        this.memberships = memberships;
    }

    public User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof String email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            // Verify caller actually has membership in this active tenant context
            if (user.getBusiness() != null && user.getBusiness().getId().equals(tenantId)) {
                return user;
            }
            BusinessMembership mem = memberships.findActiveMembership(user.getId(), tenantId).orElse(null);
            if (mem != null) {
                user.setBusiness(mem.getBusiness());
                user.setUserRole(mem.getRole());
            }
        } else if (user.getBusiness() != null) {
            TenantContext.setTenantId(user.getBusiness().getId());
        }

        return user;
    }

    public Long getCurrentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        User user = getCurrentUser();
        return user.getBusiness() != null ? user.getBusiness().getId() : null;
    }

    public Business getCurrentBusiness() {
        Long tenantId = getCurrentTenantId();
        if (tenantId != null) {
            return businesses.findById(tenantId)
                    .orElseGet(() -> getCurrentUser().getBusiness());
        }
        return getCurrentUser().getBusiness();
    }
}
