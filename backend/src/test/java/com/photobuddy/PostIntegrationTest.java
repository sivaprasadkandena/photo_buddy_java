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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostIntegrationTest {
    private static final String PASSWORD = "PhotoBuddy!2026Secure";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void createsPagedPostFeedAndTracksLikeAndUnlike() throws Exception {
        JsonNode alice = register("postalice", "postalice@example.test");
        JsonNode bob = register("postbob", "postbob@example.test");
        String bobToken = token(bob);
        long postId = createPost(token(alice), "A morning walk", "NATURE");

        mvc.perform(get("/api/posts?page=0&size=1").header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].postId").value(postId))
                .andExpect(jsonPath("$.content[0].user.username").value("postalice"))
                .andExpect(jsonPath("$.content[0].imageUrl").value(containsString("/api/files/")))
                .andExpect(jsonPath("$.content[0].likeCount").value(0))
                .andExpect(jsonPath("$.content[0].likedByMe").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true))
                .andExpect(jsonPath("$.likedByMe").value(true));
        mvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", bearer(bobToken)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/posts/" + postId).header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likedByCurrentUser").value(true));
        mvc.perform(delete("/api/posts/" + postId + "/like").header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));
    }

    @Test
    void commentsCanBeCreatedReadAndDeletedOnlyByTheirOwner() throws Exception {
        JsonNode alice = register("commentalice", "commentalice@example.test");
        JsonNode bob = register("commentbob", "commentbob@example.test");
        JsonNode charlie = register("commentcharlie", "commentcharlie@example.test");
        long postId = createPost(token(alice), "", "TRAVEL");

        String body = mvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", bearer(token(bob))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  Beautiful view!  \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.content").value("Beautiful view!"))
                .andReturn().getResponse().getContentAsString();
        long commentId = mapper.readTree(body).path("id").asLong();

        mvc.perform(get("/api/posts/" + postId + "/comments?page=0&size=10")
                        .header("Authorization", bearer(token(alice))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].user.username").value("commentbob"));
        mvc.perform(delete("/api/comments/" + commentId).header("Authorization", bearer(token(charlie))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/comments/" + commentId).header("Authorization", bearer(token(bob))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/posts/" + postId).header("Authorization", bearer(token(alice))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.commentCount").value(0));
    }

    @Test
    void onlyPostOwnerCanDeleteAndImageUploadRejectsUnsupportedData() throws Exception {
        JsonNode alice = register("deletealice", "deletealice@example.test");
        JsonNode bob = register("deletebob", "deletebob@example.test");
        long postId = createPost(token(alice), "", "STREET");
        mvc.perform(delete("/api/posts/" + postId).header("Authorization", bearer(token(bob))))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/posts").file(new MockMultipartFile("image", "fake.gif", "image/gif", "GIF89a".getBytes(StandardCharsets.UTF_8)))
                        .param("style", "OTHER").header("Authorization", bearer(token(alice))))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(delete("/api/posts/" + postId).header("Authorization", bearer(token(alice))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/posts/" + postId).header("Authorization", bearer(token(bob))))
                .andExpect(status().isNotFound());
    }

    private long createPost(String authToken, String caption, String style) throws Exception {
        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        String body = mvc.perform(multipart("/api/posts")
                        .file(new MockMultipartFile("image", "photo.jpg", "image/jpeg", jpeg))
                        .param("caption", caption).param("style", style).header("Authorization", bearer(authToken)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.style").value(style))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("postId").asLong();
    }

    private JsonNode register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Photo", "Buddy", username, email, PASSWORD, PASSWORD,
                Gender.PREFER_NOT_TO_SAY, null, false, null);
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }
    private String token(JsonNode user) { return user.path("accessToken").asText(); }
    private String bearer(String token) { return "Bearer " + token; }
}
