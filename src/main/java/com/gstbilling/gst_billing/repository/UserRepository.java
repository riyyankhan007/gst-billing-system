package com.gstbilling.gst_billing.repository;
import com.gstbilling.gst_billing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByResetPasswordToken(String resetPasswordToken);
    java.util.List<User> findByBusiness_Id(Long businessId);
    Optional<User> findByIdAndBusiness_Id(Long id, Long businessId);
}
