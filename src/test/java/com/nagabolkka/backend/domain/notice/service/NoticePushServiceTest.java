package com.nagabolkka.backend.domain.notice.service;

import com.nagabolkka.backend.domain.notice.dto.NoticePushResponse;
import com.nagabolkka.backend.domain.push.service.PushNotifier;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.push.FcmSender;
import com.nagabolkka.backend.global.push.PushMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NoticePushServiceTest {

    @Mock NoticeService noticeService;
    @Mock PushNotifier pushNotifier;

    @InjectMocks NoticePushService noticePushService;

    @Test
    @DisplayName("보낸 시각 기록 → 그다음 발송 → 보낸 · 실패 수")
    void push() {
        PushMessage message = PushMessage.notice("서버 점검 안내", "내일 점검해요");
        given(noticeService.markPushed(3L)).willReturn(message);
        given(pushNotifier.notice(message)).willReturn(new FcmSender.Result(42, 1, List.of("old")));

        NoticePushResponse response = noticePushService.push(3L);

        assertThat(response).isEqualTo(new NoticePushResponse(42, 1));
        InOrder order = inOrder(noticeService, pushNotifier);
        order.verify(noticeService).markPushed(3L);
        order.verify(pushNotifier).notice(message);
    }

    @Test
    @DisplayName("이미 보낸 공지 (409) → 발송 안 함")
    void push_alreadyPushed() {
        given(noticeService.markPushed(3L)).willThrow(new CustomException(ErrorCode.NOTICE_ALREADY_PUSHED));

        assertThatThrownBy(() -> noticePushService.push(3L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOTICE_ALREADY_PUSHED);
        then(pushNotifier).should(never()).notice(any());
    }
}