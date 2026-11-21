package com.nagabolkka.backend.domain.notice.service;

import com.nagabolkka.backend.domain.notice.dto.NoticePushResponse;
import com.nagabolkka.backend.domain.push.service.PushNotifier;
import com.nagabolkka.backend.global.push.FcmSender;
import com.nagabolkka.backend.global.push.PushMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticePushService {

    private final NoticeService noticeService;
    private final PushNotifier pushNotifier;

    public NoticePushResponse push(Long noticeId) {
        PushMessage message = noticeService.markPushed(noticeId); // 여기서 커밋 → 연타해도 두 번째는 409
        FcmSender.Result r = pushNotifier.notice(message);       // 외부 호출 동안 DB 안 잡음
        return new NoticePushResponse(r.sent(), r.failed());
    }
}