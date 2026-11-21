package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.domain.practice.dto.TranscribeResponse;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

// 연습 음성 → 글자 : 녹음 파일은 저장하지 않음 · 원문 로그 X · 대화 턴 · 위기 검사는 보낼 때 (기존 대화 API)
@Service
@RequiredArgsConstructor
public class PracticeVoiceService {

    static final long MAX_BYTES = 2L * 1024 * 1024; // 30초 녹음은 보통 수십 ~ 수백 KB
    static final int TEXT_MAX = 200;                // 직접 입력과 같은 최대 글자 수
    static final Set<String> AUDIO_TYPES = Set.of(
            "audio/webm", "audio/mp4", "audio/mpeg", "audio/ogg", "audio/wav", "audio/x-m4a");

    private final PracticeSessionManager sessionManager;
    private final AiClient aiClient;

    // 트랜잭션 없음 : 세션 확인 (짧은 읽기) → AI 서버 (외부 호출 동안 DB 안 잡음)
    public TranscribeResponse transcribe(Long userId, Long sessionId, MultipartFile audio) {
        String type = baseType(audio.getContentType());
        if (audio.isEmpty() || audio.getSize() > MAX_BYTES || !AUDIO_TYPES.contains(type)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        sessionManager.checkOpen(userId, sessionId); // 404 · 409

        String text = aiClient.transcribe(bytesOf(audio), filename(type), type)
                .map(AiClient.TranscribeResult::text)
                .map(String::strip)
                .filter(t -> !t.isEmpty())
                .map(t -> t.length() > TEXT_MAX ? t.substring(0, TEXT_MAX) : t)
                .orElse(null);
        return new TranscribeResponse(text);
    }

    // "audio/webm;codecs=opus" → "audio/webm"
    static String baseType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon < 0 ? contentType : contentType.substring(0, semicolon);
        return base.strip().toLowerCase(Locale.ROOT);
    }

    // OpenAI 는 파일 확장자로 형식을 봄
    static String filename(String type) {
        return switch (type) {
            case "audio/mp4", "audio/x-m4a" -> "voice.mp4"; // 아이폰 사파리
            case "audio/mpeg" -> "voice.mp3";
            case "audio/ogg" -> "voice.ogg";
            case "audio/wav" -> "voice.wav";
            default -> "voice.webm";                       // 크롬 · 안드로이드
        };
    }

    private static byte[] bytesOf(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}