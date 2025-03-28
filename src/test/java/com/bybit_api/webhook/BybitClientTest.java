package com.bybit_api.webhook;

import com.bybit_api.webhook.client.BybitClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BybitClientTest {

    @Autowired
    private BybitClient bybitClient;

    @Test
    void testCreateOrder() {
        String payload = "{"
                + "\"category\": \"linear\","
                + "\"symbol\": \"BTCUSDT\","
                + "\"side\": \"Buy\","
                + "\"orderType\": \"Market\","
                + "\"qty\": \"0.001\""
                + "}";

        String response = bybitClient.createOrder(payload);
        System.out.println("Ответ API на создание ордера: " + response);
    }

    @Test
    void testTradingStop(){
        String payload = "{"
                + "\"category\": \"linear\","
                + "\"symbol\": \"BTCUSDT\","
                + "\"stopLoss\": \"40000\","
                + "\"takeProfit\": \"50000\""
                + "}";

        String response = bybitClient.tradingStop(payload);
        System.out.println("Ответ API на установку SL/TP: " + response);
    }
}
