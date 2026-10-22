package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_membership", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_business", columnNames = {"user_id", "business_id"})
}, indexes = {
        @Index(name = "idx_membership_user", columnList = "user_id"),
        @Index(name = "idx_membership_business", columnList = "business_id"),
        @Index(name = "idx_membership_status", columnList = "status")
})
public class BusinessMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.OWNER;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INVITED, SUSPENDED

    @Column(name = "invited_by")
    private String invitedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BusinessMembership() {
    }

    public BusinessMembership(User user, Business business, UserRole role, String status, String invitedBy) {
        this.user = user;
        this.business = business;
        this.role = role != null ? role : UserRole.OWNER;
        this.status = status != null ? status : "ACTIVE";
        this.invitedBy = invitedBy;
        this.createdAt = LocalDateTime.now();
    }

    public BusinessMembership(User user, Business business, UserRole role, String status) {
        this(user, business, role, status, null);
    }

    public BusinessMembership(User user, Business business, UserRole role) {
        this(user, business, role, "ACTIVE", null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
