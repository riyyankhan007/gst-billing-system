package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @JsonIgnore @Column(nullable = false) private String password;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "business_id", nullable = false)
    @JsonIgnore
    private Business business;

    @Column(name = "role")
    private String role = "ADMIN"; // ADMIN, ACCOUNTANT, SALES, VIEWER

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_expires_at")
    private java.time.LocalDateTime resetPasswordExpiresAt;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public Business getBusiness() { return business; } public void setBusiness(Business business) { this.business = business; }
    public String getRole() { return role != null ? role : "ADMIN"; } public void setRole(String role) { this.role = role; }
    public String getResetPasswordToken() { return resetPasswordToken; } public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }
    public java.time.LocalDateTime getResetPasswordExpiresAt() { return resetPasswordExpiresAt; } public void setResetPasswordExpiresAt(java.time.LocalDateTime resetPasswordExpiresAt) { this.resetPasswordExpiresAt = resetPasswordExpiresAt; }
}
