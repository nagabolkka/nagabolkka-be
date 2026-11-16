package com.nagabolkka.backend.domain.user.service;

import com.nagabolkka.backend.domain.auth.kakao.KakaoOAuthClient;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WithdrawServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long KAKAO_ID = 4100000001L;

    @Mock UserRemover userRemover;
    @Mock KakaoOAuthClient kakaoOAuthClient;

    @InjectMocks WithdrawService withdrawService;

    @Test
    @DisplayName("DB 삭제 뒤에 카카오 연결 끊기")
    void withdraw() {
        given(userRemover.remove(USER_ID)).willReturn(Optional.of(KAKAO_ID));

        withdrawService.withdraw(USER_ID);

        InOrder order = inOrder(userRemover, kakaoOAuthClient);
        order.verify(userRemover).remove(USER_ID);
        order.verify(kakaoOAuthClient).unlink(KAKAO_ID);
    }

    @Test
    @DisplayName("회원번호 없음 (복호화 실패 등) → 연결 끊기 안 부름 · 탈퇴는 끝")
    void withdraw_noKakaoId() {
        given(userRemover.remove(USER_ID)).willReturn(Optional.empty());

        withdrawService.withdraw(USER_ID);

        then(kakaoOAuthClient).should(never()).unlink(anyLong());
    }

    @Test
    @DisplayName("관리자 (403) → 카카오 안 부름")
    void withdraw_admin() {
        given(userRemover.remove(USER_ID)).willThrow(new CustomException(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> withdrawService.withdraw(USER_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        then(kakaoOAuthClient).should(never()).unlink(anyLong());
    }
}