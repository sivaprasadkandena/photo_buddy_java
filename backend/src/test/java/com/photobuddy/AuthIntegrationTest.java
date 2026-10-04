package com.photobuddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobuddy.dto.auth.RegisterRequest;
import com.photobuddy.dto.user.UpdateProfileRequest;
import com.photobuddy.entity.Gender;
import com.photobuddy.repository.UserRepository;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {
    private static final String PASSWORD = "PhotoBuddy!2026Secure";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
        @Autowired CustomUserDetailsService userDetailsService;

    @Test
    void registrationHashesPasswordAndReturnsJwtWithoutSensitiveFields() throws Exception {
        String email = "register@example.test";
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("registerer", email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(users.findByEmail(email).orElseThrow().getPassword()).isNotEqualTo(PASSWORD);
        JsonNode body = objectMapper.readTree(response);
        String accessToken = body.path("accessToken").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("registerer"));
    }

    @Test
    void versionedAuthenticationRoutesRemainCompatibleWithTheExistingContract() throws Exception {
        String response = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("versioned", "versioned@example.test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(response).path("accessToken").asText();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("versioned"));
    }

    @Test
    void duplicateEmailAndUsernameAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("first", "same@example.test"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("second", "same@example.test"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("first", "different@example.test"))))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRejectsBadPasswordAndAcceptsValidCredentials() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("logintest", "login@example.test"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.test\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"LOGIN@example.test\",\"password\":\"PhotoBuddy!2026Secure\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

        @Test
        void userDetailsCanResolveUsernameClaimUsedByChatWebSocket() throws Exception {
                register("chatuser", "chatuser@example.test");

                AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername("chatuser");

                assertThat(user.getUsername()).isEqualTo("chatuser");
        }

    @Test
    void refreshRotatesRefreshTokenAndLogoutRevokesTheNewToken() throws Exception {
        String registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("rotator", "rotate@example.test"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String oldRefresh = objectMapper.readTree(registered).path("refreshToken").asText();

        String refreshed = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newRefresh = objectMapper.readTree(refreshed).path("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(oldRefresh);

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", newRefresh))))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", newRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateEndpointsRejectMissingOrInvalidTokens() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesPasswordStrengthAndConfirmation() throws Exception {
        RegisterRequest mismatch = new RegisterRequest("Photo", "Buddy", "mismatch", "mismatch@example.test",
                PASSWORD, "NotTheSame!2026", Gender.OTHER, null, false, null);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(mismatch)))
                .andExpect(status().isBadRequest());

        RegisterRequest weak = new RegisterRequest("Photo", "Buddy", "weakpassword", "weak@example.test",
                "short", "short", Gender.OTHER, null, false, null);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(weak)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void profileCanBeReadByUsernameOrIdWithoutExposingPrivateFields() throws Exception {
        JsonNode registration = register("profileuser", "profile@example.test");
        long id = registration.path("user").path("id").asLong();
        String token = registration.path("accessToken").asText();

        mvc.perform(get("/api/users/profile/PROFILEUSER").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("profileuser"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/users/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Photo"));
    }

    @Test
    void userCanUpdateTheirOwnProfile() throws Exception {
        JsonNode registration = register("profileedit", "profileedit@example.test");
        String token = registration.path("accessToken").asText();
        UpdateProfileRequest update = new UpdateProfileRequest("Siva", "Photographer", "Street and travel",
                Gender.OTHER, true, "https://images.example.test/profile.png");

        mvc.perform(put("/api/users/profile").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Siva"))
                .andExpect(jsonPath("$.bio").value("Street and travel"))
                .andExpect(jsonPath("$.isPhotographer").value(true));
    }

    @Test
    void profileUpdateAlwaysUsesAuthenticatedUserAndCannotChangeAnotherUser() throws Exception {
        JsonNode victim = register("victim", "victim@example.test");
        JsonNode attacker = register("attacker", "attacker@example.test");
        String attackerBody = "{\"id\":" + victim.path("user").path("id").asLong()
                + ",\"firstName\":\"Changed\",\"lastName\":\"By attacker\",\"bio\":\"attempt\","
                + "\"gender\":\"OTHER\",\"isPhotographer\":true,\"profilePicture\":null}";

        mvc.perform(put("/api/users/profile").header("Authorization", "Bearer " + attacker.path("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(attackerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attacker.path("user").path("id").asLong()))
                .andExpect(jsonPath("$.firstName").value("Changed"));

        mvc.perform(get("/api/users/profile/victim").header("Authorization", "Bearer " + victim.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Photo"));
    }

    private RegisterRequest registerRequest(String username, String email) {
        return new RegisterRequest("Photo", "Buddy", username, email, PASSWORD, PASSWORD,
                Gender.PREFER_NOT_TO_SAY, null, false, null);
    }

    private JsonNode register(String username, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest(username, email))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
}
