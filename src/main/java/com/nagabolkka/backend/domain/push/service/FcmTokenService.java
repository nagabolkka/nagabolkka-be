package com.nagabolkka.backend.domain.push.service;

import com.nagabolkka.backend.domain.push.entity.FcmToken;
import com.nagabolkka.backend.domain.push.repository.FcmTokenRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, String token) {
        User user = userRepository.getReferenceById(userId);
        repository.findByToken(token).ifPresentOrElse(
                t -> t.changeOwner(user),
                () -> repository.save(FcmToken.of(user, token)));
    }

    @Transactional
    public void unregister(Long userId, String token) {
        repository.findByToken(token)
                .filter(t -> t.getUser().getId().equals(userId)) // 남의 토큰은 못 지움 (없는 것처럼 204)
                .ifPresent(repository::delete);
    }
}