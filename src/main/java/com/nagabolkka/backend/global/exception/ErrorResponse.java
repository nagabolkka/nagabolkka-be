package com.nagabolkka.backend.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "실패 응답 (모든 API 공통)")
public record ErrorResponse(

        @Schema(description = "에러 코드 (프론트는 이 값으로 분기)", example = "TERMS_REQUIRED")
        String code,

        @Schema(description = "사용자에게 보여줄 문구", example = "필수 약관에 동의해 주세요")
        String message
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    /** Security 필터처럼 컨트롤러 밖에서 직접 JSON 을 쓸 때 사용 (메시지에 따옴표가 없다는 전제) */
    public String toJson() {
        return "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
    }
}