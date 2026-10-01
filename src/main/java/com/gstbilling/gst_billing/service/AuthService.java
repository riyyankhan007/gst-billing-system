package com.gstbilling.gst_billing.service;
import com.gstbilling.gst_billing.dto.*;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import com.gstbilling.gst_billing.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AuthService {
    private final UserRepository users; private final BusinessRepository businesses; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, BusinessRepository businesses, PasswordEncoder encoder, JwtService jwt) { this.users=users; this.businesses=businesses; this.encoder=encoder; this.jwt=jwt; }
    @Transactional public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        Business business = new Business(); business.setName(request.businessName()); business.setInvoicePrefix("INV"); businesses.save(business);
        User user = new User(); user.setName(request.name()); user.setEmail(request.email().trim().toLowerCase()); user.setPassword(encoder.encode(request.password())); user.setBusiness(business); users.save(user);
        return response(user);
    }
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(request.password(), user.getPassword())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return response(user);
    }
    private AuthResponse response(User user) { return new AuthResponse(jwt.generateToken(user.getEmail(), user.getId()), user.getName(), user.getEmail()); }
}
