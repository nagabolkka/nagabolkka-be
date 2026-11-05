package com.nagabolkka.backend.domain.user.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(name = "user_private_info")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrivateInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String kakaoIdHash;

    @Column(nullable = false, length = 256)
    private byte[] kakaoIdEnc;

    public static UserPrivateInfo of(User user, String kakaoIdHash, byte[] kakaoIdEnc) {
        UserPrivateInfo info = new UserPrivateInfo();
        info.user = user;
        info.kakaoIdHash = kakaoIdHash;
        info.kakaoIdEnc = kakaoIdEnc;
        return info;
    }
}