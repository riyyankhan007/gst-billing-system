package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "customer",
        uniqueConstraints = @UniqueConstraint(name = "uk_business_customer_gstin", columnNames = {"business_id", "gstin"})
)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String gstin;

    private String pan;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    private String state;

    @Column(name = "state_code")
    private String stateCode;

    private String phone;

    private String email;

    @Column(name = "customer_type")
    private String customerType = "B2B"; // B2B, B2C, EXPORT

    @Column(name = "credit_limit")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "payment_terms")
    private String paymentTerms = "Net 30";

    @Column(name = "opening_balance")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    @JsonIgnore
    private Business business;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getAddress() {
        if (address == null || address.isBlank()) {
            return billingAddress;
        }
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (this.billingAddress == null || this.billingAddress.isBlank()) {
            this.billingAddress = address;
        }
    }

    public String getBillingAddress() {
        if (billingAddress == null || billingAddress.isBlank()) {
            return address;
        }
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
        if (this.address == null || this.address.isBlank()) {
            this.address = billingAddress;
        }
    }

    public String getShippingAddress() {
        if (shippingAddress == null || shippingAddress.isBlank()) {
            return getBillingAddress();
        }
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit != null ? creditLimit : BigDecimal.ZERO;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance != null ? openingBalance : BigDecimal.ZERO;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }
}
