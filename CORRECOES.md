# Registro de Correções — banco-facil-api

**Disciplina:** Segurança da Informação — DevSecOps (Unifebe)  
**Atividade:** Shift Left / Shift Right em Pipelines de CI/CD  
**Alunos:** Maria de Fatima Groh · Giovanna Gabrieli Felipe · Suelen Cristina Zierke  

Este documento registra, de forma estruturada, cada vulnerabilidade intencional presente no repositório base, o gate da pipeline que a detectava, a causa raiz e a correção aplicada.

---

## Resumo das Correções

| # | Gate | Arquivo | Vulnerabilidade | Status |
|---|---|---|---|---|
| 1 | `secret-scan` (Gitleaks) | `AppConfig.java` + `.gitleaks.toml` | Secret Sprawl — credenciais hardcoded | ✅ Corrigido |
| 2 | `unit-tests` (JUnit) | `PaymentService.java` | Bug lógico — divisão por 1000 em vez de 100 | ✅ Corrigido |
| 3 | `sast` (Semgrep) | `AccountController.java` | SQL Injection por concatenação | ✅ Corrigido |
| 4 | `sca` (Trivy) | `pom.xml` | Log4Shell (CVE-2021-44228) + CVEs do Spring Boot 3.2.5 | ✅ Corrigido |
| 5 | `sca` (Trivy) | `pom.xml` | Dependência `gson` duplicada e não utilizada | ✅ Corrigido |
| 6 | `dockerfile-lint` (Hadolint) | `Dockerfile` | DL3007 — tag `latest` não fixada | ✅ Corrigido |
| 7 | `dockerfile-lint` (Hadolint) | `Dockerfile` | DL3002 — container rodando como `root` | ✅ Corrigido |

---

## Correção 1 — Secret Sprawl (Gate: `secret-scan`)

### Problema

O arquivo `AppConfig.java` continha quatro credenciais hardcoded como constantes `public static final String`:

```java
// ANTES (vulnerável)
public static final String DB_PASSWORD             = "<senha-banco-exemplo>";
public static final String AWS_ACCESS_KEY_ID       = "<chave-aws-exemplo>";
public static final String AWS_SECRET_ACCESS_KEY   = "<segredo-aws-exemplo>";
public static final String PAYMENT_GATEWAY_API_KEY = "<chave-stripe-exemplo>";
```

O Gitleaks varrendo o histórico completo do repositório detectava a chave AWS (`aws-access-token`) e a chave Stripe (`stripe-access-token`), falhando com código de saída 1 e bloqueando toda a pipeline.

**Por que "apagar o arquivo" não resolve:**  
O Git preserva o histórico de todos os commits. Mesmo que o arquivo seja modificado ou deletado no commit atual, o segredo continua acessível em commits anteriores (neste caso, no commit `df279d8`). Qualquer ferramenta que varre o histórico completo — como o Gitleaks com `--source .` — continuará encontrando o segredo.

### Correção Aplicada

**Arquivo:** `src/main/java/com/unifebe/devsecops/config/AppConfig.java`

```java
// DEPOIS (corrigido)
public static final String DB_PASSWORD             = System.getenv("DB_PASSWORD");
public static final String AWS_ACCESS_KEY_ID       = System.getenv("AWS_ACCESS_KEY_ID");
public static final String AWS_SECRET_ACCESS_KEY   = System.getenv("AWS_SECRET_ACCESS_KEY");
public static final String PAYMENT_GATEWAY_API_KEY = System.getenv("PAYMENT_GATEWAY_API_KEY");
```

As constantes agora leem seus valores de variáveis de ambiente em tempo de execução. Em produção, essas variáveis seriam fornecidas por um cofre de segredos (HashiCorp Vault, AWS Secrets Manager, etc.) — nunca hardcoded no código ou na imagem Docker.

**Arquivo criado:** `.gitleaks.toml`

Como o histórico do Git ainda contém os valores das chaves no commit `df279d8`, foi criado um arquivo `.gitleaks.toml` com uma `[allowlist]` por regex. Essa abordagem é adequada porque:

- As chaves são **valores de exemplo públicos e inofensivos** (a chave AWS é literalmente o exemplo da documentação oficial da AWS).
- Não há credencial real a revogar, portanto reescrever o histórico (`git filter-repo`) seria desproporcional.
- O allowlist por regex é mais robusto que o `.gitleaksignore` por hash de commit (não quebra se commits forem refeitos).

O workflow `.github/workflows/security.yml` foi atualizado para passar `--config .gitleaks.toml` ao Gitleaks.

**Em um cenário real com credenciais verdadeiras, as ações corretas seriam:**
1. Revogar/rotacionar imediatamente a credencial exposta.
2. Reescrever o histórico com `git filter-repo` ou BFG Repo-Cleaner + force-push.
3. Notificar o time de segurança e verificar se houve acesso não autorizado.

---

## Correção 2 — Bug Lógico no Cálculo de Desconto (Gate: `unit-tests`)

### Problema

O método `applyDiscount` em `PaymentService.java` dividia por `1000` em vez de `100`, fazendo o desconto calculado ser 10 vezes menor que o esperado:

```java
// ANTES (bug)
return price - (price * discountPercent / 1000);
// Para price=200, discount=10%: 200 - (200 * 10 / 1000) = 200 - 2 = 198.0
```

O teste unitário `PaymentServiceTest.deveAplicarDezPorCentoDeDesconto` esperava `180.0` e recebia `198.0`, falhando com:
```
expected: <180.0> but was: <198.0>
```

### Correção Aplicada

**Arquivo:** `src/main/java/com/unifebe/devsecops/service/PaymentService.java`

```java
// DEPOIS (corrigido)
return price - (price * discountPercent / 100);
// Para price=200, discount=10%: 200 - (200 * 10 / 100) = 200 - 20 = 180.0
```

O teste `PaymentServiceTest` agora passa: `expected: <180.0>` ✅

---

## Correção 3 — SQL Injection (Gate: `sast`)

### Problema

O endpoint `GET /conta` em `AccountController.java` concatenava o parâmetro `id` diretamente na string SQL:

```java
// ANTES (vulnerável)
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM contas WHERE id = '" + id + "'");
```

Um atacante poderia enviar valores maliciosos no parâmetro `id`:
- `1' OR '1'='1` → retorna todas as contas (bypass de autenticação)
- `1'; DROP TABLE contas; --` → destruição de dados
- `1' UNION SELECT senha FROM usuarios --` → exfiltração de dados

O Semgrep detectava essa vulnerabilidade com as regras `p/java` (injection patterns).

### Correção Aplicada

**Arquivo:** `src/main/java/com/unifebe/devsecops/controller/AccountController.java`

```java
// DEPOIS (corrigido)
PreparedStatement stmt = conn.prepareStatement(
        "SELECT * FROM contas WHERE id = ?");
stmt.setString(1, id);
ResultSet rs = stmt.executeQuery();
```

O `PreparedStatement` com placeholder `?` faz o escape automático do valor via o driver JDBC. O valor do parâmetro nunca é interpretado como SQL, tornando a injeção impossível.

O import desnecessário `java.sql.Statement` também foi removido.

---

## Correção 4 — Log4Shell e CVEs do Spring Boot (Gate: `sca`)

### Problema

O `pom.xml` original declarava:

```xml
<!-- Log4Shell — CRITICAL -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.14.1</version>
</dependency>
```

A versão `2.14.1` é vulnerável a:
- **CVE-2021-44228** (Log4Shell) — CVSS 10.0 CRITICAL: execução remota de código via JNDI lookup em mensagens de log.
- **CVE-2021-45046** — CVSS 9.0 CRITICAL: bypass da correção incompleta da versão 2.15.0.
- **CVE-2021-45105** — CVSS 7.5 HIGH: negação de serviço via recursão infinita.

Além disso, o `spring-boot-starter-parent 3.2.5` trazia CVEs HIGH/CRITICAL em Tomcat, Spring Framework e outras dependências transitivas.

### Correção Aplicada

**Arquivo:** `pom.xml`

1. **`spring-boot-starter-parent`** atualizado de `3.2.5` → `3.5.16` (última patch da linha 3.5.x, setembro/2026).

2. **Propriedade `log4j2.version`** adicionada com valor `2.25.5` (versão mais recente do log4j2, release 2026-07-01, sem CVEs conhecidas):
   ```xml
   <properties>
       <java.version>17</java.version>
       <log4j2.version>2.25.5</log4j2.version>
   </properties>
   ```
   O `spring-boot-starter-parent` gerencia todas as dependências do log4j2 via essa propriedade, garantindo que o upgrade seja consistente para todos os módulos transitivos.

3. **`log4j-core`** mantido declarado (o código usa a API do log4j2 diretamente em `PaymentService`), mas sem `<version>` explícita — a versão é controlada pela propriedade acima.

**Nota sobre EOL:** Spring Boot 3.5.x atingiu o End of Life em junho/2026. Para um ambiente de produção real, a migração para Spring Boot 4.x seria o próximo passo. Para este exercício didático, a versão 3.5.16 satisfaz os critérios do gate SCA (elimina as CVEs CRITICAL/HIGH exigidas pela atividade).

---

## Correção 5 — Dependência Não Utilizada: `gson` (Gate: `sca` / revisão manual)

### Problema

O `pom.xml` original declarava a dependência `com.google.code.gson:gson:2.10.1` **duas vezes**:

```xml
<!-- Declarada 2x, nunca usada -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

Nenhum `import com.google.gson.*` existe no código-fonte (verificado com `grep -r "import com.google.gson" src/` — sem resultados).

**Por que isso é um problema de segurança:**  
Dependências não utilizadas aumentam a superfície de ataque sem nenhum benefício. Cada biblioteca é um vetor potencial de CVEs. O princípio de menor privilégio também se aplica a dependências: o projeto deve incluir apenas o que realmente usa.

### Correção Aplicada

**Arquivo:** `pom.xml`

Ambas as declarações de `gson` foram removidas integralmente.

---

## Correção 6 — Dockerfile: Tag `latest` (Gate: `dockerfile-lint`, regra DL3007)

### Problema

```dockerfile
# ANTES (vulnerável)
FROM openjdk:latest
```

O uso da tag `latest`:
- **Não é reprodutível:** duas builds feitas em datas diferentes com `latest` podem usar versões distintas do JRE, introduzindo regressões ou vulnerabilidades silenciosas.
- **Dificulta auditorias:** é impossível saber qual versão exata do JRE foi usada em um container em produção.
- **Usa a imagem `openjdk`:** essa imagem foi deprecada em favor de `eclipse-temurin`.

### Correção Aplicada

**Arquivo:** `Dockerfile`

```dockerfile
# DEPOIS (corrigido)
FROM eclipse-temurin:17.0.15_6-jre-alpine
```

Mudanças:
- Imagem base trocada de `openjdk` (deprecada) para `eclipse-temurin` (mantida ativamente pela Adoptium).
- Tag fixada em `17.0.15_6-jre-alpine` (versão exata, reprodutível).
- Variante `-jre` em vez de `-jdk`: contém apenas o Java Runtime Environment (sem compilador, ferramentas de desenvolvimento), reduzindo a superfície de ataque e o tamanho da imagem.
- Variante `-alpine`: imagem base Alpine Linux (~5 MB), significativamente menor que a Debian (~120 MB), com menos pacotes e portanto menos CVEs potenciais.

---

## Correção 7 — Dockerfile: Container Rodando como `root` (Gate: `dockerfile-lint`, regra DL3002)

### Problema

```dockerfile
# ANTES (vulnerável)
USER root
```

Um processo `root` dentro do container representa risco porque:
- Em caso de **container escape** (vulnerabilidade no runtime do container), o processo já possui privilégios de root no host.
- Viola o **Princípio do Menor Privilégio (PoLP)**: nenhum processo deve ter mais permissões do que o estritamente necessário.
- Uma aplicação Spring Boot servindo HTTP não precisa de privilégios de root.

### Correção Aplicada

**Arquivo:** `Dockerfile`

```dockerfile
# DEPOIS (corrigido)
RUN addgroup -S app && adduser -S app -G app
RUN chown app:app app.jar
USER app
```

- `addgroup -S app`: cria o grupo `app` como grupo de sistema (sem GID específico alocado para login).
- `adduser -S app -G app`: cria o usuário `app` como usuário de sistema, sem shell de login e sem diretório home — minimizando a superfície de ataque.
- `chown app:app app.jar`: garante que o arquivo `.jar` seja legível pelo usuário `app`.
- `USER app`: todos os processos subsequentes (incluindo o `CMD`) rodam como usuário não-privilegiado.

> **Nota Alpine:** os comandos `addgroup -S` e `adduser -S` são os comandos BusyBox do Alpine Linux. Em imagens baseadas em Debian/Ubuntu, os equivalentes são `groupadd -r` e `useradd -r -g`.

---

## Arquivos Modificados

| Arquivo | Tipo de Mudança |
|---|---|
| `src/main/java/com/unifebe/devsecops/config/AppConfig.java` | Credenciais hardcoded → `System.getenv()` |
| `src/main/java/com/unifebe/devsecops/service/PaymentService.java` | Bug lógico: `/1000` → `/100` |
| `src/main/java/com/unifebe/devsecops/controller/AccountController.java` | SQL Injection: `Statement` + concatenação → `PreparedStatement` com `?` |
| `pom.xml` | Spring Boot 3.2.5 → 3.5.16; log4j2 2.14.1 → 2.25.5; `gson` removido |
| `Dockerfile` | Imagem fixada `eclipse-temurin:17.0.15_6-jre-alpine`; usuário não-root `app` |
| `.gitleaks.toml` | Criado — allowlist para chaves de exemplo no histórico do Git |
| `.github/workflows/security.yml` | Gitleaks atualizado para usar `--config .gitleaks.toml` |

---

## Referências

- [CVE-2021-44228 — Log4Shell](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)
- [Apache Log4j Security Vulnerabilities](https://logging.apache.org/log4j/2.x/security.html)
- [OWASP — SQL Injection](https://owasp.org/www-community/attacks/SQL_Injection)
- [OWASP — Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [Gitleaks — documentação](https://github.com/gitleaks/gitleaks)
- [Hadolint — regras DL3007 e DL3002](https://github.com/hadolint/hadolint/wiki/DL3007)
- [Eclipse Temurin — imagens Docker](https://hub.docker.com/_/eclipse-temurin)
- [Spring Boot 3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)
