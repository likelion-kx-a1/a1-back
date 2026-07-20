package com.likelion.a1.generation.application.port.out;

import java.util.Map;

public record FalGenerationSubmission(
    String externalRequestId, String statusUrl, String responseUrl, Map<String, Object> rawResponse) {}
