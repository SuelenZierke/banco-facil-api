package com.unifebe.devsecops.config;

/**
 * Configuracao de credenciais da aplicacao.
 *
 * CORRECAO (Secret Sprawl): as credenciais foram removidas do codigo-fonte
 * e passaram a ser lidas de variaveis de ambiente em tempo de execucao.
 * Em producao, essas variaveis devem ser fornecidas por um cofre de segredos
 * (ex.: HashiCorp Vault, AWS Secrets Manager, GitHub Actions Secrets),
 * NUNCA hardcoded no codigo ou na imagem Docker.
 *
 * Variaveis de ambiente esperadas:
 *   DB_PASSWORD            - senha do banco de dados
 *   AWS_ACCESS_KEY_ID      - chave de acesso AWS
 *   AWS_SECRET_ACCESS_KEY  - chave secreta AWS
 *   PAYMENT_GATEWAY_API_KEY - chave da API do gateway de pagamentos
 */
public class AppConfig {

    public static final String DB_PASSWORD =
            System.getenv("DB_PASSWORD");

    public static final String AWS_ACCESS_KEY_ID =
            System.getenv("AWS_ACCESS_KEY_ID");

    public static final String AWS_SECRET_ACCESS_KEY =
            System.getenv("AWS_SECRET_ACCESS_KEY");

    public static final String PAYMENT_GATEWAY_API_KEY =
            System.getenv("PAYMENT_GATEWAY_API_KEY");

}
