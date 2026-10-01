package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.service.BusinessService;
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
    public Business getBusiness() {
        return businessService.getBusiness();
    }
    @PutMapping
    public Business updateBusiness(@RequestBody Business business) {
        return businessService.updateBusiness(business);
    }
    @PostMapping("/logo")
    public Business uploadLogo(@RequestParam("file") MultipartFile file) { return businessService.saveLogo(file); }
}
