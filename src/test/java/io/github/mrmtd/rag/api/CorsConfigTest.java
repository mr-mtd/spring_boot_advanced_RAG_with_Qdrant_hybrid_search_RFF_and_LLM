package io.github.mrmtd.rag.api;

import io.github.mrmtd.rag.dto.AskResponse;
import io.github.mrmtd.rag.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks that CORS headers are sent only for the configured origins. */
@WebMvcTest(AskController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173, https://frontend.example.com/")
class CorsConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void preflightFromAllowedOriginSucceeds() throws Exception {
        mvc.perform(options("/api/ask")
                        .header(HttpHeaders.ORIGIN, "https://frontend.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.example.com"));
    }

    @Test
    void postFromAllowedOriginGetsAllowOriginHeader() throws Exception {
        when(ragService.ask("hi")).thenReturn(new AskResponse("ok", List.of()));

        mvc.perform(post("/api/ask")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        mvc.perform(options("/api/ask")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
