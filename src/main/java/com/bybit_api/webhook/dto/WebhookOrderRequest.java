package com.bybit_api.webhook.dto;
import lombok.Data;

@Data
public class WebhookOrderRequest {
    private String symbol;
    private String side;
    private String qty;
    private String entry;
    private String tp_pct;
    private String sl_pct;
}
