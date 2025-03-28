package com.bybit_api.webhook.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BybitConfig {
    @Value(value = "${bybit.api.key}")
    private String apikey;

    @Value(value = "${bybit.api.secret}")
    private String apiSecret;

    @Value(value = "${bybit.api.baseurl}")
    private String baseUrl;
}