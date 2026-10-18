package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.HsnSacImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HsnSacImportBatchRepository extends JpaRepository<HsnSacImportBatch, Long> {

    Optional<HsnSacImportBatch> findByBatchId(String batchId);

    List<HsnSacImportBatch> findAllByOrderByCreatedAtDesc();
}
