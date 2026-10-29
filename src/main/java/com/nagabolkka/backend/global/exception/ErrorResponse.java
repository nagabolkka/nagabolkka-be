package com.nagabolkka.backend.global.exception;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public String toJson() {
        return "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
    }
}