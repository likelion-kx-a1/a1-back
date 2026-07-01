package com.likelion.a1.generation.application.port;

import com.likelion.a1.generation.domain.GenerationType;

public interface AiGenerationPort {
    Submission submit(GenerationType type, String model, String prompt);

    record Submission(boolean completed, String providerRequestId, String resultUrl) {
        public static Submission completed(String url) { return new Submission(true, null, url); }
        public static Submission pending(String id) { return new Submission(false, id, null); }
    }
}
