package com.unifebe.devsecops.config;

/**
 * ATENCAO - CODIGO PROPOSITALMENTE INSEGURO PARA FINS DIDATICOS.
 * Nunca faca isto em um projeto real: credenciais NUNCA devem ser
 * gravadas diretamente no codigo-fonte (Secret Sprawl).
 */
public class AppConfig {

    // Exemplo de credencial de banco de dados exposta no repositorio
    public static final String DB_PASSWORD = "SuperSecretP@ssw0rd123";

    // Exemplo classico de chave AWS (formato oficial de exemplo da AWS)
    public static final String AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
    public static final String AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    // Exemplo de chave de API de um provedor de pagamentos
    public static final String PAYMENT_GATEWAY_API_KEY = "sk_live_51H8xJ2EXAMPLEKEYDONOTUSEINPRODUCTION0001";

}
