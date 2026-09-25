# Registro de Correcoes de Seguranca - banco-facil-api

Disciplina: Seguranca da Informacao - DevSecOps (Unifebe)
Atividade: Shift Left e Shift Right em Pipelines de CI/CD
Alunos: Maria de Fatima Groh, Giovanna Gabrieli Felipe, Suelen Cristina Zierke

Este documento descreve cada vulnerabilidade identificada no repositorio base, o respectivo gate da pipeline responsavel pela deteccao, a analise da causa raiz e a correcao implementada.

---

## Sumario

| N. | Gate da Pipeline | Arquivo | Vulnerabilidade |
|----|-----------------|---------|-----------------|
| 1 | secret-scan (Gitleaks) | AppConfig.java | Credenciais hardcoded no codigo-fonte (Secret Sprawl) |
| 2 | unit-tests (JUnit) | PaymentService.java | Erro logico no calculo de desconto |
| 3 | sast (Semgrep) | AccountController.java | Injecao de SQL por concatenacao de parametro |
| 4 | sca (Trivy) | pom.xml | Dependencia log4j-core 2.14.1 com CVE-2021-44228 |
| 5 | sca (Trivy) | pom.xml | Spring Boot 3.2.5 com CVEs HIGH/CRITICAL |
| 6 | sca / revisao manual | pom.xml | Dependencia gson declarada duas vezes e nao utilizada |
| 7 | dockerfile-lint (Hadolint) | Dockerfile | Tag de imagem base nao fixada (regra DL3007) |
| 8 | dockerfile-lint (Hadolint) | Dockerfile | Container executando como usuario root (regra DL3002) |

---

## 1. Credenciais Hardcoded no Codigo-Fonte

Gate: secret-scan (Gitleaks)
Arquivo: src/main/java/com/unifebe/devsecops/config/AppConfig.java

### Descricao do Problema

O arquivo AppConfig.java continha quatro credenciais declaradas diretamente como constantes publicas estaticas: uma senha de banco de dados, uma chave de acesso AWS, a respectiva chave secreta AWS e uma chave de API de gateway de pagamentos. Essa pratica e conhecida como Secret Sprawl e representa um dos vetores de exposicao mais comuns em repositorios de codigo.

O Gitleaks, configurado para varrer o historico completo de commits, detectava as chaves AWS e Stripe e retornava codigo de saida 1, bloqueando todos os gates subsequentes da pipeline.

### Por que remover o arquivo nao e suficiente

O Git preserva o historico integral de todas as alteracoes. A remocao ou edicao de um arquivo no commit atual nao apaga sua existencia em commits anteriores. Qualquer ferramenta que varre o historico completo, como o Gitleaks com a flag --source ., continuara encontrando os valores expostos nos commits antigos, independentemente do estado atual do arquivo.

### Correcao Implementada

As constantes foram reescritas para ler seus valores de variaveis de ambiente em tempo de execucao:

```java
public static final String DB_PASSWORD             = System.getenv("DB_PASSWORD");
public static final String AWS_ACCESS_KEY_ID       = System.getenv("AWS_ACCESS_KEY_ID");
public static final String AWS_SECRET_ACCESS_KEY   = System.getenv("AWS_SECRET_ACCESS_KEY");
public static final String PAYMENT_GATEWAY_API_KEY = System.getenv("PAYMENT_GATEWAY_API_KEY");
```

Em ambientes de producao, essas variaveis devem ser fornecidas por um gerenciador centralizado de segredos, como HashiCorp Vault ou AWS Secrets Manager.

Para tratar o historico do repositorio, foi criado o arquivo .gitleaks.toml com uma secao [allowlist] referenciando o caminho do arquivo original. Essa abordagem e adequada neste contexto porque os valores presentes no historico sao exclusivamente chaves de exemplo publicas, sem nenhuma credencial real associada. O workflow security.yml foi atualizado para que o Gitleaks utilize essa configuracao via --config .gitleaks.toml.

Em um cenario com credenciais reais expostas, as acoes corretas seriam: (1) revogar e rotacionar imediatamente a credencial; (2) reescrever o historico com git filter-repo; (3) notificar o time de seguranca.

---

## 2. Erro Logico no Calculo de Desconto

Gate: unit-tests (JUnit)
Arquivo: src/main/java/com/unifebe/devsecops/service/PaymentService.java

### Descricao do Problema

O metodo applyDiscount calculava o desconto dividindo por 1000 em vez de 100, resultando em um valor dez vezes menor que o esperado. Para um preco de R$ 200,00 com desconto de 10%, o resultado retornado era R$ 198,00 em vez de R$ 180,00.

O teste unitario PaymentServiceTest.deveAplicarDezPorCentoDeDesconto falhava com a mensagem:
expected: <180.0> but was: <198.0>

### Correcao Implementada

```java
// Antes
return price - (price * discountPercent / 1000);

// Depois
return price - (price * discountPercent / 100);
```

Apos a correcao, o teste passa e o gate unit-tests e liberado.

---

## 3. Injecao de SQL por Concatenacao de Parametro

Gate: sast (Semgrep)
Arquivo: src/main/java/com/unifebe/devsecops/controller/AccountController.java

### Descricao do Problema

O endpoint GET /conta construia a consulta SQL concatenando diretamente o parametro id recebido da requisicao HTTP, sem nenhuma sanitizacao ou parametrizacao:

```java
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM contas WHERE id = '" + id + "'");
```

Essa pratica permite que um atacante manipule a estrutura da consulta SQL por meio de entradas maliciosas. Exemplos de vetores de ataque: entrada "1 OR 1=1" retorna todos os registros; entrada com DROP TABLE executa um comando destrutivo; entrada com UNION SELECT exfiltra dados de outras tabelas. O Semgrep identificou o padrao com as regras do conjunto p/java.

### Correcao Implementada

O Statement com concatenacao foi substituido por PreparedStatement com parametro posicional:

```java
PreparedStatement stmt = conn.prepareStatement(
        "SELECT * FROM contas WHERE id = ?");
stmt.setString(1, id);
ResultSet rs = stmt.executeQuery();
```

O driver JDBC realiza o escape automatico do valor fornecido, garantindo que ele seja tratado sempre como dado e nunca como instrucao SQL. O import desnecessario de java.sql.Statement tambem foi removido.

---

## 4. Dependencia log4j-core com Vulnerabilidade Critica (Log4Shell)

Gate: sca (Trivy)
Arquivo: pom.xml

### Descricao do Problema

O pom.xml declarava explicitamente org.apache.logging.log4j:log4j-core:2.14.1, sobrescrevendo a versao gerenciada pelo Spring Boot. Essa versao e vulneravel a tres CVEs de alta severidade:

- CVE-2021-44228 (Log4Shell) - CVSS 10.0 CRITICAL: execucao remota de codigo via JNDI lookup em mensagens de log.
- CVE-2021-45046 - CVSS 9.0 CRITICAL: contorna a correcao incompleta da versao 2.15.0.
- CVE-2021-45105 - CVSS 7.5 HIGH: negacao de servico por recursao infinita.

### Correcao Implementada

A versao foi atualizada para 2.25.5 por meio da propriedade gerenciada pelo Spring Boot:

```xml
<properties>
    <java.version>17</java.version>
    <log4j2.version>2.25.5</log4j2.version>
</properties>
```

A declaracao da dependencia foi mantida sem versao explicita, delegando o controle inteiramente a propriedade acima e garantindo consistencia entre todos os modulos transitivos do log4j2.

---

## 5. Spring Boot 3.2.5 com CVEs em Dependencias Transitivas

Gate: sca (Trivy)
Arquivo: pom.xml

### Descricao do Problema

O spring-boot-starter-parent na versao 3.2.5 gerenciava versoes de Tomcat, Spring Framework e outras bibliotecas que continham CVEs classificadas como HIGH e CRITICAL pelo Trivy.

### Correcao Implementada

O spring-boot-starter-parent foi atualizado de 3.2.5 para 3.5.16, ultima versao patch da linha 3.5.x, que incorpora as correcoes de seguranca acumuladas ate setembro de 2026.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
</parent>
```

Nota: a linha 3.5.x atingiu o End of Life em junho de 2026. Em producao real, a migracao para Spring Boot 4.x seria o proximo passo recomendado. Para este exercicio, a versao 3.5.16 e suficiente para eliminar as CVEs detectadas.

---

## 6. Dependencia Nao Utilizada Declarada em Duplicidade

Gate: sca (Trivy) / revisao manual
Arquivo: pom.xml

### Descricao do Problema

A dependencia com.google.code.gson:gson:2.10.1 estava declarada duas vezes no pom.xml e nao era referenciada em nenhum ponto do codigo-fonte. A verificacao foi realizada com:

    grep -r "import com.google.gson" src/

O comando nao retornou resultados. Manter dependencias nao utilizadas aumenta desnecessariamente a superficie de ataque, pois cada biblioteca adicional e um vetor potencial de novas CVEs sem nenhum beneficio funcional.

### Correcao Implementada

Ambas as declaracoes de gson foram removidas do pom.xml.

---

## 7. Imagem Base do Dockerfile com Tag Nao Fixada

Gate: dockerfile-lint (Hadolint, regra DL3007)
Arquivo: Dockerfile

### Descricao do Problema

O Dockerfile utilizava a tag latest na instrucao FROM:

```dockerfile
FROM openjdk:latest
```

O uso de latest nao e reprodutivel: a mesma tag pode apontar para versoes diferentes do JRE em momentos distintos, o que pode introduzir regressoes ou vulnerabilidades de forma silenciosa. Alem disso, a imagem openjdk foi oficialmente descontinuada em favor de eclipse-temurin.

### Correcao Implementada

```dockerfile
FROM eclipse-temurin:17.0.15_6-jre-alpine
```

A imagem eclipse-temurin e mantida pela Adoptium. A tag 17.0.15_6-jre-alpine e imutavel, garantindo reproducibilidade. A variante -jre contem apenas o Java Runtime Environment, reduzindo a superficie de ataque. A variante -alpine resulta em uma imagem menor e com menos pacotes expostos a vulnerabilidades.

---

## 8. Container Executando como Usuario Root

Gate: dockerfile-lint (Hadolint, regra DL3002)
Arquivo: Dockerfile

### Descricao do Problema

O Dockerfile definia explicitamente o usuario root como contexto de execucao:

```dockerfile
USER root
```

Executar processos como root viola o Principio do Menor Privilegio. Em caso de exploracao de uma vulnerabilidade no runtime do container, um processo root ja possui os privilegios maximos no sistema host. Uma aplicacao Spring Boot que expoe endpoints HTTP nao requer privilegios elevados.

### Correcao Implementada

```dockerfile
RUN addgroup -S app && adduser -S app -G app
RUN chown app:app app.jar
USER app
```

O usuario app e criado como usuario de sistema, sem shell de login e sem diretorio home. A instrucao chown garante que o arquivo .jar seja acessivel antes da troca de contexto. Os comandos addgroup -S e adduser -S sao especificos do BusyBox presente na imagem Alpine; em imagens Debian/Ubuntu os equivalentes sao groupadd -r e useradd -r -g.

---

## Arquivos Modificados

| Arquivo | Alteracao Realizada |
|---------|---------------------|
| src/main/java/com/unifebe/devsecops/config/AppConfig.java | Credenciais substituidas por variaveis de ambiente |
| src/main/java/com/unifebe/devsecops/service/PaymentService.java | Divisor corrigido de 1000 para 100 |
| src/main/java/com/unifebe/devsecops/controller/AccountController.java | PreparedStatement com parametro posicional |
| pom.xml | Spring Boot 3.5.16; log4j2 2.25.5; gson removido |
| Dockerfile | eclipse-temurin:17.0.15_6-jre-alpine; usuario nao-root |
| .gitleaks.toml | Allowlist para AppConfig.java do historico |
| .github/workflows/security.yml | Gitleaks configurado com --config .gitleaks.toml |

---

## Referencias

- NVD - CVE-2021-44228: https://nvd.nist.gov/vuln/detail/CVE-2021-44228
- Apache Log4j Security: https://logging.apache.org/log4j/2.x/security.html
- OWASP SQL Injection: https://owasp.org/www-community/attacks/SQL_Injection
- OWASP Secrets Management Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
- Gitleaks: https://github.com/gitleaks/gitleaks
- Hadolint DL3007/DL3002: https://github.com/hadolint/hadolint/wiki/DL3007
- Eclipse Temurin: https://hub.docker.com/_/eclipse-temurin
- Spring Boot 3.5 Release Notes: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes
