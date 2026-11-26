package com.example.hybridrag.api;

import com.example.hybridrag.service.RagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AskController {
    private final RagService ragService;

    public AskController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return ragService.ask(request.question());
    }

    public record AskRequest(@NotBlank @Size(max = 4000) String question) { }
    public record Source(String id, String question) { }
    public record AskResponse(String answer, List<Source> sources) { }
}
