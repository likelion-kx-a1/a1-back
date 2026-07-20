package com.likelion.a1.generation.application.port.out;

import java.util.Map;

public record FalGenerationStatus(String status, Map<String, Object> rawResponse) {}
