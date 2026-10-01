package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.service.BusinessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public Business createBusiness(@RequestBody Business business) {
        return businessService.createBusiness(business);
    }

    @GetMapping
    public List<Business> getAllBusinesses() {
        return businessService.getAllBusinesses();
    }
}