package com.nagabolkka.backend.domain.plus.repository;

import com.nagabolkka.backend.domain.plus.entity.PriceOption;

// 관리자 : 선택지 별 응답 수 · 직접 입력 평균 (CUSTOM 만 값, 나머지 null)
public record PriceOptionCount(PriceOption priceOption, Long count, Double customPriceAvg) {
}