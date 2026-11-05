package com.nagabolkka.backend.global.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesUtilTest {

    private static final String DEV_KEY = "bG9jYWwtZGV2LW9ubHktYWVzLWtleS0zMmJ5dGVzISE=";

    AesUtil aesUtil = new AesUtil(new CryptoProperties(null, DEV_KEY));

    @Test
    @DisplayName("암호화 → 복호화하면 원래 회원번호")
    void roundTrip() {
        byte[] encrypted = aesUtil.encrypt("1234567890");

        assertThat(aesUtil.decrypt(encrypted)).isEqualTo("1234567890");
        assertThat(encrypted.length).isLessThanOrEqualTo(256); // varbinary(256)
    }

    @Test
    @DisplayName("같은 값도 매번 다른 암호문 (IV 랜덤)")
    void differentEachTime() {
        assertThat(aesUtil.encrypt("1234567890")).isNotEqualTo(aesUtil.encrypt("1234567890"));
    }

    @Test
    @DisplayName("키가 32바이트가 아니면 앱이 뜰 때 실패")
    void invalidKey() {
        assertThatThrownBy(() -> new AesUtil(new CryptoProperties(null, "c2hvcnQ=")))
                .isInstanceOf(IllegalStateException.class);
    }
}