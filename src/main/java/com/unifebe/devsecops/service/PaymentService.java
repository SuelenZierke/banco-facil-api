package com.unifebe.devsecops.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger logger = LogManager.getLogger(PaymentService.class);

    /**
     * Aplica um desconto percentual sobre um preco.
     *
     * CORRECAO: a formula agora divide por 100 (percentual correto).
     * Exemplo: price=200, discountPercent=10 → 200 - (200 * 10 / 100) = 180.0
     */
    public double applyDiscount(double price, int discountPercent) {
        logger.info("Calculando desconto de {}% sobre {}", discountPercent, price);
        return price - (price * discountPercent / 100);
    }
}
