package io.github.mrmtd.rag.api;

import io.github.mrmtd.rag.dto.AskResponse;
import io.github.mrmtd.rag.dto.Source;
import io.github.mrmtd.rag.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer tests for {@link AskController}; {@link RagService} is a Mockito mock. */
@WebMvcTest(AskController.class)
class AskControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void returnsAnswerAndSources() throws Exception {
        when(ragService.ask("How do I retry?")).thenReturn(
                new AskResponse("Use a backoff [Source 1].", List.of(new Source("42", "Retry question"))));

        mvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How do I retry?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use a backoff [Source 1]."))
                .andExpect(jsonPath("$.sources[0].id").value("42"))
                .andExpect(jsonPath("$.sources[0].question").value("Retry question"));

        verify(ragService).ask("How do I retry?");
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).ask(anyString());
    }

    @Test
    void rejectsMissingQuestion() throws Exception {
        mvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).ask(anyString());
    }

    @Test
    void rejectsQuestionOver4000Characters() throws Exception {
        String tooLong = "x".repeat(4001);
        mvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).ask(anyString());
    }

    @Test
    void acceptsQuestionOfExactly4000Characters() throws Exception {
        String max = "x".repeat(4000);
        when(ragService.ask(max)).thenReturn(new AskResponse("ok", List.of()));

        mvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + max + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("ok"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).ask(anyString());
    }
}
