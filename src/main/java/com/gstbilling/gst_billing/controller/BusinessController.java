package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.BusinessMembershipDto;
import com.gstbilling.gst_billing.dto.CreateBusinessRequest;
import com.gstbilling.gst_billing.dto.InviteMemberRequest;
import com.gstbilling.gst_billing.dto.SwitchBusinessResponse;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.BusinessMembership;
import com.gstbilling.gst_billing.service.BusinessMembershipService;
import com.gstbilling.gst_billing.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;
    private final BusinessMembershipService membershipService;

    public BusinessController(BusinessService businessService, BusinessMembershipService membershipService) {
        this.businessService = businessService;
        this.membershipService = membershipService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public Business getBusiness() {
        return businessService.getBusiness();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Business updateBusiness(@RequestBody Business business) {
        return businessService.updateBusiness(business);
    }

    @PostMapping("/logo")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Business uploadLogo(@RequestParam("file") MultipartFile file) {
        return businessService.saveLogo(file);
    }

    @PostMapping("/signature")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Business uploadSignature(@RequestParam("file") MultipartFile file) {
        return businessService.saveSignature(file);
    }

    // ==========================================
    // Multi-Business Membership & Switching
    // ==========================================

    @GetMapping("/memberships")
    public List<BusinessMembershipDto> getUserBusinesses() {
        return membershipService.getUserBusinesses();
    }

    @PostMapping("/switch/{businessId}")
    public SwitchBusinessResponse switchBusiness(@PathVariable Long businessId) {
        return membershipService.switchBusiness(businessId);
    }

    @PostMapping("/create")
    public SwitchBusinessResponse createBusiness(@Valid @RequestBody CreateBusinessRequest request) {
        return membershipService.createBusiness(request);
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public List<BusinessMembership> getMembersOfCurrentBusiness() {
        return membershipService.getMembersOfCurrentBusiness();
    }

    @PostMapping("/members/invite")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public BusinessMembership inviteMember(@Valid @RequestBody InviteMemberRequest request) {
        return membershipService.inviteMember(request);
    }

    @PutMapping("/members/{membershipId}/role")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public BusinessMembership updateMemberRole(@PathVariable Long membershipId, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return membershipService.updateMemberRole(membershipId, role);
    }

    @DeleteMapping("/members/{membershipId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> removeMember(@PathVariable Long membershipId) {
        membershipService.removeMember(membershipId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }
}
