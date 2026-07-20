package com.likelion.a1.generation.application.port.out;

import java.util.Map;

public record AiTextGenerationResult(String text, Map<String, Object> rawResponse) {}
