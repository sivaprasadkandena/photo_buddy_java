package com.photobuddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobuddy.dto.auth.RegisterRequest;
import com.photobuddy.entity.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationIntegrationTest {
    private static final String PASSWORD = "PhotoBuddy!2026Secure";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void buddyRequestAndAcceptanceCreateNotificationsThatCanBeMarkedRead() throws Exception {
        JsonNode alice = register("alicenotifier", "alice.notify@example.test");
        JsonNode bob = register("bobnotifier", "bob.notify@example.test");

        String requestBody = mvc.perform(post("/api/buddy-requests/" + bob.path("user").path("id").asLong())
                        .header("Authorization", bearer(token(alice))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long requestId = objectMapper.readTree(requestBody).path("id").asLong();

        mvc.perform(get("/api/notifications").header("Authorization", bearer(token(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("BUDDY_REQUEST"))
                .andExpect(jsonPath("$.content[0].message").value("alicenotifier sent you a buddy request"));

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(token(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(put("/api/buddy-requests/" + requestId + "/accept")
                        .header("Authorization", bearer(token(bob))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/notifications").header("Authorization", bearer(token(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("BUDDY_ACCEPTED"));

        mvc.perform(put("/api/notifications/read-all").header("Authorization", bearer(token(alice))))
                .andExpect(status().isOk());
    }

    private JsonNode register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Photo", "Buddy", username, email, PASSWORD, PASSWORD,
                Gender.PREFER_NOT_TO_SAY, null, false, null);
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String token(JsonNode user) { return user.path("accessToken").asText(); }
    private String bearer(String token) { return "Bearer " + token; }
}
