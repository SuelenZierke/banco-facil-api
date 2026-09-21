package com.unifebe.devsecops.controller;

import com.unifebe.devsecops.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@RestController
public class AccountController {

    private final PaymentService paymentService;

    public AccountController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/discount")
    public double getDiscount(@RequestParam double price, @RequestParam int percent) {
        return paymentService.applyDiscount(price, percent);
    }

    @GetMapping("/conta")
    public String buscarConta(@RequestParam String id) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
        Statement stmt = conn.createStatement();

        //FALHA (SQL Injection): o parametro "id" vem direto da requisicao HTTP
        //e e concatenado na string SQL sem nenhuma sanitizacao/parametrizacao.
        //Um atacante pode enviar, por exemplo, "1' OR '1'='1" para ler contas
        //que nao deveria, ou "1'; DROP TABLE contas; --" para destruir dados.
        ResultSet rs = stmt.executeQuery("SELECT * FROM contas WHERE id = '" + id + "'");

        StringBuilder resultado = new StringBuilder();
        while (rs.next()) {
            resultado.append(rs.getString("nome")).append(" ");
        }
        return resultado.toString();
    }

    

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
