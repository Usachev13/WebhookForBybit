package com.bybit_api.webhook.controller;

import com.bybit_api.webhook.dto.WebhookOrderRequest;
import com.bybit_api.webhook.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final OrderService orderService;

    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/order")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookOrderRequest request) {
        try {
            // Можно добавить валидацию входящего запроса перед выполнением
            validateRequest(request);

            orderService.executeOrder(request);

            return ResponseEntity
                    .ok("Ордер успешно создан для " + request.getSymbol() + ", TP и SL выставлены.");
        } catch (IllegalArgumentException e) {
            // Отдельная обработка для невалидных входных данных
            return ResponseEntity
                    .badRequest()
                    .body("Некорректный запрос: " + e.getMessage());
        } catch (Exception e) {
            // Логирование критических ошибок
            log.error("Ошибка при создании ордера", e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка создания ордера: " + e.getMessage());
        }
    }

    private void validateRequest(WebhookOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Пустой запрос");
        }
        if (!hasText(request.getSymbol())) {
            throw new IllegalArgumentException("Не указан символ");
        }
        if (!hasText(request.getSide())) {
            throw new IllegalArgumentException("Не указана сторона ордера");
        }
        // Добавьте другие необходимые проверки
    }
}
