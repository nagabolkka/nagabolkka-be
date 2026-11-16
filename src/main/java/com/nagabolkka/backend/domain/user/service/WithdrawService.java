package com.nagabolkka.backend.domain.user.service;

import com.nagabolkka.backend.domain.auth.kakao.KakaoOAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRemover userRemover;
    private final KakaoOAuthClient kakaoOAuthClient;

    // 트랜잭션 없음 : DB 삭제가 커밋된 뒤에 카카오를 부름
    public void withdraw(Long userId) {
        userRemover.remove(userId).ifPresent(kakaoOAuthClient::unlink);
    }
}