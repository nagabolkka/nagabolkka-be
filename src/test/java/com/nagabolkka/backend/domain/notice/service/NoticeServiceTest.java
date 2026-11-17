package com.nagabolkka.backend.domain.notice.service;

import com.nagabolkka.backend.domain.notice.dto.NoticeCreateRequest;
import com.nagabolkka.backend.domain.notice.dto.NoticeItem;
import com.nagabolkka.backend.domain.notice.dto.NoticeUpdateRequest;
import com.nagabolkka.backend.domain.notice.entity.Notice;
import com.nagabolkka.backend.domain.notice.entity.NoticeStatus;
import com.nagabolkka.backend.domain.notice.repository.NoticeRepository;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock NoticeRepository noticeRepository;

    @InjectMocks NoticeService noticeService;

    private Notice notice(Long id, String title) {
        Notice notice = Notice.create(title, "내용");
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    @Test
    @DisplayName("게시 중 공지 → 최근 1개 (id · 제목 · 내용)")
    void getActive() {
        given(noticeRepository.findFirstByStatusOrderByCreatedAtDescIdDesc(NoticeStatus.ACTIVE))
                .willReturn(Optional.of(notice(3L, "서버 점검 안내")));

        assertThat(noticeService.getActive().notice().noticeId()).isEqualTo(3L);
        assertThat(noticeService.getActive().notice().title()).isEqualTo("서버 점검 안내");
    }

    @Test
    @DisplayName("게시 중 공지 없음 → notice null")
    void getActive_none() {
        given(noticeRepository.findFirstByStatusOrderByCreatedAtDescIdDesc(NoticeStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThat(noticeService.getActive().notice()).isNull();
    }

    @Test
    @DisplayName("등록 → 앞뒤 공백 제거 · ACTIVE")
    void create() {
        given(noticeRepository.save(any(Notice.class))).will(returnsFirstArg());

        NoticeItem item = noticeService.create(new NoticeCreateRequest("  서버 점검  ", " 내일 점검해요 "));

        assertThat(item.title()).isEqualTo("서버 점검");
        assertThat(item.content()).isEqualTo("내일 점검해요");
        assertThat(item.status()).isEqualTo(NoticeStatus.ACTIVE);
    }

    @Test
    @DisplayName("상태만 수정 → STOPPED · 제목 · 내용 그대로")
    void update_statusOnly() {
        Notice notice = notice(3L, "서버 점검 안내");
        given(noticeRepository.findById(3L)).willReturn(Optional.of(notice));

        NoticeItem item = noticeService.update(3L, new NoticeUpdateRequest(NoticeStatus.STOPPED, null, null));

        assertThat(item.status()).isEqualTo(NoticeStatus.STOPPED);
        assertThat(item.title()).isEqualTo("서버 점검 안내");
        assertThat(item.content()).isEqualTo("내용");
    }

    @Test
    @DisplayName("바꿀 값 없음 · 빈 제목 → 400 · 조회 안 함")
    void update_invalid() {
        assertThatThrownBy(() -> noticeService.update(3L, new NoticeUpdateRequest(null, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> noticeService.update(3L, new NoticeUpdateRequest(null, "   ", null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        then(noticeRepository).should(never()).findById(anyLong());
    }

    @Test
    @DisplayName("없는 공지 수정 · 삭제 → 404")
    void notFound() {
        given(noticeRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.update(9L, new NoticeUpdateRequest(NoticeStatus.ACTIVE, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> noticeService.delete(9L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        then(noticeRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("삭제 → 그 공지 delete")
    void delete() {
        Notice notice = notice(3L, "서버 점검 안내");
        given(noticeRepository.findById(3L)).willReturn(Optional.of(notice));

        noticeService.delete(3L);

        then(noticeRepository).should().delete(notice);
    }
}