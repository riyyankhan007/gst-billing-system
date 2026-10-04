package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByBusiness_IdOrderByNameAsc(Long businessId);
    Optional<Supplier> findByIdAndBusiness_Id(Long id, Long businessId);

    @Query("SELECT s FROM Supplier s WHERE s.business.id = :businessId AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.gstin) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Supplier> searchSuppliers(@Param("businessId") Long businessId, @Param("query") String query);
}
