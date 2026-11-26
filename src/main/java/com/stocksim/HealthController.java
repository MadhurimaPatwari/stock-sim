package com.stocksim;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    public record Status(String message) {
    }

    @GetMapping("/health")
    public Status health() {
        return new Status("ok");
    }
}