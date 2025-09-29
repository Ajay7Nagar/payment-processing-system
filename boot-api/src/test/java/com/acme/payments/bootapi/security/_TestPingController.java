package com.acme.payments.bootapi.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class _TestPingController {
    @GetMapping("/ping")
    String ping() {
        return "ok";
    }
}


