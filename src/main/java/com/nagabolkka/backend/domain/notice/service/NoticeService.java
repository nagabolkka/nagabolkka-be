package com.nagabolkka.backend.domain.notice.service;

import com.nagabolkka.backend.domain.notice.dto.ActiveNoticeResponse;
import com.nagabolkka.backend.domain.notice.dto.ActiveNoticeResponse.ActiveNotice;
import com.nagabolkka.backend.domain.notice.dto.NoticeCreateRequest;
import com.nagabolkka.backend.domain.notice.dto.NoticeItem;
import com.nagabolkka.backend.domain.notice.dto.NoticeUpdateRequest;
import com.nagabolkka.backend.domain.notice.dto.NoticesResponse;
import com.nagabolkka.backend.domain.notice.entity.Notice;
import com.nagabolkka.backend.domain.notice.entity.NoticeStatus;
import com.nagabolkka.backend.domain.notice.repository.NoticeRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.push.PushMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    // 앱 실행 시 팝업 (로그인 없이)
    public ActiveNoticeResponse getActive() {
        return new ActiveNoticeResponse(noticeRepository.findFirstByStatusOrderByCreatedAtDescIdDesc(NoticeStatus.ACTIVE)
                .map(ActiveNotice::from)
                .orElse(null));
    }

    // 관리자 목록
    public NoticesResponse getAll() {
        return new NoticesResponse(noticeRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(NoticeItem::from)
                .toList());
    }

    // 등록 → 게시 중
    @Transactional
    public NoticeItem create(NoticeCreateRequest request) {
        Notice notice = noticeRepository.save(Notice.create(request.title().strip(), request.content().strip()));
        return NoticeItem.from(notice);
    }

    // 27 : 게시 중지 · 다시 게시 · 제목 / 내용 수정 (보낸 것만)
    @Transactional
    public NoticeItem update(Long noticeId, NoticeUpdateRequest request) {
        if (request.isEmpty() || isBlankGiven(request.title()) || isBlankGiven(request.content())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Notice notice = find(noticeId);
        notice.edit(strip(request.title()), strip(request.content()));
        if (request.status() != null) {
            notice.changeStatus(request.status());
        }
        return NoticeItem.from(notice);
    }

    // 삭제
    @Transactional
    public void delete(Long noticeId) {
        noticeRepository.delete(find(noticeId));
    }

    // 알림 보낸 시각 기록 → 보낼 내용 돌려줌 (발송은 NoticePushService 가 트랜잭션 밖에서)
    @Transactional
    public PushMessage markPushed(Long noticeId) {
        Notice notice = find(noticeId);
        notice.markPushed(LocalDateTime.now(clock));
        return PushMessage.notice(notice.getTitle(), notice.getContent());
    }

    private Notice find(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    // 보냈는데 비어 있음 ("" · 공백만) → 400
    private static boolean isBlankGiven(String value) {
        return value != null && value.isBlank();
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}