package com.bybit_api.webhook.service;

import com.bybit_api.webhook.client.BybitClient;
import com.bybit_api.webhook.dto.WebhookOrderRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class OrderService {

    private final BybitClient bybitClient;
    private final ObjectMapper objectMapper;

    public OrderService(BybitClient bybitClient, ObjectMapper objectMapper) {
        this.bybitClient = bybitClient;
        this.objectMapper = objectMapper;
    }

    private String createOrderMarketPayload(WebhookOrderRequest request) {
        return String.format(
                "{\"category\":\"linear\",\"symbol\":\"%s\",\"side\":\"%s\",\"orderType\":\"Market\",\"qty\":\"%s\"}",
                request.getSymbol(),
                request.getSide(),
                request.getQty()
        );
    }

    private String createTradingStopPayload(WebhookOrderRequest request, BigDecimal slPrice, BigDecimal tpPrice) {
        validateTradingStopPrices(slPrice, tpPrice);

        return String.format(
                "{\"category\":\"linear\",\"symbol\":\"%s\",\"stopLoss\":\"%s\",\"takeProfit\":\"%s\",\"positionIdx\":0}",
                request.getSymbol(),
                slPrice.setScale(2, RoundingMode.HALF_UP),
                tpPrice.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private void validateTradingStopPrices(BigDecimal slPrice, BigDecimal tpPrice) {
        if (slPrice.compareTo(BigDecimal.ZERO) <= 0 || tpPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stop Loss и Take Profit должны быть положительными числами");
        }
    }

    private BigDecimal calculatePrice(BigDecimal entryPrice, BigDecimal percent, boolean isLongSide, boolean isTakeProfit) {
        BigDecimal adjustedPercent = percent.divide(BigDecimal.valueOf(100),8, RoundingMode.HALF_UP);

        if (isLongSide) {
            return isTakeProfit
                    ? entryPrice.multiply(BigDecimal.ONE.add(adjustedPercent))
                    : entryPrice.multiply(BigDecimal.ONE.subtract(adjustedPercent));
        } else {
            return isTakeProfit
                    ? entryPrice.multiply(BigDecimal.ONE.subtract(adjustedPercent))
                    : entryPrice.multiply(BigDecimal.ONE.add(adjustedPercent));
        }
    }

    public void executeOrder(WebhookOrderRequest request) throws Exception {
        try {
            // Создание маркет-ордера
            String orderPayload = createOrderMarketPayload(request);
            String orderResponse = bybitClient.createOrder(orderPayload);

            JsonNode orderJson = objectMapper.readTree(orderResponse);
            if (orderJson.path("retCode").asInt() != 0) {
                throw new RuntimeException("Ошибка создания ордера: " + orderResponse);
            }

            // Определение цены входа
            BigDecimal entryPrice = determineEntryPrice(request, orderJson);

            // Расчет процентов Stop Loss и Take Profit
            BigDecimal slPercent = new BigDecimal(request.getSl_pct());
            BigDecimal tpPercent = new BigDecimal(request.getTp_pct());

            // Расчет цен Stop Loss и Take Profit
            boolean isLongSide = "Buy".equalsIgnoreCase(request.getSide());
            BigDecimal slPrice = calculatePrice(entryPrice, slPercent, isLongSide, false);
            BigDecimal tpPrice = calculatePrice(entryPrice, tpPercent, isLongSide, true);

            // Создание и отправка payload для trading-stop
            String tradingStopPayload = createTradingStopPayload(request, slPrice, tpPrice);
            String tradingStopResponse = bybitClient.tradingStop(tradingStopPayload);

            JsonNode stopJson = objectMapper.readTree(tradingStopResponse);
            if (stopJson.path("retCode").asInt() != 0) {
                throw new RuntimeException("Ошибка установки SL/TP: " + tradingStopResponse);
            }

            log.info("Ордер успешно создан для {}, цена входа: {}, SL: {}, TP: {}",
                    request.getSymbol(), entryPrice, slPrice, tpPrice);

        } catch (Exception e) {
            log.error("Ошибка создания ордера: ", e);
            throw e;
        }
    }

    private BigDecimal determineEntryPrice(WebhookOrderRequest request, JsonNode orderJson) {
        BigDecimal entryPrice;
        if (request.getEntry() != null && !request.getEntry().isEmpty()) {
            entryPrice = new BigDecimal(request.getEntry());
        } else {
            entryPrice = new BigDecimal(orderJson.path("result").path("avgPrice").asText("0"));
        }

        if (entryPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Не удалось определить корректную цену входа");
        }

        return entryPrice;
    }
}