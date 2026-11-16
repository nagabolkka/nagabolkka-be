package com.nagabolkka.backend.domain.user.service;

import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserPrivateInfoRepository;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.crypto.AesUtil;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRemover {

    private final UserRepository userRepository;
    private final UserPrivateInfoRepository userPrivateInfoRepository;
    private final AesUtil aesUtil;

    // 사용자 삭제 → DB ON DELETE CASCADE 로 기록 전부 삭제 (location_usage_logs 는 FK 없어서 남음)
    // 반환 : 카카오 회원번호 (연결 끊기용 · 없거나 복호화 실패면 empty)
    @Transactional
    public Optional<Long> remove(Long userId) {
        // 탈퇴 중 홈 · 퀘스트 요청이 새 행을 만들지 않도록 사용자 행 잠금
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() == Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN); // 관리자는 DB 에서 직접
        }

        // 삭제 전에 회원번호를 꺼내 둠 (삭제 후엔 user_private_info 도 없음)
        Long kakaoId = userPrivateInfoRepository.findKakaoIdEncByUserId(userId)
                .map(this::decryptKakaoId)
                .orElse(null);

        userRepository.delete(user);
        userRepository.flush(); // 여기서 DELETE 실행 → FK 문제면 카카오 끊기 전에 실패 (롤백)
        return Optional.ofNullable(kakaoId);
    }

    // 복호화 실패 (키가 바뀐 경우 등) → 연결 끊기만 건너뛰고 탈퇴는 진행
    private Long decryptKakaoId(byte[] kakaoIdEnc) {
        try {
            return Long.valueOf(aesUtil.decrypt(kakaoIdEnc));
        } catch (RuntimeException e) {
            log.warn("카카오 회원번호 복호화 실패: {}", e.getClass().getSimpleName());
            return null;
        }
    }
}