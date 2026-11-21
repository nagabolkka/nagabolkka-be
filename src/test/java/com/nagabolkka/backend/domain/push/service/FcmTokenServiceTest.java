package com.nagabolkka.backend.domain.push.service;

import com.nagabolkka.backend.domain.push.entity.FcmToken;
import com.nagabolkka.backend.domain.push.repository.FcmTokenRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock FcmTokenRepository repository;
    @Mock UserRepository userRepository;

    @InjectMocks FcmTokenService service;

    private User user(Long id) {
        User user = User.createKakaoUser("윤지" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("처음 보는 토큰 → 새로 저장")
    void register_new() {
        User me = user(1L);
        given(userRepository.getReferenceById(1L)).willReturn(me);
        given(repository.findByToken("tok")).willReturn(Optional.empty());

        service.register(1L, "tok");

        ArgumentCaptor<FcmToken> saved = ArgumentCaptor.forClass(FcmToken.class);
        then(repository).should().save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo("tok");
        assertThat(saved.getValue().getUser()).isEqualTo(me);
    }

    @Test
    @DisplayName("이미 있는 토큰 (같은 기기 · 다른 계정) → 새로 저장 안 하고 주인만 바꿈")
    void register_existing_changeOwner() {
        User before = user(1L);
        User me = user(2L);
        FcmToken token = FcmToken.of(before, "tok");
        given(userRepository.getReferenceById(2L)).willReturn(me);
        given(repository.findByToken("tok")).willReturn(Optional.of(token));

        service.register(2L, "tok");

        assertThat(token.getUser()).isEqualTo(me);
        then(repository).should(never()).save(any());
    }

    @Test
    @DisplayName("내 토큰 해제 → 삭제")
    void unregister_mine() {
        FcmToken token = FcmToken.of(user(1L), "tok");
        given(repository.findByToken("tok")).willReturn(Optional.of(token));

        service.unregister(1L, "tok");

        then(repository).should().delete(token);
    }

    @Test
    @DisplayName("남의 토큰 · 없는 토큰 해제 → 아무것도 안 함 (에러 없이)")
    void unregister_notMine() {
        given(repository.findByToken("tok")).willReturn(Optional.of(FcmToken.of(user(1L), "tok")));
        given(repository.findByToken("none")).willReturn(Optional.empty());

        service.unregister(2L, "tok");
        service.unregister(2L, "none");

        then(repository).should(never()).delete(any());
    }
}