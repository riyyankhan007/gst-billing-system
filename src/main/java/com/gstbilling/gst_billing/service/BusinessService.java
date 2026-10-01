package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final CurrentUserService currentUserService;

    public BusinessService(BusinessRepository businessRepository, CurrentUserService currentUserService) {
        this.businessRepository = businessRepository;
        this.currentUserService = currentUserService;
    }

    public Business getBusiness() {
        return currentUserService.getCurrentUser().getBusiness();
    }
    public Business updateBusiness(Business changes) {
        Business business = getBusiness();
        business.setName(changes.getName()); business.setGstin(changes.getGstin()); business.setAddress(changes.getAddress());
        business.setState(changes.getState()); business.setStateCode(changes.getStateCode()); business.setPhone(changes.getPhone());
        business.setEmail(changes.getEmail()); business.setWebsite(changes.getWebsite()); business.setLogo(changes.getLogo()); business.setInvoicePrefix(changes.getInvoicePrefix());
        return businessRepository.save(business);
    }
    public Business saveLogo(MultipartFile file) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a logo file");
        String filename = "business-" + getBusiness().getId() + "-" + System.currentTimeMillis() + "-" + Path.of(file.getOriginalFilename()).getFileName();
        try {
            Path directory = Path.of("uploads").toAbsolutePath(); Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            Business business = getBusiness(); business.setLogo("/uploads/" + filename); return businessRepository.save(business);
        } catch (IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save logo"); }
    }
}
