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
    @JsonIgnore private Business business;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public Business getBusiness() { return business; } public void setBusiness(Business business) { this.business = business; }
}
