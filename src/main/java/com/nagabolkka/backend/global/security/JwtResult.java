package com.nagabolkka.backend.global.security;

public record JwtResult(Status status, Long userId) {

    public enum Status {
        VALID,
        EXPIRED,
        INVALID
    }

    public static JwtResult valid(Long userId) {
        return new JwtResult(Status.VALID, userId);
    }

    public static JwtResult expired() {
        return new JwtResult(Status.EXPIRED, null);
    }

    public static JwtResult invalid() {
        return new JwtResult(Status.INVALID, null);
    }
}