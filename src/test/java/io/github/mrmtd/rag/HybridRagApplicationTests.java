package io.github.mrmtd.rag;

import io.github.mrmtd.rag.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full Spring context wires up with placeholder credentials.
 * No request is sent, so Qdrant and OpenAI are never contacted.
 */
@SpringBootTest(properties = {
        "qdrant.uri=https://qdrant.test",
        "qdrant.api-key=test-qdrant-key",
        "openai.api-key=test-openai-key"
})
class HybridRagApplicationTests {

    @Autowired
    private RagService ragService;

    @Test
    void contextLoads() {
        assertThat(ragService).isNotNull();
    }
}
