package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.service.BusinessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
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
}
