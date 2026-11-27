package com.stocksim.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    @Autowired PortfolioService portfolioService;

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

    @Test
    void withdrawalsReduceTheBalanceAndShowAsNegativeEntries() throws Exception {
        String token = login(newUser(null));
        String id = myPortfolioId(token);
        deposit(token, id, "1000").andExpect(status().isCreated());

        withdraw(token, id, "250.75").andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(-250.75));

        mvc.perform(get("/api/portfolios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.cash").value(749.25));
        mvc.perform(get("/api/portfolios/" + id + "/ledger").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"));
    }

    @Test
    void withdrawingTheWholeBalanceWorksButOneCentMoreIsRejected() throws Exception {
        String token = login(newUser(null));
        String id = myPortfolioId(token);
        deposit(token, id, "100").andExpect(status().isCreated());

        withdraw(token, id, "100.01").andExpect(status().isConflict());
        withdraw(token, id, "100.00").andExpect(status().isCreated());
        withdraw(token, id, "0.01").andExpect(status().isConflict());

        mvc.perform(get("/api/portfolios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.cash").value(0.00));
    }

    @Test
    void invalidWithdrawalAmountsAreRejected() throws Exception {
        String token = login(newUser(null));
        String id = myPortfolioId(token);
        deposit(token, id, "100").andExpect(status().isCreated());

        withdraw(token, id, "0").andExpect(status().isBadRequest());
        withdraw(token, id, "-10").andExpect(status().isBadRequest());
        withdraw(token, id, "10.005").andExpect(status().isBadRequest());   // EUR has 2 decimals
    }

    @Test
    void cannotWithdrawFromSomeoneElsesPortfolio() throws Exception {
        String ownerToken = login(newUser(null));
        String id = myPortfolioId(ownerToken);
        deposit(ownerToken, id, "100").andExpect(status().isCreated());

        withdraw(login(newUser(null)), id, "10").andExpect(status().isForbidden());
    }

    /**
     * 8 withdrawals of 200 from a balance of 1000, all at once. With the portfolio row locked they run one at a
     * time, so exactly 5 succeed and the balance ends at 0. Without the lock several could read 1000 at the same
     * moment and the balance would go negative.
     */
    @Test
    void concurrentWithdrawalsNeverOverdraw() throws Exception {
        UUID userId = userService.register("user-" + UUID.randomUUID() + "@example.com", PASSWORD, null, null).getId();
        UUID portfolioId = portfolioService.listFor(userId).get(0).id();
        portfolioService.deposit(portfolioId, userId, new BigDecimal("1000"));

        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    start.await();   // every thread waits here, then they all go at once
                    portfolioService.withdraw(portfolioId, userId, new BigDecimal("200"));
                    succeeded.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    refused.incrementAndGet();
                } catch (Throwable e) {
                    unexpected.add(e);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected).isEmpty();
        assertThat(succeeded).hasValue(5);
        assertThat(refused).hasValue(3);
        assertThat(portfolioService.get(portfolioId, userId).cash()).isEqualByComparingTo("0");
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

    private ResultActions withdraw(String token, String portfolioId, String amount) throws Exception {
        return mvc.perform(post("/api/portfolios/" + portfolioId + "/withdrawals")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + "}"));
    }
}
