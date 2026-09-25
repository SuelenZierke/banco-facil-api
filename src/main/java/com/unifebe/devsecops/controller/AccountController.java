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

        // CORRECAO (SQL Injection): o parametro "id" agora e passado via
        // PreparedStatement com placeholder "?". O driver JDBC faz o escape
        // automatico do valor, tornando impossivel a injecao de SQL arbitrario.
        // Antes: stmt.executeQuery("SELECT * FROM contas WHERE id = '" + id + "'")
        // Depois: query parametrizada — o valor nunca e interpretado como SQL.
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM contas WHERE id = ?");
        stmt.setString(1, id);
        ResultSet rs = stmt.executeQuery();

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
