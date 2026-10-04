package com.photobuddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobuddy.dto.auth.RegisterRequest;
import com.photobuddy.dto.location.LocationUpdateRequest;
import com.photobuddy.entity.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuddyIntegrationTest {
    private static final String PASSWORD = "PhotoBuddy!2026Secure";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void requestsAreUniqueAcrossDirectionsAndCannotBeSentToSelf() throws Exception {
        JsonNode alice = register("alice", "alice@example.test");
        JsonNode bob = register("bob", "bob@example.test");
        String aliceToken = token(alice);
        long bobId = bob.path("user").path("id").asLong();
        long aliceId = alice.path("user").path("id").asLong();

        mvc.perform(post("/api/buddy-requests/" + bobId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(post("/api/buddy-requests/" + bobId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/buddy-requests/" + aliceId).header("Authorization", bearer(token(bob))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/buddy-requests/" + aliceId).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/buddy-requests/received").header("Authorization", bearer(token(bob))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sender.username").value("alice"));
        mvc.perform(get("/api/buddy-requests/sent").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void onlyReceiverCanAcceptOrRejectAndAcceptanceCreatesOneMatchForBothUsers() throws Exception {
        JsonNode sender = register("sender", "sender@example.test");
        JsonNode receiver = register("receiver", "receiver@example.test");
        JsonNode stranger = register("stranger", "stranger@example.test");
        long requestId = send(sender, receiver);

        mvc.perform(put("/api/buddy-requests/" + requestId + "/accept")
                        .header("Authorization", bearer(token(sender))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/buddy-requests/" + requestId + "/accept")
                        .header("Authorization", bearer(token(stranger))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/buddy-requests/" + requestId + "/accept")
                        .header("Authorization", bearer(token(receiver))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));

        mvc.perform(get("/api/matches").header("Authorization", bearer(token(sender))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.username").value("receiver"));
        mvc.perform(get("/api/matches").header("Authorization", bearer(token(receiver))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.username").value("sender"));
        mvc.perform(post("/api/buddy-requests/" + receiver.path("user").path("id").asLong())
                        .header("Authorization", bearer(token(sender))))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyReceiverCanRejectAndARejectedPairCanSendANewRequest() throws Exception {
        JsonNode sender = register("rejectsender", "rejectsender@example.test");
        JsonNode receiver = register("rejectreceiver", "rejectreceiver@example.test");
        long requestId = send(sender, receiver);

        mvc.perform(put("/api/buddy-requests/" + requestId + "/reject")
                        .header("Authorization", bearer(token(sender))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/buddy-requests/" + requestId + "/reject")
                        .header("Authorization", bearer(token(receiver))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(post("/api/buddy-requests/" + sender.path("user").path("id").asLong())
                        .header("Authorization", bearer(token(receiver))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void onlySenderCanCancelAndCancelledRequestCanBeResent() throws Exception {
        JsonNode sender = register("cancelsender", "cancelsender@example.test");
        JsonNode receiver = register("cancelreceiver", "cancelreceiver@example.test");
        long requestId = send(sender, receiver);

        mvc.perform(delete("/api/buddy-requests/" + requestId).header("Authorization", bearer(token(receiver))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/buddy-requests/" + requestId).header("Authorization", bearer(token(sender))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/buddy-requests/received").header("Authorization", bearer(token(receiver))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/buddy-requests/" + receiver.path("user").path("id").asLong())
                        .header("Authorization", bearer(token(sender))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void matchDistanceIsShownOnlyWhenBothMembersShareLocation() throws Exception {
        JsonNode first = register("firstmatch", "firstmatch@example.test");
        JsonNode second = register("secondmatch", "secondmatch@example.test");
        long firstId = first.path("user").path("id").asLong();
        long secondId = second.path("user").path("id").asLong();
        setSharedLocation(first, "17.385", "78.4867");
        setSharedLocation(second, "17.395", "78.4867");
        long requestId = send(first, second);
        mvc.perform(put("/api/buddy-requests/" + requestId + "/accept")
                        .header("Authorization", bearer(token(second))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/matches").header("Authorization", bearer(token(first))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].distanceKm", closeTo(1.1, 0.1)));
        mvc.perform(put("/api/locations/toggle").header("Authorization", bearer(token(second)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locationEnabled\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/matches").header("Authorization", bearer(token(first))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].distanceKm").doesNotExist());
    }

    private long send(JsonNode sender, JsonNode receiver) throws Exception {
        String body = mvc.perform(post("/api/buddy-requests/" + receiver.path("user").path("id").asLong())
                        .header("Authorization", bearer(token(sender))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private void setSharedLocation(JsonNode user, String latitude, String longitude) throws Exception {
        String token = token(user);
        LocationUpdateRequest update = new LocationUpdateRequest(new java.math.BigDecimal(latitude),
                new java.math.BigDecimal(longitude), new java.math.BigDecimal("10"));
        mvc.perform(put("/api/locations/update").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/locations/toggle").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locationEnabled\":true}"))
                .andExpect(status().isOk());
    }

    private JsonNode register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Photo", "Buddy", username, email, PASSWORD, PASSWORD,
                Gender.PREFER_NOT_TO_SAY, null, false, null);
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String token(JsonNode user) { return user.path("accessToken").asText(); }
    private String bearer(String token) { return "Bearer " + token; }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
}
