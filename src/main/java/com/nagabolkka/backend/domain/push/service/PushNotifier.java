package com.nagabolkka.backend.domain.push.service;

import com.nagabolkka.backend.domain.push.repository.FcmTokenRepository;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.global.push.FcmSender;
import com.nagabolkka.backend.global.push.PushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotifier {

    private final FcmTokenRepository tokenRepository;
    private final FcmSender sender;
    private final Clock clock;

    // 트랜잭션 없음 : 외부 호출 동안 DB 잡지 않음
    public void questArrived() {
        if (!sender.enabled()) return;
        List<String> tokens = tokenRepository.findQuestTargetTokens(Role.USER, LocalDate.now(clock),
                List.of(QuestStatus.COMPLETED, QuestStatus.SKIPPED));
        send(tokens, PushMessage.quest(), "오늘의 퀘스트");
    }

    public void letterArrived(Collection<Long> userIds) {
        if (!sender.enabled() || userIds.isEmpty()) return;
        send(tokenRepository.findTokensByUserIds(userIds), PushMessage.letter(), "루미의 편지");
    }

    // 관리자 공지 알림 (서버 점검 등) — 알림 켠 일반 사용자 전부
    public FcmSender.Result notice(PushMessage message) {
        if (!sender.enabled()) {
            log.info("푸시 공지 : Firebase 키 없음 → 보내지 않음");
            return FcmSender.Result.disabled();
        }
        return send(tokenRepository.findTokensByRole(Role.USER), message, "공지");
    }

    private FcmSender.Result send(List<String> tokens, PushMessage message, String label) {
        FcmSender.Result r = sender.send(tokens, message);
        if (!r.expiredTokens().isEmpty()) {
            tokenRepository.deleteAllByTokenIn(r.expiredTokens());
        }
        log.info("푸시 {} : 보냄 {} · 실패 {} · 만료 삭제 {}", label, r.sent(), r.failed(), r.expiredTokens().size());
        return r;
    }
}