package com.nagabolkka.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(

        @Schema(description = "카카오 인가 코드", example = "abc123")
        @NotBlank
        String code,

        @Schema(description = "인가 요청 때 쓴 redirect_uri (서버 허용 목록과 일치해야 함)",
                example = "http://localhost:5173/oauth/kakao")
        @NotBlank
        String redirectUri,

        @Schema(description = "약관 동의 (신규 가입 때만 사용, 기존 회원은 무시)")
        Consents consents
) {

    public record Consents(
            @Schema(description = "서비스 이용약관 (필수)", example = "true") Boolean terms,
            @Schema(description = "개인정보 처리방침 (필수)", example = "true") Boolean privacy
    ) {
    }

    /** 필수 항목(terms · privacy) 에 모두 동의했는지 */
    public boolean agreedToRequiredTerms() {
        return consents != null
                && Boolean.TRUE.equals(consents.terms())
                && Boolean.TRUE.equals(consents.privacy());
    }
}