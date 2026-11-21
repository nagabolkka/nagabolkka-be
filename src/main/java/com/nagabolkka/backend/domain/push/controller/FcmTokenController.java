package com.nagabolkka.backend.domain.push.controller;

import com.nagabolkka.backend.domain.push.dto.FcmTokenRequest;
import com.nagabolkka.backend.domain.push.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "FCM 토큰 등록 · 해제")
@RestController
@RequestMapping("/api/push/tokens")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService service;

    @Operation(summary = "FCM 토큰 등록 (같은 기기는 덮어씀 · 앱을 열 때마다 불러도 됨)")
    @PostMapping
    public ResponseEntity<Void> register(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody FcmTokenRequest request) {
        service.register(userId, request.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "FCM 토큰 해제 (이 기기만)")
    @DeleteMapping
    public ResponseEntity<Void> unregister(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody FcmTokenRequest request) {
        service.unregister(userId, request.token());
        return ResponseEntity.noContent().build();
    }
}