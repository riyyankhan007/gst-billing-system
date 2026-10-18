package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.GstTaxRule;
import com.gstbilling.gst_billing.entity.HsnSacType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GstTaxRuleRepository extends JpaRepository<GstTaxRule, Long> {

    List<GstTaxRule> findByHsnSacCodeIdAndActiveTrue(Long hsnSacCodeId);

    List<GstTaxRule> findByHsnSacCode_CodeAndHsnSacCode_TypeAndActiveTrue(String code, HsnSacType type);
}
