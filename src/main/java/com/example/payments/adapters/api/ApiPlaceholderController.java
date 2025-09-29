package com.example.payments.adapters.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/placeholder")
class ApiPlaceholderController {

    @GetMapping
    ResponseEntity<String> placeholder() {
        return ResponseEntity.ok("API placeholder endpoint");
    }
}

