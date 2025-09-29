package com.example.payments.test.support;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SignatureHelper {

    private SignatureHelper() {
    }

    public static String hmacSha512Hex(String keyHex, String payload) {
        try {
            byte[] keyBytes = HexFormat.of().parseHex(keyHex);
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA512"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(computed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute HmacSHA512 signature", ex);
        }
    }
}
