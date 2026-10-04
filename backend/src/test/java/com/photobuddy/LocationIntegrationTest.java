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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LocationIntegrationTest {
    private static final String PASSWORD = "PhotoBuddy!2026Secure";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
        void locationUpdatesAutomaticallyEnableSharingAndCanBeDisabled() throws Exception {
        String token = register("locowner", "locowner@example.test").path("accessToken").asText();

        mvc.perform(put("/api/locations/update").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LocationUpdateRequest(decimal("17.3850"), decimal("78.4867"), decimal("20.5")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(17.3850))
                .andExpect(jsonPath("$.locationEnabled").value(true));
        mvc.perform(get("/api/locations/my-location").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longitude").value(78.4867));
        mvc.perform(put("/api/locations/toggle").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locationEnabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.locationEnabled").value(false));
        mvc.perform(put("/api/locations/toggle").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locationEnabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.locationEnabled").value(true));
    }

    @Test
    void nearbySearchFiltersDisabledAndOutOfRadiusUsersSortsByDistanceAndRoundsCoordinates() throws Exception {
        String requesterToken = register("searcher", "searcher@example.test").path("accessToken").asText();
        addLocation("closer", "closer@example.test", "17.38942", "78.4867", true);
        addLocation("farther", "farther@example.test", "17.40000", "78.4867", true);
        addLocation("privateuser", "private@example.test", "17.38600", "78.4867", false);
        addLocation("outofrange", "outofrange@example.test", "17.50000", "78.4867", true);

        mvc.perform(get("/api/users/nearby").header("Authorization", bearer(requesterToken))
                        .param("latitude", "17.3850").param("longitude", "78.4867").param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("closer"))
                .andExpect(jsonPath("$[0].distanceKm", closeTo(0.49, 0.1)))
                .andExpect(jsonPath("$[0].approximateLatitude").value(17.39))
                .andExpect(jsonPath("$[0].latitude").doesNotExist())
                .andExpect(jsonPath("$[1].username").value("farther"));
    }

    @Test
    void nearbySearchHandlesLongitudeWrapAtTheInternationalDateLine() throws Exception {
        String requesterToken = register("dateline", "dateline@example.test").path("accessToken").asText();
        addLocation("acrossline", "acrossline@example.test", "0", "-179.99", true);

        mvc.perform(get("/api/users/nearby").header("Authorization", bearer(requesterToken))
                        .param("latitude", "0").param("longitude", "179.99").param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("acrossline"));
    }

    @Test
    void locationApisRequireAuthenticationAndRejectInvalidCoordinates() throws Exception {
        mvc.perform(get("/api/locations/my-location")).andExpect(status().isUnauthorized());
        String token = register("invalidloc", "invalidloc@example.test").path("accessToken").asText();
        mvc.perform(put("/api/locations/update").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":91,\"longitude\":181,\"accuracy\":-1}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/users/nearby").header("Authorization", bearer(token))
                        .param("latitude", "17").param("longitude", "78").param("radius", "0"))
                .andExpect(status().isUnprocessableEntity());
    }

    private void addLocation(String username, String email, String latitude, String longitude,
                             boolean share) throws Exception {
        String token = register(username, email).path("accessToken").asText();
        mvc.perform(put("/api/locations/update").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LocationUpdateRequest(decimal(latitude), decimal(longitude), decimal("15")))))
                .andExpect(status().isOk());
        if (share) {
            mvc.perform(put("/api/locations/toggle").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"locationEnabled\":true}"))
                    .andExpect(status().isOk());
        } else {
            mvc.perform(put("/api/locations/toggle").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"locationEnabled\":false}"))
                    .andExpect(status().isOk());
        }
    }

    private JsonNode register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Photo", "Buddy", username, email, PASSWORD, PASSWORD,
                Gender.PREFER_NOT_TO_SAY, null, false, null);
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String bearer(String token) { return "Bearer " + token; }
    private java.math.BigDecimal decimal(String value) { return new java.math.BigDecimal(value); }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
}
