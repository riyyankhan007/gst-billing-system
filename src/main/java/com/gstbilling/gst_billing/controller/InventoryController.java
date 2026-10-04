package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.StockMovement;
import com.gstbilling.gst_billing.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/movements")
    public List<StockMovement> getAllMovements() {
        return inventoryService.getAllMovements();
    }

    @GetMapping("/product/{productId}/movements")
    public List<StockMovement> getMovementsForProduct(@PathVariable Long productId) {
        return inventoryService.getMovementsForProduct(productId);
    }

    @PostMapping("/adjust")
    public StockMovement adjustStock(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return inventoryService.adjustStock(productId, quantity, reason);
    }
}
