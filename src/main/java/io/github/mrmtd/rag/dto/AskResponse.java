package io.github.mrmtd.rag.dto;

import java.util.List;

public record AskResponse(String answer, List<Source> sources) {
}
