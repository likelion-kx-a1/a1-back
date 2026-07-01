package com.likelion.a1.generation.application.port;

public interface VideoStatusPort {
    Result poll(String model, String requestId);

    record Result(State state, String storedUrl) {}

    enum State {
        PROCESSING, COMPLETED
    }
}
