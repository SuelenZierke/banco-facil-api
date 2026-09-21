package com.unifebe.devsecops.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentServiceTest {

    private final PaymentService paymentService = new PaymentService();

    @Test
    void deveAplicarDezPorCentoDeDesconto() {
        double resultado = paymentService.applyDiscount(200.0, 10);
        // Esperado: 200 - 10% = 180.0
        // Devido ao bug em PaymentService, o resultado real sera 198.0
        assertEquals(180.0, resultado, 0.001);
    }
}
