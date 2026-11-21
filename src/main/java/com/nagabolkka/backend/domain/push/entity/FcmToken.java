package com.nagabolkka.backend.domain.push.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // 탈퇴 = 토큰도 삭제 (RefreshToken 과 같음)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token; // 기기 (브라우저) 마다 다름

    public static FcmToken of(User user, String token) {
        FcmToken t = new FcmToken();
        t.user = user;
        t.token = token;
        return t;
    }

    // 같은 기기로 다른 계정이 로그인한 경우
    public void changeOwner(User user) {
        this.user = user;
    }
}