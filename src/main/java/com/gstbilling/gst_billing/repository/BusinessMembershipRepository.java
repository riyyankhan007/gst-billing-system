package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.BusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, Long> {

    List<BusinessMembership> findByUser_Id(Long userId);

    @Query("SELECT bm FROM BusinessMembership bm WHERE bm.user.id = :userId AND bm.status = 'ACTIVE'")
    List<BusinessMembership> findActiveMembershipsByUserId(@Param("userId") Long userId);

    Optional<BusinessMembership> findByUser_IdAndBusiness_Id(Long userId, Long businessId);

    @Query("SELECT bm FROM BusinessMembership bm WHERE bm.user.id = :userId AND bm.business.id = :businessId AND bm.status = 'ACTIVE'")
    Optional<BusinessMembership> findActiveMembership(@Param("userId") Long userId, @Param("businessId") Long businessId);

    List<BusinessMembership> findByBusiness_Id(Long businessId);

    boolean existsByUser_IdAndBusiness_Id(Long userId, Long businessId);

    long countByBusiness_IdAndRole(Long businessId, com.gstbilling.gst_billing.entity.UserRole role);

    void deleteByUser_IdAndBusiness_Id(Long userId, Long businessId);
}
