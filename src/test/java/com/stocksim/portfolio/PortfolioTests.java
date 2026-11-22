package com.stocksim.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.stocksim.TestcontainersConfiguration;
import com.stocksim.user.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Portfolios end to end: real signup, real login token, real HTTP requests through security. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PortfolioTests {

    static final String PASSWORD = "correct-horse";

    @Autowired MockMvc mvc;
    @Autowired UserService userService;

    @Test
    void signupCreatesOneEmptyPortfolioInTheUsersCurrency() throws Exception {
        String token = login(newUser("INR"));

        mvc.perform(get("/api/portfolios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].cash").value(0.00));
    }

    @Test
    void depositsAddUpAndAppearInTheLedger() throws Exception {
        String token = login(newUser(null));
        String id = myPortfolioId(token);

        deposit(token, id, "100.50").andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.50));
        deposit(token, id, "25").andExpect(status().isCreated());

        mvc.perform(get("/api/portfolios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.cash").value(125.50));
        mvc.perform(get("/api/portfolios/" + id + "/ledger").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(100.50))
                .andExpect(jsonPath("$[1].amount").value(25.00));
    }

    @Test
    void invalidDepositAmountsAreRejected() throws Exception {
        String token = login(newUser(null));
        String id = myPortfolioId(token);

        deposit(token, id, "0").andExpect(status().isBadRequest());
        deposit(token, id, "-10").andExpect(status().isBadRequest());
        deposit(token, id, "10.005").andExpect(status().isBadRequest());   // EUR has 2 decimals

        mvc.perform(get("/api/portfolios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.cash").value(0.00));
    }

    @Test
    void otherUsersPortfolioIsForbidden() throws Exception {
        String ownerToken = login(newUser(null));
        String id = myPortfolioId(ownerToken);
        String otherToken = login(newUser(null));

        mvc.perform(get("/api/portfolios/" + id).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        deposit(otherToken, id, "10").andExpect(status().isForbidden());
        mvc.perform(get("/api/portfolios/" + id + "/ledger").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void portfoliosNeedAToken() throws Exception {
        mvc.perform(get("/api/portfolios")).andExpect(status().isUnauthorized());
    }

    private String newUser(String currency) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        userService.register(email, PASSWORD, null, currency);
        return email;
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String myPortfolioId(String token) throws Exception {
        String body = mvc.perform(get("/api/portfolios").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private ResultActions deposit(String token, String portfolioId, String amount) throws Exception {
        return mvc.perform(post("/api/portfolios/" + portfolioId + "/deposits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + "}"));
    }
}
