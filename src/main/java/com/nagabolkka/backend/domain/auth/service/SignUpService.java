package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.kakao.KakaoUserInfo;
import com.nagabolkka.backend.domain.user.entity.ConsentType;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.entity.UserConsent;
import com.nagabolkka.backend.domain.user.entity.UserPrivateInfo;
import com.nagabolkka.backend.domain.user.repository.UserConsentRepository;
import com.nagabolkka.backend.domain.user.repository.UserPrivateInfoRepository;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.crypto.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserRepository userRepository;
    private final UserPrivateInfoRepository userPrivateInfoRepository;
    private final UserConsentRepository userConsentRepository;
    private final AesUtil aesUtil;

    @Transactional
    public User signUp(KakaoUserInfo kakaoUser, String kakaoIdHash) {
        User user = userRepository.save(User.createKakaoUser(kakaoUser.nickname()));

        byte[] kakaoIdEnc = aesUtil.encrypt(String.valueOf(kakaoUser.kakaoId()));
        userPrivateInfoRepository.save(UserPrivateInfo.of(user, kakaoIdHash, kakaoIdEnc));

        LocalDateTime now = LocalDateTime.now();
        userConsentRepository.saveAll(
                Arrays.stream(ConsentType.values())
                        .map(type -> UserConsent.agree(user, type, now))
                        .toList()
        );
        return user;
    }
}