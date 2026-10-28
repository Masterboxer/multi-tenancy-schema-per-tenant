package com.masterboxer.springbootmultitenancy.controller;

import com.masterboxer.springbootmultitenancy.record.CustomerRecord;
import com.masterboxer.springbootmultitenancy.record.PageResponse;
import com.masterboxer.springbootmultitenancy.service.CustomerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // NOTE: do not add @Validated to this class; Spring 6.1+ validates the parameter constraints
    // below natively and answers 400 on violations.
    @GetMapping("/get")
    public ResponseEntity<PageResponse<CustomerRecord>> getCustomers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        long start = System.nanoTime();
        PageResponse<CustomerRecord> result = customerService.getCustomers(page, size);
        log.info("Fetched customers page={} size={} in {} ms", page, size, (System.nanoTime() - start) / 1_000_000);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    public ResponseEntity<Void> insertCustomers(@RequestParam @Min(1) @Max(1000) int customerNumber) {
        customerService.insertCustomers(customerNumber);
        return ResponseEntity.ok().build();
    }
}
