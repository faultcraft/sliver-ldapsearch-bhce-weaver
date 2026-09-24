package com.faultcraft.weaver.bloodhound.upload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.faultcraft.weaver.util.Log;

/**
 * Uploads BH-CE JSON files via the file-upload API with HMAC-SHA256 chained signing.
 */
public final class BhceUploadClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final String baseUrl;
    private final String tokenId;
    private final byte[] tokenKey;
    private final HttpClient httpClient;

    public BhceUploadClient(String url, String tokenId, String tokenKey) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.tokenId = tokenId;
        this.tokenKey = tokenKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /** Uploads JSON/zip files to BH-CE and triggers ingestion. */
    public void uploadFiles(List<Path> files) throws IOException, InterruptedException {
        if (files.isEmpty()) {
            Log.warn("No files to upload");
            return;
        }

        Log.info("Uploading %d files to BH-CE at %s", files.size(), baseUrl);

        int jobId = startUploadJob();
        Log.info("Upload job started: id=%d", jobId);

        for (Path file : files) {
            byte[] data = Files.readAllBytes(file);
            String ct = file.toString().endsWith(".zip")
                    ? "application/zip" : "application/json";
            Log.info("  %s (%,d bytes)", file.getFileName(), data.length);
            request("POST", "/api/v2/file-upload/" + jobId, data, ct);
        }

        request("POST", "/api/v2/file-upload/" + jobId + "/end", null, "application/json");
        Log.info("Upload complete, ingestion triggered");
    }

    private int startUploadJob() throws IOException, InterruptedException {
        var resp = request("POST", "/api/v2/file-upload/start", null, "application/json");
        JsonNode root = MAPPER.readTree(resp);
        return root.path("data").path("id").asInt();
    }

    private byte[] request(String method, String path, byte[] body, String contentType)
            throws IOException, InterruptedException {

        var headers = sign(method, path, body, contentType);

        var builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("User-Agent", "weaver-bhce-upload")
                .header("Authorization", "bhesignature " + tokenId)
                .header("RequestDate", headers.requestDate)
                .header("Signature", headers.signature)
                .header("Content-Type", contentType);

        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<byte[]> resp = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofByteArray());

        if (resp.statusCode() >= 400) {
            String msg = new String(resp.body(), java.nio.charset.StandardCharsets.UTF_8);
            if (msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            throw new IOException("BH-CE API error " + resp.statusCode() + ": " + msg);
        }

        return resp.body();
    }

    /**
     * HMAC-SHA256 chained signing per BH-CE SDK specification.
     *
     * <p>Three-stage HMAC chain:
     * <ol>
     *   <li>HMAC(tokenKey, method + uri)</li>
     *   <li>HMAC(stage1, datetimePrefix)</li>
     *   <li>HMAC(stage2, body)</li>
     * </ol>
     */
    SignedHeaders sign(String method, String uri, byte[] body, String contentType) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);

            mac.init(new SecretKeySpec(tokenKey, HMAC_ALGO));
            mac.update((method + uri).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] stage1 = mac.doFinal();

            String datetime = OffsetDateTime.now()
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String datetimePrefix = datetime.substring(0, Math.min(13, datetime.length()));

            mac.init(new SecretKeySpec(stage1, HMAC_ALGO));
            mac.update(datetimePrefix.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] stage2 = mac.doFinal();

            mac.init(new SecretKeySpec(stage2, HMAC_ALGO));
            if (body != null) {
                mac.update(body);
            }
            byte[] stage3 = mac.doFinal();

            return new SignedHeaders(datetime,
                    Base64.getEncoder().encodeToString(stage3));

        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    record SignedHeaders(String requestDate, String signature) {}
}
