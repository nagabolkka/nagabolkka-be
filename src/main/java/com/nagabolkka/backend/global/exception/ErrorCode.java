package com.nagabolkka.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요"),
    TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해 주세요"),
    INVALID_REDIRECT_URI(HttpStatus.BAD_REQUEST, "허용되지 않은 redirectUri 예요"),
    SPLIT_STEP_REQUIRED(HttpStatus.BAD_REQUEST, "한 단계 이상 해낸 뒤 완료할 수 있어요"),

    // 401
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했어요. 다시 시도해 주세요"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요"),
    PRACTICE_PLUS_ONLY(HttpStatus.FORBIDDEN, "루미 플러스에서 열리는 연습이에요"),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없어요"),

    // 409
    ONBOARDING_ALREADY_DONE(HttpStatus.CONFLICT, "이미 시작 체크를 마쳤어요"),
    QUEST_CHANGE_LIMIT(HttpStatus.CONFLICT, "오늘은 더 바꿀 수 없어요"),
    QUEST_ALREADY_DONE(HttpStatus.CONFLICT, "이미 끝난 퀘스트예요"),
    REGION_NOT_SUGGESTED(HttpStatus.CONFLICT, "지금은 다음 지역으로 갈 수 없어요"),
    PRACTICE_DAILY_LIMIT(HttpStatus.CONFLICT, "오늘 연습은 여기까지예요"),
    PRACTICE_TURN_LIMIT(HttpStatus.CONFLICT, "대화 횟수를 다 썼어요"),
    PRACTICE_SESSION_CLOSED(HttpStatus.CONFLICT, "이미 끝난 연습이에요"),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해 주세요");

    private final HttpStatus status;
    private final String message;
}
