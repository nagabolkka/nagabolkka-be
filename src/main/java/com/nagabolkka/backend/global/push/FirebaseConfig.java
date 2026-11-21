package com.nagabolkka.backend.global.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    // 서비스 계정 키가 있을 때만 FirebaseMessaging 빈 생성 (없으면 FcmSender 가 발송 건너뜀)
    @Bean
    @ConditionalOnExpression("!'${push.firebase-credentials:}'.isBlank()")
    public FirebaseMessaging firebaseMessaging(@Value("${push.firebase-credentials}") String base64) throws IOException {
        byte[] json = Base64.getDecoder().decode(base64.trim());
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json));
        FirebaseApp app = FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build())
                : FirebaseApp.getInstance();
        log.info("Firebase 메시징 준비 완료");
        return FirebaseMessaging.getInstance(app);
    }
}