package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.WarehouseAdjustmentRequest;
import com.gstbilling.gst_billing.dto.WarehouseOpeningStockRequest;
import com.gstbilling.gst_billing.dto.WarehouseTransferRequest;
import com.gstbilling.gst_billing.entity.Warehouse;
import com.gstbilling.gst_billing.entity.WarehouseStock;
import com.gstbilling.gst_billing.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<Warehouse> getWarehouses() {
        return warehouseService.getWarehouses();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public Warehouse getWarehouseById(@PathVariable Long id) {
        return warehouseService.getWarehouseById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Warehouse createWarehouse(@Valid @RequestBody Warehouse warehouse) {
        return warehouseService.createWarehouse(warehouse);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Warehouse updateWarehouse(@PathVariable Long id, @RequestBody Warehouse updates) {
        return warehouseService.updateWarehouse(id, updates);
    }

    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<WarehouseStock> getStockByWarehouse(@PathVariable Long id) {
        return warehouseService.getStockByWarehouse(id);
    }

    @PostMapping("/opening-stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public WarehouseStock recordOpeningStock(@Valid @RequestBody WarehouseOpeningStockRequest request) {
        return warehouseService.recordOpeningStock(request);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, String>> transferStock(@Valid @RequestBody WarehouseTransferRequest request) {
        warehouseService.transferStock(request);
        return ResponseEntity.ok(Map.of("message", "Stock transferred successfully"));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public WarehouseStock adjustStock(@Valid @RequestBody WarehouseAdjustmentRequest request) {
        return warehouseService.adjustStock(request);
    }
}
