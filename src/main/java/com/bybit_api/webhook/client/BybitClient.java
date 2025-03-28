package com.bybit_api.webhook.client;

import com.bybit_api.webhook.config.BybitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class BybitClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private final BybitConfig bybitConfig;

    public BybitClient(BybitConfig bybitConfig) {
        this.bybitConfig = bybitConfig;
    }


    // Method to generate authenticated headers for Bybit API requests
    private HttpHeaders generateHeaders(String timestamp, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-BAPI-API-KEY", bybitConfig.getApikey());
        headers.set("X-BAPI-SIGN", generateSignature(timestamp, payload));
        headers.set("X-BAPI-TIMESTAMP", timestamp);
        headers.set("X-BAPI-RECV-WINDOW", "5000");
        return headers;
    }

    // Overloaded method for generating headers (likely for trading stop)
    private HttpHeaders generateHeaders(String payload) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return generateHeaders(timestamp, payload);
    }

    // Method to create a new order on Bybit
    public String createOrder(String payload) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        HttpHeaders headers = generateHeaders(timestamp, payload);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                bybitConfig.getBaseUrl() + "/v5/order/create",
                entity,
                String.class
        );

        return response.getBody();
    }

    // Method to set trading stop for a position
    public String tradingStop(String jsonPayload){
        String url = bybitConfig.getBaseUrl() + "/v5/position/trading-stop";
        HttpHeaders headers = generateHeaders(jsonPayload);
        HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();
    }

    // Generate HMAC-SHA256 signature for API authentication
    private String generateSignature(String timestamp, String payload) {
        try {
            String signaturePayload = timestamp + bybitConfig.getApikey() + "5000" + payload;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(bybitConfig.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            return bytesToHex(sha256_HMAC.doFinal(signaturePayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Signature generation error", e);
        }
    }

    // Convert byte array to hexadecimal string
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}