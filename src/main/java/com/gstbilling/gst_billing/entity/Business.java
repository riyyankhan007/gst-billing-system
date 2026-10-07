package com.gstbilling.gst_billing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "business")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String gstin;

    private String pan;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String state;

    @Column(name = "state_code")
    private String stateCode;

    private String phone;

    private String email;

    private String website;

    @Column(name = "logo_path")
    private String logo;

    @Column(name = "invoice_prefix")
    private String invoicePrefix = "INV";

    @Column(name = "financial_year")
    private String financialYear = "2026-27";

    @Column(name = "invoice_seq_number")
    private Long invoiceSeqNumber = 1L;

    @Column(name = "bank_name")
    private String bankName;

    @jakarta.persistence.Convert(converter = com.gstbilling.gst_billing.util.EncryptedStringConverter.class)
    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "upi_qr_code")
    private String upiQrCode;

    @Column(name = "signature_path")
    private String signature;

    @Column(name = "default_terms", columnDefinition = "TEXT")
    private String defaultTerms = "1. Goods once sold will not be taken back.\n2. Interest @ 18% p.a. will be charged if payment is not made within due date.\n3. Subject to local jurisdiction.";

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
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public Long getInvoiceSeqNumber() {
        return invoiceSeqNumber;
    }

    public void setInvoiceSeqNumber(Long invoiceSeqNumber) {
        this.invoiceSeqNumber = invoiceSeqNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankIfsc() {
        return bankIfsc;
    }

    public void setBankIfsc(String bankIfsc) {
        this.bankIfsc = bankIfsc;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getUpiQrCode() {
        return upiQrCode;
    }

    public void setUpiQrCode(String upiQrCode) {
        this.upiQrCode = upiQrCode;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDefaultTerms() {
        return defaultTerms;
    }

    public void setDefaultTerms(String defaultTerms) {
        this.defaultTerms = defaultTerms;
    }
}
