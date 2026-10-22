package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.BusinessMembershipDto;
import com.gstbilling.gst_billing.dto.CreateBusinessRequest;
import com.gstbilling.gst_billing.dto.InviteMemberRequest;
import com.gstbilling.gst_billing.dto.SwitchBusinessResponse;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.BusinessMembership;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.entity.Warehouse;
import com.gstbilling.gst_billing.repository.BusinessMembershipRepository;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import com.gstbilling.gst_billing.repository.WarehouseRepository;
import com.gstbilling.gst_billing.security.JwtService;
import com.gstbilling.gst_billing.util.IndianTaxValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BusinessMembershipService {

    private final BusinessMembershipRepository membershipRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final CurrentUserService currentUserService;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public BusinessMembershipService(
            BusinessMembershipRepository membershipRepository,
            BusinessRepository businessRepository,
            UserRepository userRepository,
            WarehouseRepository warehouseRepository,
            CurrentUserService currentUserService,
            JwtService jwtService,
            AuditLogService auditLogService
    ) {
        this.membershipRepository = membershipRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
        this.currentUserService = currentUserService;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<BusinessMembershipDto> getUserBusinesses() {
        User user = currentUserService.getCurrentUser();
        List<BusinessMembership> memberships = membershipRepository.findActiveMembershipsByUserId(user.getId());

        // Self-healing backfill if membership table was not populated for this user
        if (memberships.isEmpty() && user.getBusiness() != null) {
            BusinessMembership fallback = new BusinessMembership(user, user.getBusiness(), user.getUserRole(), "ACTIVE", null);
            membershipRepository.save(fallback);
            memberships = List.of(fallback);
        }

        return memberships.stream()
                .map(m -> new BusinessMembershipDto(
                        m.getId(),
                        m.getBusiness().getId(),
                        m.getBusiness().getName(),
                        m.getBusiness().getGstin(),
                        m.getRole().name(),
                        m.getStatus(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public SwitchBusinessResponse switchBusiness(Long targetBusinessId) {
        User user = currentUserService.getCurrentUser();

        BusinessMembership membership = membershipRepository.findActiveMembership(user.getId(), targetBusinessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: You do not have an active membership in the requested business"));

        Business targetBusiness = membership.getBusiness();
        user.setBusiness(targetBusiness);
        user.setUserRole(membership.getRole());
        userRepository.save(user);

        String newToken = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                targetBusiness.getId(),
                membership.getRole().name()
        );

        auditLogService.logAction("SWITCH_BUSINESS", "BUSINESS", targetBusiness.getId(),
                "Switched active business to " + targetBusiness.getName() + " with role " + membership.getRole());

        return new SwitchBusinessResponse(
                newToken,
                targetBusiness.getId(),
                targetBusiness.getName(),
                targetBusiness.getGstin(),
                membership.getRole().name(),
                user.getName(),
                user.getEmail()
        );
    }

    @Transactional
    public SwitchBusinessResponse createBusiness(CreateBusinessRequest request) {
        User user = currentUserService.getCurrentUser();

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business name is required");
        }

        Business business = new Business();
        business.setName(request.name().trim());

        if (request.gstin() != null && !request.gstin().isBlank()) {
            String cleanGstin = request.gstin().trim().toUpperCase();
            if (!IndianTaxValidator.isValidGstin(cleanGstin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format");
            }
            if (businessRepository.existsByGstinIgnoreCase(cleanGstin)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another business is already registered with this GSTIN");
            }
            business.setGstin(cleanGstin);
            business.setPan(IndianTaxValidator.extractPanFromGstin(cleanGstin));
        }

        if (request.pan() != null && !request.pan().isBlank()) {
            business.setPan(request.pan().trim().toUpperCase());
        }

        business.setAddress(request.address());
        business.setState(request.state());
        business.setStateCode(request.stateCode());
        business.setPhone(request.phone());
        business.setEmail(request.email());
        business.setInvoicePrefix((request.invoicePrefix() != null && !request.invoicePrefix().isBlank())
                ? request.invoicePrefix().trim().toUpperCase() : "INV");

        Business savedBusiness = businessRepository.save(business);

        // Auto-create default primary warehouse for this new business
        Warehouse defaultWarehouse = new Warehouse(
                savedBusiness,
                "Main Warehouse",
                "MAIN",
                savedBusiness.getAddress(),
                savedBusiness.getState(),
                savedBusiness.getStateCode(),
                true
        );
        warehouseRepository.save(defaultWarehouse);

        // Add user as OWNER of this business
        BusinessMembership membership = new BusinessMembership(user, savedBusiness, UserRole.OWNER, "ACTIVE", user.getEmail());
        membershipRepository.save(membership);

        // Set as active business for user
        user.setBusiness(savedBusiness);
        user.setUserRole(UserRole.OWNER);
        userRepository.save(user);

        String newToken = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                savedBusiness.getId(),
                UserRole.OWNER.name()
        );

        auditLogService.logAction("CREATE_BUSINESS", "BUSINESS", savedBusiness.getId(),
                "Created new business: " + savedBusiness.getName());

        return new SwitchBusinessResponse(
                newToken,
                savedBusiness.getId(),
                savedBusiness.getName(),
                savedBusiness.getGstin(),
                UserRole.OWNER.name(),
                user.getName(),
                user.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getMembersOfCurrentBusiness() {
        Long businessId = currentUserService.getCurrentTenantId();
        return membershipRepository.findByBusiness_Id(businessId);
    }

    @Transactional
    public BusinessMembership inviteMember(InviteMemberRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Long businessId = currentUserService.getCurrentTenantId();

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        String cleanEmail = request.email().trim().toLowerCase();
        User targetUser = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No account found with email '" + cleanEmail + "'. User must create an account first."));

        if (membershipRepository.existsByUser_IdAndBusiness_Id(targetUser.getId(), businessId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this business");
        }

        UserRole role = UserRole.fromString(request.role());
        BusinessMembership membership = new BusinessMembership(targetUser, business, role, "ACTIVE", currentUser.getEmail());
        BusinessMembership saved = membershipRepository.save(membership);

        auditLogService.logAction("INVITE_MEMBER", "BUSINESS_MEMBERSHIP", saved.getId(),
                "Added user " + cleanEmail + " to " + business.getName() + " with role " + role);

        return saved;
    }

    @Transactional
    public BusinessMembership updateMemberRole(Long membershipId, String newRoleStr) {
        Long businessId = currentUserService.getCurrentTenantId();
        BusinessMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (!membership.getBusiness().getId().equals(businessId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Membership does not belong to active business");
        }

        UserRole newRole = UserRole.fromString(newRoleStr);

        // Prevent demoting the last OWNER
        if (membership.getRole() == UserRole.OWNER && newRole != UserRole.OWNER) {
            long ownerCount = membershipRepository.countByBusiness_IdAndRole(businessId, UserRole.OWNER);
            if (ownerCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change role: A business must have at least one OWNER.");
            }
        }

        membership.setRole(newRole);
        membership.setUpdatedAt(LocalDateTime.now());
        BusinessMembership saved = membershipRepository.save(membership);

        auditLogService.logAction("UPDATE_MEMBER_ROLE", "BUSINESS_MEMBERSHIP", saved.getId(),
                "Updated role of member " + membership.getUser().getEmail() + " to " + newRole);

        return saved;
    }

    @Transactional
    public void removeMember(Long membershipId) {
        Long businessId = currentUserService.getCurrentTenantId();
        BusinessMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (!membership.getBusiness().getId().equals(businessId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Membership does not belong to active business");
        }

        // Prevent removing the last OWNER
        if (membership.getRole() == UserRole.OWNER) {
            long ownerCount = membershipRepository.countByBusiness_IdAndRole(businessId, UserRole.OWNER);
            if (ownerCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the only OWNER of the business.");
            }
        }

        membershipRepository.delete(membership);
        auditLogService.logAction("REMOVE_MEMBER", "BUSINESS_MEMBERSHIP", membershipId,
                "Removed member " + membership.getUser().getEmail() + " from business");
    }
}
