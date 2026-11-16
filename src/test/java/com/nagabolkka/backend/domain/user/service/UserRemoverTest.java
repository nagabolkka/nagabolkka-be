package com.nagabolkka.backend.domain.user.service;

import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserPrivateInfoRepository;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.crypto.AesUtil;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserRemoverTest {

    private static final Long USER_ID = 1L;
    private static final byte[] KAKAO_ID_ENC = {1, 2, 3};

    @Mock UserRepository userRepository;
    @Mock UserPrivateInfoRepository userPrivateInfoRepository;
    @Mock AesUtil aesUtil;

    @InjectMocks UserRemover userRemover;

    User user;

    @BeforeEach
    void setUp() {
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    @DisplayName("탈퇴 → 사용자 삭제 · flush · 카카오 회원번호 반환")
    void remove() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userPrivateInfoRepository.findKakaoIdEncByUserId(USER_ID)).willReturn(Optional.of(KAKAO_ID_ENC));
        given(aesUtil.decrypt(KAKAO_ID_ENC)).willReturn("4100000001");

        Optional<Long> kakaoId = userRemover.remove(USER_ID);

        assertThat(kakaoId).contains(4100000001L);
        then(userRepository).should().delete(user);
        then(userRepository).should().flush();
    }

    @Test
    @DisplayName("관리자 → 403 FORBIDDEN · 삭제 안 함")
    void remove_admin() {
        ReflectionTestUtils.setField(user, "role", Role.ADMIN);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userRemover.remove(USER_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        then(userRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("개인정보 행 없음 → 회원번호 없이 삭제")
    void remove_noPrivateInfo() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userPrivateInfoRepository.findKakaoIdEncByUserId(USER_ID)).willReturn(Optional.empty());

        assertThat(userRemover.remove(USER_ID)).isEmpty();
        then(userRepository).should().delete(user);
    }

    @Test
    @DisplayName("복호화 실패 → 회원번호 없이 그래도 삭제")
    void remove_decryptFail() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userPrivateInfoRepository.findKakaoIdEncByUserId(USER_ID)).willReturn(Optional.of(KAKAO_ID_ENC));
        willThrow(new IllegalStateException("복호화 실패")).given(aesUtil).decrypt(KAKAO_ID_ENC);

        assertThat(userRemover.remove(USER_ID)).isEmpty();
        then(userRepository).should().delete(user);
    }
}