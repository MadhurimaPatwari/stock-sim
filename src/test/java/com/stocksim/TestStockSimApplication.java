package com.stocksim;

import org.springframework.boot.SpringApplication;

/**
 * Run this from your IDE to start the app against a Testcontainers database,
 * without needing docker compose. Data is lost when you stop it.
 */
public class TestStockSimApplication {

    public static void main(String[] args) {
        SpringApplication.from(StockSimApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
