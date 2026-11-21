package com.nagabolkka.backend.domain.practice.controller;

import com.nagabolkka.backend.domain.practice.dto.FinishRequest;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse;
import com.nagabolkka.backend.domain.practice.dto.ScenariosResponse;
import com.nagabolkka.backend.domain.practice.dto.StartPracticeRequest;
import com.nagabolkka.backend.domain.practice.dto.StartPracticeResponse;
import com.nagabolkka.backend.domain.practice.dto.TranscribeResponse;
import com.nagabolkka.backend.domain.practice.dto.TurnRequest;
import com.nagabolkka.backend.domain.practice.dto.TurnResponse;
import com.nagabolkka.backend.domain.practice.service.PracticeService;
import com.nagabolkka.backend.domain.practice.service.PracticeVoiceService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "연습실", description = "AI 상대역과 3턴 대화 연습 · 피드백 · 연습 조각")
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private static final String NOT_FOUND_EX = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}";
    private static final String CLOSED_EX = "{\"code\":\"PRACTICE_SESSION_CLOSED\",\"message\":\"이미 끝난 연습이에요\"}";
    private static final String INVALID_EX = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}";

    private final PracticeService practiceService;
    private final PracticeVoiceService practiceVoiceService;

    @Operation(summary = "연습 시나리오",
            description = "시나리오 3개와 오늘 남은 연습 횟수입니다. (하루 3회 · 시작 기준 · KST 00:00 초기화)")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ScenariosResponse.class)))
    @GetMapping("/scenarios")
    public ScenariosResponse getScenarios(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return practiceService.getScenarios(userId);
    }

    @Operation(summary = "연습 시작",
            description = "세션을 만들고 오늘 횟수를 1회 씁니다. (중간에 나가도 돌려주지 않음) opening 을 첫 대화로 보여 주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시작",
                    content = @Content(schema = @Schema(implementation = StartPracticeResponse.class))),
            @ApiResponse(responseCode = "403", description = "플러스 전용 시나리오",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "PRACTICE_PLUS_ONLY", value = "{\"code\":\"PRACTICE_PLUS_ONLY\",\"message\":\"루미 플러스에서 열리는 연습이에요\"}"))),
            @ApiResponse(responseCode = "404", description = "없는 시나리오",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = NOT_FOUND_EX))),
            @ApiResponse(responseCode = "409", description = "하루 3회 초과 · 시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "PRACTICE_DAILY_LIMIT", value = "{\"code\":\"PRACTICE_DAILY_LIMIT\",\"message\":\"오늘 연습은 여기까지예요\"}"),
                            @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")
                    }))
    })
    @PostMapping("/sessions")
    public StartPracticeResponse start(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody StartPracticeRequest request) {
        return practiceService.start(userId, request);
    }

    @Operation(summary = "연습 대화",
            description = "내 말 → 상대역 답변입니다. 턴 수는 서버가 셉니다. (history 길이는 믿지 않음) \n\n"
                    + "위기 표현이면 {\"crisisDetected\": true} 만 오고 세션이 멈춥니다. → 도움 받기 (?from=practice) \n\n"
                    + "AI 가 응답하지 않으면 고정 대사입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 · 위기 감지",
                    content = @Content(schema = @Schema(implementation = TurnResponse.class))),
            @ApiResponse(responseCode = "400", description = "message 빈 값 · 200자 초과 · history 7개 초과 · text 300자 초과 · role 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = INVALID_EX))),
            @ApiResponse(responseCode = "404", description = "없는 세션 · 다른 사람 세션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = NOT_FOUND_EX))),
            @ApiResponse(responseCode = "409", description = "끝났거나 멈춘 세션 · 최대 턴 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "PRACTICE_SESSION_CLOSED", value = CLOSED_EX),
                            @ExampleObject(name = "PRACTICE_TURN_LIMIT", value = "{\"code\":\"PRACTICE_TURN_LIMIT\",\"message\":\"대화 횟수를 다 썼어요\"}")
                    }))
    })
    @PostMapping("/sessions/{sessionId}/turns")
    public TurnResponse turn(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                             @Parameter(description = "세션 id", example = "55") @PathVariable Long sessionId,
                             @Valid @RequestBody TurnRequest request) {
        return practiceService.turn(userId, sessionId, request);
    }

    @Operation(summary = "연습 끝",
            description = "피드백 · 연습 조각 · 실제 미션 제안입니다. 1턴 이상 한 세션만 끝낼 수 있습니다. \n\n"
                    + "연습 조각은 현재 계단과 무관하게 지급되고 복원도는 오르지 않습니다. \n\n"
                    + "followMission.missionId 는 퀘스트 동작 PRACTICE_MISSION 에 쓰세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "끝",
                    content = @Content(schema = @Schema(implementation = FinishResponse.class))),
            @ApiResponse(responseCode = "400", description = "history 0개 · 7개 초과 · text 300자 초과 · role 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = INVALID_EX))),
            @ApiResponse(responseCode = "404", description = "없는 세션 · 다른 사람 세션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = NOT_FOUND_EX))),
            @ApiResponse(responseCode = "409", description = "0턴 · 이미 끝났거나 멈춘 세션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "PRACTICE_SESSION_CLOSED", value = CLOSED_EX)))
    })
    @PostMapping("/sessions/{sessionId}/finish")
    public FinishResponse finish(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                 @Parameter(description = "세션 id", example = "55") @PathVariable Long sessionId,
                                 @Valid @RequestBody FinishRequest request) {
        return practiceService.finish(userId, sessionId, request);
    }

    // 음성 → 글자
    @Operation(summary = "연습 음성 → 글자",
            description = "녹음 파일 (audio · 2MB · 30초 이하)을 글자로 바꿔 돌려줍니다. 대화 턴은 쓰지 않습니다. "
                    + "(보낼 때는 기존 연습 대화 API) 녹음 파일은 저장하지 않습니다. \n\n"
                    + "형식 : audio/webm · audio/mp4 · audio/mpeg · audio/ogg · audio/wav \n\n"
                    + "못 알아들었거나 AI 가 응답하지 않으면 {\"text\": null} → 다시 말하기 · 직접 입력")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "글자 (못 알아들으면 null)",
                    content = @Content(schema = @Schema(implementation = TranscribeResponse.class))),
            @ApiResponse(responseCode = "400", description = "파일 없음 · 빈 파일 · 2MB 초과 · 오디오 형식 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = INVALID_EX))),
            @ApiResponse(responseCode = "404", description = "없는 세션 · 다른 사람 세션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = NOT_FOUND_EX))),
            @ApiResponse(responseCode = "409", description = "끝났거나 멈춘 세션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "PRACTICE_SESSION_CLOSED", value = CLOSED_EX)))
    })
    @PostMapping(value = "/sessions/{sessionId}/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscribeResponse transcribe(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                         @Parameter(description = "세션 id", example = "55") @PathVariable Long sessionId,
                                         @RequestPart("audio") MultipartFile audio) {
        return practiceVoiceService.transcribe(userId, sessionId, audio);
    }
}