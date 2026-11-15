package dev.umut.wallet_api.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthSecurityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestWithoutTokenGetsProblemDetail401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void garbageAndForgedTokensAreRejected() throws Exception {
        String email = randomEmail();
        register(email);
        String token = login(email);

        // saldırı: gerçek token'ın imzasını koru, payload'daki sub'ı başkasının id'siyle değiştir
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"%s\",\"exp\":9999999999}".formatted(UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void accountOwnerComesFromTokenNotFromRequestBody() throws Exception {
        String email = randomEmail();
        String userId = register(email);
        String token = login(email);
        String someoneElse = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"TRY","ownerId":"%s"}""".formatted(someoneElse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(userId));
    }

    @Test
    void otherUserGets404ForSomeoneElsesAccount() throws Exception {
        String ownerEmail = randomEmail();
        register(ownerEmail);
        String ownerToken = login(ownerEmail);
        String accountId = openAccount(ownerToken);

        String intruderEmail = randomEmail();
        register(intruderEmail);
        String intruderToken = login(intruderEmail);

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/accounts/{id}/deposits", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void emailIsCaseInsensitiveForLoginAndDuplicateRegistration() throws Exception {
        String suffix = UUID.randomUUID().toString();
        register("Case-" + suffix + "@Test.com");

        login("CASE-" + suffix + "@test.COM");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"case-%s@test.com","password":"12345678"}""".formatted(suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    private static String randomEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"12345678"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"12345678"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String openAccount(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"TRY"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}