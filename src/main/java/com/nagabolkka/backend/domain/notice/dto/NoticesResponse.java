package com.nagabolkka.backend.domain.notice.dto;

import java.util.List;

public record NoticesResponse(List<NoticeItem> notices) {
}