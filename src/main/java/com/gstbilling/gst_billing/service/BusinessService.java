package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public Business createBusiness(Business business) {
        return businessRepository.save(business);
    }

    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }
}