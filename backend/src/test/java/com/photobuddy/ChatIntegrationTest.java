package com.photobuddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobuddy.dto.auth.RegisterRequest;
import com.photobuddy.entity.Gender;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatIntegrationTest {
    private static final String PASSWORD = "PhotoBuddy!2026Secure";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void matchedMembersCanReuseRoomSendImageAndReadPagedHistory() throws Exception {
        JsonNode alice = register("chatalice", "chatalice@example.test");
        JsonNode bob = register("chatbob", "chatbob@example.test");
        JsonNode stranger = register("chatstranger", "chatstranger@example.test");
        String aliceToken = token(alice);
        String bobToken = token(bob);
        String strangerToken = token(stranger);
        long bobId = bob.path("user").path("id").asLong();

        mvc.perform(post("/api/v1/chat/rooms/" + bobId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isForbidden());

        String requestBody = mvc.perform(post("/api/v1/buddy-requests/" + bobId)
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long requestId = objectMapper.readTree(requestBody).path("id").asLong();
        mvc.perform(put("/api/v1/buddy-requests/" + requestId + "/accept")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk());

        String roomBody = mvc.perform(post("/api/v1/chat/rooms/" + bobId)
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buddy.username").value("chatbob"))
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(roomBody).path("roomId").asLong();

        mvc.perform(post("/api/v1/chat/rooms/" + alice.path("user").path("id").asLong())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(roomId));
        mvc.perform(get("/api/v1/chat/rooms").header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(get("/api/v1/chat/rooms/" + roomId + "/messages")
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/chat/rooms/" + roomId + "/messages")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello from REST\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Hello from REST"))
                .andExpect(jsonPath("$.sender.username").value("chatbob"));

        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        mvc.perform(multipart("/api/v1/chat/rooms/" + roomId + "/messages/image")
                        .file(new MockMultipartFile("image", "shared.jpg", "image/jpeg", jpeg))
                        .param("text", "  See you there  ")
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("See you there"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());

        mvc.perform(get("/api/v1/chat/rooms/" + roomId + "/messages?page=0&size=10")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].roomId").value(roomId))
                .andExpect(jsonPath("$.content[0].sender.username").value("chatalice"))
                .andExpect(jsonPath("$.content[0].text").value("See you there"))
                .andExpect(jsonPath("$.content[1].sender.username").value("chatbob"))
                .andExpect(jsonPath("$.content[1].text").value("Hello from REST"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private JsonNode register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Photo", "Buddy", username, email, PASSWORD, PASSWORD,
                Gender.PREFER_NOT_TO_SAY, null, false, null);
        String body = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String token(JsonNode user) { return user.path("accessToken").asText(); }
    private String bearer(String token) { return "Bearer " + token; }
}
