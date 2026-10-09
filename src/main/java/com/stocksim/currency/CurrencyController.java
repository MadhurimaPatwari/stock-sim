package com.stocksim.currency;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/currencies  list supported currencies, so clients know valid values for reportingCurrency.
 */
@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    public record CurrencyResponse(String code, String name, int minorUnits) {
        static CurrencyResponse from(Currency c) {
            return new CurrencyResponse(c.getCode(), c.getName(), c.getMinorUnits());
        }
    }

    private final CurrencyRepository currencies;

    public CurrencyController(CurrencyRepository currencies) {
        this.currencies = currencies;
    }

    @GetMapping
    public List<CurrencyResponse> list() {
        return currencies.findAll(Sort.by("code")).stream()
                .map(CurrencyResponse::from)
                .toList();
    }
}
