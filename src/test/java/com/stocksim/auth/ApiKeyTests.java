package com.stocksim.auth;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** API keys end to end: created with a login token, then used as a bearer token with limited scopes. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiKeyTests {

    static final String PASSWORD = "correct-horse";

    @Autowired MockMvc mvc;
    @Autowired UserService userService;

    @Test
    void keyIsShownOnceAtCreationAndNeverListed() throws Exception {
        String login = login(newUser());

        createKey(login, "[\"portfolio:read\"]").andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(startsWith("ssk_")));
        mvc.perform(get("/api/api-keys").header("Authorization", "Bearer " + login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").doesNotExist());
    }

    @Test
    void readOnlyKeyCanReadButNotDeposit() throws Exception {
        String login = login(newUser());
        String key = keyOf(createKey(login, "[\"portfolio:read\"]"));

        String portfolioId = JsonPath.read(call(get("/api/portfolios"), key)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$[0].id");
        deposit(key, portfolioId).andExpect(status().isForbidden());
    }

    @Test
    void depositKeyCanDeposit() throws Exception {
        String login = login(newUser());
        String key = keyOf(createKey(login, "[\"portfolio:read\", \"portfolio:deposit\"]"));
        String portfolioId = JsonPath.read(call(get("/api/portfolios"), key)
                .andReturn().getResponse().getContentAsString(), "$[0].id");

        deposit(key, portfolioId).andExpect(status().isCreated());
    }

    @Test
    void revokedKeyIsRejected() throws Exception {
        String login = login(newUser());
        String created = createKey(login, "[\"portfolio:read\"]").andReturn().getResponse().getContentAsString();
        String key = JsonPath.read(created, "$.key");
        String id = JsonPath.read(created, "$.id");

        call(delete("/api/api-keys/" + id), login).andExpect(status().isNoContent());

        call(get("/api/portfolios"), key).andExpect(status().isUnauthorized());
    }

    @Test
    void apiKeyCannotManageKeys() throws Exception {
        String key = keyOf(createKey(login(newUser()), "[\"portfolio:read\", \"portfolio:deposit\"]"));

        createKey(key, "[\"portfolio:read\"]").andExpect(status().isForbidden());
        call(get("/api/api-keys"), key).andExpect(status().isForbidden());
    }

    @Test
    void manageScopeCannotBeGrantedToAKey() throws Exception {
        createKey(login(newUser()), "[\"api-keys:manage\"]").andExpect(status().isBadRequest());
    }

    @Test
    void unknownKeyIsRejected() throws Exception {
        call(get("/api/portfolios"), "ssk_not-a-real-key").andExpect(status().isUnauthorized());
    }

    @Test
    void cannotRevokeSomeoneElsesKey() throws Exception {
        String ownerLogin = login(newUser());
        String id = JsonPath.read(createKey(ownerLogin, "[\"portfolio:read\"]")
                .andReturn().getResponse().getContentAsString(), "$.id");

        call(delete("/api/api-keys/" + id), login(newUser())).andExpect(status().isNotFound());
    }

    private String newUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        userService.register(email, PASSWORD, null, null);
        return email;
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions createKey(String bearer, String scopesJson) throws Exception {
        return mvc.perform(post("/api/api-keys").header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test key\",\"scopes\":" + scopesJson + "}"));
    }

    private static String keyOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.key");
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String bearer) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + bearer));
    }

    private ResultActions deposit(String bearer, String portfolioId) throws Exception {
        return mvc.perform(post("/api/portfolios/" + portfolioId + "/deposits")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10}"));
    }
}
