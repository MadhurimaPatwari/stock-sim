package com.stocksim;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockSimApplication {

    public static void main(String[] args) {
        // All timestamps in the system are UTC; convert only at the edges (API/UI).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StockSimApplication.class, args);
    }
}
