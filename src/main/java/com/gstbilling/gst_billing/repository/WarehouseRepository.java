package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByBusiness_IdOrderByIdAsc(Long businessId);

    List<Warehouse> findByBusiness_IdAndActiveTrueOrderByIdAsc(Long businessId);

    Optional<Warehouse> findByIdAndBusiness_Id(Long id, Long businessId);

    Optional<Warehouse> findByBusiness_IdAndCodeIgnoreCase(Long businessId, String code);

    Optional<Warehouse> findByBusiness_IdAndIsDefaultTrue(Long businessId);

    boolean existsByBusiness_IdAndCodeIgnoreCase(Long businessId, String code);

    boolean existsByBusiness_IdAndCodeIgnoreCaseAndIdNot(Long businessId, String code, Long id);
}
