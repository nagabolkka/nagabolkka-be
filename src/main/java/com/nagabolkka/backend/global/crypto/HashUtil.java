package com.nagabolkka.backend.global.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class HashUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKeySpec hmacKey;

    public HashUtil(CryptoProperties cryptoProperties) {
        String secret = cryptoProperties.hashSecret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("HASH_SECRET 은 32자 이상이어야 해요");
        }
        this.hmacKey = new SecretKeySpec(secret.getBytes(UTF_8), HMAC_ALGORITHM);
    }

    // 카카오 회원번호 → kakao_id_hash (서버 비밀키가 있어야 같은 값을 만들 수 있음)
    public String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }

    /** Refresh Token → token_hash (원문은 DB 에 저장하지 않는다) */
    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 계산 실패", e);
        }
    }
}