package com.nagabolkka.backend.domain.push.batch;

import com.nagabolkka.backend.domain.push.service.PushNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestPushJob {

    private final PushNotifier pushNotifier;

    @Scheduled(cron = "${push.quest-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void run() {
        pushNotifier.questArrived();
    }
}