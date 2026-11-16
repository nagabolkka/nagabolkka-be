package com.nagabolkka.backend.domain.safety.service;

import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import com.nagabolkka.backend.domain.safety.repository.CrisisEventRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SafetyService {

    // 이 시간 안에 다시 열면 같은 방문으로 보고 저장 안 함
    static final long DUPLICATE_MINUTES = 1;

    private final UserRepository userRepository;
    private final CrisisEventRepository crisisEventRepository;
    private final Clock clock;

    // 도움 받기 직접 열기 → 사용자 · 경로 · 시각만 기록 (원문 · 위치 없음)
    @Transactional
    public void record(Long userId, CrisisSource source) {
        // 회고 · 연습 위기 감지는 서버가 수행하며 사용자가 만들 수 없음
        if (source != CrisisSource.MANUAL) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        LocalDateTime since = LocalDateTime.now(clock).minusMinutes(DUPLICATE_MINUTES);
        if (crisisEventRepository.existsByUserIdAndSourceAndCreatedAtAfter(user.getId(), CrisisSource.MANUAL, since)) {
            return; // 뒤로가기 · 다시 열기 · 개발 모드 이중 호출
        }
        crisisEventRepository.save(CrisisEvent.of(user, CrisisSource.MANUAL));
    }
}