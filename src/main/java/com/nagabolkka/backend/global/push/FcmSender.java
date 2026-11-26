package com.nagabolkka.backend.global.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.google.firebase.messaging.WebpushNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FcmSender {

    static final int BATCH = 500; // FCM sendEach 한 번 최대

    public record Result(int sent, int failed, List<String> expiredTokens) {
        public static Result disabled() {
            return new Result(0, 0, List.of());
        }
    }

    private final FirebaseMessaging messaging; // 키 없으면 null
    private final String appUrl;

    public FcmSender(ObjectProvider<FirebaseMessaging> messaging, @Value("${push.app-url}") String appUrl) {
        this.messaging = messaging.getIfAvailable();
        this.appUrl = appUrl;
        if (this.messaging == null) {
            log.info("Firebase 서비스 계정 키 없음 → 푸시 알림 발송 안 함");
        }
    }

    public boolean enabled() {
        return messaging != null;
    }

    public Result send(List<String> tokens, PushMessage message) {
        if (messaging == null || tokens.isEmpty()) {
            return Result.disabled();
        }
        int sent = 0;
        int failed = 0;
        List<String> expired = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += BATCH) {
            List<String> chunk = tokens.subList(from, Math.min(from + BATCH, tokens.size()));
            try {
                BatchResponse res = messaging.sendEach(chunk.stream().map(t -> toMessage(t, message)).toList());
                List<SendResponse> each = res.getResponses();
                for (int i = 0; i < each.size(); i++) {
                    if (each.get(i).isSuccessful()) {
                        sent++;
                        continue;
                    }
                    failed++;
                    FirebaseMessagingException ex = each.get(i).getException();
                    MessagingErrorCode code = ex.getMessagingErrorCode();
                    // 실패 이유 (토큰 값은 찍지 않음)
                    log.warn("FCM 개별 실패 : code={} · {} · {}", code, ex.getErrorCode(), ex.getMessage());
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        expired.add(chunk.get(i)); // 앱 지움 · 권한 끔 · 잘못된 토큰
                    }
                }
            } catch (FirebaseMessagingException e) {
                failed += chunk.size(); // 이 묶음만 실패, 다음 묶음은 계속
                log.warn("FCM 발송 실패 ({}개) : {} · {}", chunk.size(), e.getMessagingErrorCode(), e.getMessage());
            }
        }
        return new Result(sent, failed, expired);
    }

    private Message toMessage(String token, PushMessage m) {
        return Message.builder()
                .setToken(token)
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(m.title())
                                .setBody(m.body())
                                .setIcon("/icon-192.png")
                                .setBadge("/favicon.png")
                                .build())
                        .setFcmOptions(WebpushFcmOptions.withLink(appUrl + m.path())) // 누르면 열 주소 (https 여야 함)
                        .build())
                .build();
    }
}