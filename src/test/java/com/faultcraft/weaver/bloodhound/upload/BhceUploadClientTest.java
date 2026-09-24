package com.faultcraft.weaver.bloodhound.upload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class BhceUploadClientTest {

    private static final String TOKEN_ID = "test-token-id";
    private static final String TOKEN_KEY = "test-secret-key";

    @Test
    void sign_chainedHmac_nonEmpty() {
        var client = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);

        var headers = client.sign("POST", "/api/v2/file-upload/start",
                null, "application/json");

        assertNotNull(headers.signature());
        assertFalse(headers.signature().isEmpty());
        assertNotNull(headers.requestDate());
    }

    @Test
    void sign_withBody_differentFromWithout() {
        var client = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);

        var noBody = client.sign("POST", "/api/v2/test", null, "application/json");
        var withBody = client.sign("POST", "/api/v2/test",
                "{\"test\":true}".getBytes(StandardCharsets.UTF_8), "application/json");

        assertNotEquals(noBody.signature(), withBody.signature());
    }

    @Test
    void sign_differentMethods_differentSignatures() {
        var client = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);

        var getSign = client.sign("GET", "/api/v2/test", null, "application/json");
        var postSign = client.sign("POST", "/api/v2/test", null, "application/json");

        assertNotEquals(getSign.signature(), postSign.signature());
    }

    @Test
    void sign_differentPaths_differentSignatures() {
        var client = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);

        var sign1 = client.sign("POST", "/api/v2/path1", null, "application/json");
        var sign2 = client.sign("POST", "/api/v2/path2", null, "application/json");

        assertNotEquals(sign1.signature(), sign2.signature());
    }

    @Test
    void sign_signatureIsValidBase64() {
        var client = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);

        var headers = client.sign("GET", "/api/v2/test", null, "application/json");

        byte[] decoded = assertDoesNotThrow(
                () -> Base64.getDecoder().decode(headers.signature()));
        assertEquals(32, decoded.length);
    }

    @Test
    void sign_reproducibleWithSameInput() {
        var client1 = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);
        var client2 = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);

        var h1 = client1.sign("GET", "/api/v2/test", null, "application/json");
        var h2 = client2.sign("GET", "/api/v2/test", null, "application/json");

        assertEquals(h1.requestDate().substring(0, 13), h2.requestDate().substring(0, 13));
    }

    @Test
    void sign_matchesExpectedChain() throws Exception {
        var client = new BhceUploadClient("https://bh.example.com", TOKEN_ID, TOKEN_KEY);
        byte[] body = "{\"data\":true}".getBytes(StandardCharsets.UTF_8);

        var headers = client.sign("POST", "/api/v2/upload", body, "application/json");
        String datetimePrefix = headers.requestDate().substring(0, 13);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TOKEN_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update("POST/api/v2/upload".getBytes(StandardCharsets.UTF_8));
        byte[] stage1 = mac.doFinal();

        mac.init(new SecretKeySpec(stage1, "HmacSHA256"));
        mac.update(datetimePrefix.getBytes(StandardCharsets.UTF_8));
        byte[] stage2 = mac.doFinal();

        mac.init(new SecretKeySpec(stage2, "HmacSHA256"));
        mac.update(body);
        byte[] stage3 = mac.doFinal();

        String expected = Base64.getEncoder().encodeToString(stage3);
        assertEquals(expected, headers.signature());
    }
}
