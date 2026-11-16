package com.nagabolkka.backend.domain.plus.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 루미 플러스 가격 의향 설문 (사용자 당 1행) — created_at = 첫 응답 · updated_at = 마지막 수정 (respondedAt)
@Getter
@Entity
@Table(name = "plus_interests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlusInterest extends BaseEntity {

    public static final int CUSTOM_PRICE_MAX = 50_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // 탈퇴 시 같이 삭제
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceOption priceOption;

    // CUSTOM 일 때만 (0 ~ 50,000원)
    private Integer customPrice;

    // 첫 발견 조각 지급 여부 (응답을 바꿔도 다시 안 줌)
    @Column(nullable = false)
    private boolean rewardGiven;

    public static PlusInterest of(User user, PriceOption priceOption, Integer customPrice) {
        PlusInterest interest = new PlusInterest();
        interest.user = user;
        interest.rewardGiven = false;
        interest.apply(priceOption, customPrice);
        return interest;
    }

    // 응답 바꾸기
    public void change(PriceOption priceOption, Integer customPrice) {
        apply(priceOption, customPrice);
    }

    public void markRewarded() {
        this.rewardGiven = true;
    }

    // CUSTOM 이면 금액 필수 · 범위 확인, 아니면 금액은 버림 (보내도 null)
    private void apply(PriceOption priceOption, Integer customPrice) {
        if (priceOption == PriceOption.CUSTOM
                && (customPrice == null || customPrice < 0 || customPrice > CUSTOM_PRICE_MAX)) {
            throw new CustomException(ErrorCode.PLUS_CUSTOM_PRICE_REQUIRED);
        }
        this.priceOption = priceOption;
        this.customPrice = priceOption == PriceOption.CUSTOM ? customPrice : null;
    }
}