# Da Vulnerabilidade ao Deploy Seguro: Implantando DevSecOps na BancoFácil Digital

**Disciplina:** Segurança da Informação
**Tema:** Shift Left e Shift Right em Pipelines de CI/CD
**Formato:** Pesquisa Teórica (individual) + Hands-on Prático (dupla ou trio)

---

## 1. Cenário

A **BancoFácil Digital** é uma fintech que, em três anos, saiu de um MVP feito por dois desenvolvedores para uma plataforma com mais de 40 microsserviços e 25 desenvolvedores distribuídos em quatro squads. O crescimento acelerado priorizou velocidade de entrega: o pipeline de CI/CD apenas compila o código e faz o deploy automático a cada merge na branch `main`, sem qualquer verificação de segurança. Há três semanas, um pesquisador de segurança reportou publicamente que uma chave de API de um provedor de pagamentos estava exposta em um repositório público da empresa há mais de seis meses — chave essa que havia sido commitada "temporariamente" por um desenvolvedor durante um teste local e nunca removida.

O incidente não gerou vazamento de dados de clientes, mas foi suficiente para que a diretoria determinasse a criação de um programa de **DevSecOps**. Você foi contratado(a) como consultor(a) de segurança para redesenhar o pipeline da BancoFácil, aplicando controles de **Shift Left** (mover a segurança para o início do ciclo de desenvolvimento, antes do deploy) e recomendando práticas de **Shift Right** (validar e monitorar a segurança da aplicação já em produção). Seu primeiro entregável é um piloto: um repositório de exemplo com uma aplicação Java vulnerável, sobre o qual você deve implantar uma pipeline de segurança no GitHub Actions capaz de detectar e bloquear os problemas antes que cheguem à produção.

---

## 2. Parte 1 — Guia da Atividade de Pesquisa Teórica

Responda às questões abaixo com base em pesquisa em documentação oficial, artigos técnicos e materiais de fornecedores de segurança (ex.: OWASP, NIST, GitHub Docs, GitLab Docs, Snyk, Aqua Security, Sonar). Cite suas fontes. As questões são orientadoras — não é necessário respondê-las em formato de lista; monte um texto dissertativo estruturado por seção.

### 2.1 Shift Left vs. Shift Right

1. O que significa, literalmente, "mover para a esquerda" e "mover para a direita" quando aplicado ao ciclo de vida de desenvolvimento de software (SDLC)? Desenhe ou descreva uma linha do tempo do SDLC e marque onde cada prática atua.
2. Cite três controles de segurança tipicamente associados a Shift Left e três associados a Shift Right. Por que faz sentido classificá-los assim?
3. Shift Left e Shift Right são abordagens concorrentes ou complementares? Justifique com um exemplo de uma vulnerabilidade que só seria detectada em produção (Shift Right) mesmo com um pipeline de Shift Left robusto.
4. Qual é o custo relativo (tempo, dinheiro, reputação) de corrigir uma falha de segurança em cada uma das fases: design, código, build, teste, produção? Relacione com o motivo pelo qual o mercado tem investido em Shift Left. Avalie criticamente a origem e a validade dos números que encontrar (por exemplo, a "regra dos 100x" é muito citada, mas contestada).

### 2.2 Gestão de Segredos

1. Defina **Secret Sprawl**. Quais são as formas mais comuns pelas quais segredos "vazam" para dentro de um repositório de código (commits antigos, arquivos de configuração, mensagens de log, imagens Docker, histórico do Git)?
2. Explique a diferença entre **segredos de build/CI** (ex.: token para publicar um pacote, credencial para acessar um registry) e **segredos de runtime/aplicação** (ex.: string de conexão de banco de dados, chave de API consumida pela aplicação em produção). Em que momento do pipeline cada tipo é necessário?
3. Por que armazenar um segredo de runtime dentro da imagem de contêiner (ex.: em uma variável `ENV` fixa no Dockerfile) é considerado uma má prática, mesmo que o repositório da imagem seja privado?
4. Pesquise ao menos duas ferramentas de detecção de segredos (ex.: Gitleaks, TruffleHog, GitHub Secret Scanning) e duas soluções de gerenciamento centralizado de segredos (ex.: HashiCorp Vault, AWS Secrets Manager, GitHub Actions Secrets, Azure Key Vault). Qual a diferença de propósito entre as duas categorias?
5. O que é rotação de segredos (*secret rotation*) e por que ela é a resposta correta — e não apenas a remoção do código — quando um segredo é encontrado exposto?

### 2.3 Proteção e Qualidade

1. O que é **Branch Protection** no GitHub/GitLab? Cite pelo menos quatro regras que podem ser configuradas (ex.: exigir pull request, exigir aprovação, exigir checks de status, proibir force-push).
2. Defina **Quality Gate**. Como um Quality Gate se diferencia de um simples "relatório" de qualidade que não bloqueia nada?
3. Qual o papel dos **testes unitários** dentro de uma estratégia de segurança, além da verificação de corretude funcional? Existe relação entre cobertura de testes e superfície de risco?
4. Relacione Branch Protection + Quality Gates + Testes Unitários com o conceito de Shift Left: por que essas três práticas, juntas, formam a primeira linha de defesa antes mesmo de qualquer ferramenta de segurança especializada entrar em ação?

### 2.4 Análise de Segurança: SAST, DAST e SCA

1. Defina **SAST** (Static Application Security Testing). Em que fase do pipeline ele roda e sobre o que ele analisa (código-fonte, bytecode ou aplicação em execução)?
2. Defina **DAST** (Dynamic Application Security Testing). Por que o DAST exige uma aplicação em execução e como isso muda o momento em que ele pode ser inserido no pipeline?
3. Defina **SCA** (Software Composition Analysis / Segurança da Cadeia de Suprimentos). Qual vulnerabilidade histórica de grande impacto (pesquise, por exemplo, o caso *Log4Shell* — CVE-2021-44228) exemplifica por que SCA se tornou essencial?
4. Monte uma tabela comparando SAST, DAST e SCA nas colunas: o que analisa, quando roda no pipeline, exemplos de ferramentas, tipos de falha que detecta e limitações (ex.: falsos positivos, incapacidade de detectar falhas de lógica de negócio).
5. Entre SAST e DAST, qual é puramente Shift Left, e qual pode ser usada tanto em Shift Left (contra um ambiente de staging) quanto em Shift Right (contra produção, de forma controlada)? E o SCA: ele atua apenas em Shift Left? Relacione com o fato de que novas CVEs são publicadas depois do deploy (como aconteceu com o Log4Shell).

### 2.5 Infraestrutura e Contêineres

1. O que significa **hardening** de um Dockerfile? Cite pelo menos cinco más práticas comuns (ex.: uso da tag `latest`, execução como `root`, uso de imagens base muito grandes, `ADD` em vez de `COPY`, segredos copiados para dentro da imagem, ausência de multi-stage build).
2. Explique o **Princípio do Menor Privilégio (PoLP)** aplicado a contêineres. Por que um contêiner rodando como `root` representa um risco mesmo que o processo dentro dele "precise" de privilégios de root na sua lógica de negócio?
3. Pesquise o que é uma **imagem multi-stage build** e como ela contribui tanto para redução de superfície de ataque quanto para redução do tamanho da imagem final.
4. Cite e explique ao menos uma ferramenta de *linting*/hardening de Dockerfile (ex.: Hadolint) e uma ferramenta de escaneamento de imagens já construídas (ex.: Trivy, Grype, Docker Scout).

**Entregável da Parte 1:** documento com respostas dissertativas organizadas pelas seções acima, referências bibliográficas ao final e pelo menos um diagrama/esquema autoral (pode ser desenhado à mão e fotografado) posicionando as práticas estudadas na linha do tempo Shift Left → Shift Right.

---

## 3. Parte 2 — Hands-on Prático (GitHub Actions + Java)

### 3.1 Objetivo

Vocês vão criar um repositório no GitHub a partir de um **repositório base fornecido pelo professor**, contendo uma aplicação Spring Boot **propositalmente vulnerável** e uma pipeline de segurança no GitHub Actions. O objetivo é observar a pipeline **falhando** por causa de cada uma das falhas plantadas e, em seguida, **corrigir uma a uma**, até conseguir um pipeline verde que publica a imagem Docker no GitHub Container Registry (GHCR).

> Todas as "credenciais" presentes no código de exemplo são valores de exemplo públicos e inofensivos (a chave de acesso da AWS é literalmente a chave de exemplo da documentação oficial da AWS). Isso é proposital: o objetivo é testar a **detecção**, não usar segredos reais.
>
> **Não copiem o valor dessas chaves** para o relatório, para o `README.md` nem para qualquer outro arquivo do repositório: o Gitleaks varre todos os arquivos e todo o histórico, e um valor colado em um `.md` também é detectado.

### 3.2 Repositório base e estrutura

O código completo (aplicação, `Dockerfile` e pipeline) está no repositório base: **[LINK DO REPOSITÓRIO BASE]**.

Nele, cliquem em **Use this template → Create a new repository**. O repositório base é um *template*, então o novo repositório começa com histórico limpo. Nomeiem o novo repositório **`banco-facil-api`**: o nome do repositório define o nome da imagem publicada (`ghcr.io/<seu-usuario>/banco-facil-api`). Ele pode ser público ou privado; se for privado, adicionem **[USUÁRIO GITHUB DO PROFESSOR]** como colaborador.

O repositório terá esta estrutura:

```
banco-facil-api/
├── pom.xml
├── Dockerfile
├── alunos.txt                      <- vocês preenchem com os nomes da dupla/trio
├── .gitignore
├── .gitleaksignore                 <- será usado na correção dos segredos (3.5)
├── README.md                       <- criado pela pipeline na 1ª execução (não editem à mão)
├── .github/
│   └── workflows/
│       └── security.yml
└── src/
    ├── main/java/com/unifebe/devsecops/
    │   ├── DemoApplication.java
    │   ├── config/AppConfig.java
    │   ├── controller/AccountController.java
    │   └── service/PaymentService.java
    └── test/java/com/unifebe/devsecops/service/
        └── PaymentServiceTest.java
```

A pipeline **"Pipeline de Seguranca DevSecOps"** tem 8 jobs. Os cinco *gates* rodam **em sequência** (cada um só executa se o anterior passar):

`secret-scan` → `unit-tests` → `sast` → `sca` → `dockerfile-lint` → `build-and-push`

Depois deles, rodam sempre (mesmo com falhas): `security-summary` (tabela com o resultado de cada gate) e `generate-readme` (gera o `README.md` com os nomes de `alunos.txt` e o resultado do build da imagem, e o publica na `main`).

### 3.3 Passo a passo de execução

1. Criem o repositório a partir do template, como descrito em 3.2, e clonem localmente.

2. Editem o arquivo `alunos.txt`, colocando o nome de cada integrante em uma linha.

3. Façam commit e push para a branch `main`:

   ```bash
   git add .
   git commit -m "chore: identifica os alunos e dispara a pipeline"
   git push origin main
   ```

4. Acessem a aba **Actions** do repositório no GitHub e observem a execução da pipeline **"Pipeline de Seguranca DevSecOps"**.

5. **Nenhum uso de conta externa é necessário**: Gitleaks, Semgrep, Trivy e Hadolint funcionam com o `GITHUB_TOKEN` padrão do próprio Actions.

6. **Atenção — façam `git pull` antes de cada novo push.** Ao final de cada execução na `main`, o job `generate-readme` faz um commit automático do `README.md`. Por isso o repositório remoto fica sempre um commit à frente do local, e um push comum é recusado. Antes de enviar novas correções, rodem:

   ```bash
   git pull --rebase origin main
   ```

### 3.4 Instruções de validação (o que vocês devem observar)

Como os gates rodam em sequência, na primeira execução a pipeline **falha no primeiro gate (`secret-scan`)** e os gates seguintes ficam **pulados** (⚪). A cada correção, o próximo gate é alcançado e falha por sua vez. **Guardem um print de cada falha**: eles são a evidência pedida na avaliação. O job `build-and-push` **não deve rodar** enquanto algum gate não passar.

| Ordem | Job | Situação | Falha reportada quando o gate é alcançado |
|---|---|---|---|
| 1 | `secret-scan` | ❌ Falha já na 1ª execução | Gitleaks encontra a chave AWS (`aws-access-token`) e a chave de pagamento (`stripe-access-token`) em `AppConfig.java` |
| 2 | `unit-tests` | ⚪ Pulado até o gate 1 passar | `PaymentServiceTest` falha: `expected: <180.0> but was: <198.0>` |
| 3 | `sast` | ⚪ Pulado até o gate 2 passar | Semgrep reporta a chave de API no código e uma **SQL Injection** em `AccountController` (duas regras) |
| 4 | `sca` | ⚪ Pulado até o gate 3 passar | Trivy reporta `log4j-core:2.14.1` como `CRITICAL` (Log4Shell, CVE-2021-44228) e outras CVEs `HIGH`/`CRITICAL` (Spring Boot, Tomcat etc.) |
| 5 | `dockerfile-lint` | ⚪ Pulado até o gate 4 passar | Hadolint reporta `DL3007` (tag `latest`) e `DL3002` (`USER root`) |
| 6 | `build-and-push` | ⚪ Não executa | Depende de todos os gates (`needs`) |
| 7 | `security-summary` | ✅ Executa sempre | Mostra, na aba **Summary** da execução, o resultado de cada gate e o veredito geral (BLOQUEADO/LIBERADO) |
| 8 | `generate-readme` | ✅ Executa sempre (push na `main`) | Gera o `README.md` com os nomes dos alunos e o resultado do build (⚪ Pulado enquanto houver falhas) |

Além disso, verifiquem manualmente (revisão de código, não pipeline) que a dependência `com.google.code.gson:gson` no `pom.xml` não é referenciada em nenhum `import` do código-fonte (`grep -r "import com.google.gson" src/` não deve retornar nada) — essa é a falha de **dependência não utilizada**, um problema de higiene de SCA que aumenta a superfície de ataque sem necessidade.

> Os resultados do Trivy dependem da data do banco de vulnerabilidades: com o passar do tempo, novas CVEs podem aparecer.

### 3.5 Etapa de correção (o que entregar)

Corrijam, um a um, os problemas encontrados, e reenviem commits até obter a pipeline totalmente verde e uma imagem publicada em `ghcr.io/<seu-usuario>/banco-facil-api`. No mínimo, os itens 1 a 6:

1. **Segredos:** removam as constantes de `AppConfig.java` e substituam por leitura de variáveis de ambiente (`System.getenv(...)`), documentando que em produção elas viriam de um cofre de segredos (Vault, AWS Secrets Manager etc.) — não do `GITHUB_TOKEN`, que é um segredo de **CI/build**, não de **runtime**. (O `@Value` do Spring só funciona se a classe virar um bean, por exemplo um `@Component` com campos de instância; ele não injeta valores em constantes `static final`.)

   > **Atenção:** só isso não é suficiente. Removendo o segredo do arquivo atual, o job `secret-scan` provavelmente **continuará falhando**, porque o Gitleaks varre todo o *histórico* de commits — e a chave ainda existe nos commits antigos, mesmo depois de apagada do arquivo. Isso não é bug, é o comportamento correto: uma vez commitado, o segredo fica na história do Git para sempre, a menos que ela seja reescrita (`git filter-repo`/BFG + force-push + rotação da credencial real). Como as chaves desta atividade são exemplos públicos e nunca foram credenciais reais, a solução adequada aqui é reconhecer esses achados explicitamente em um arquivo `.gitleaksignore`, usando o fingerprint exato de cada ocorrência (formato `commit:arquivo:regra:linha`, mostrado pelo próprio Gitleaks no log do job `secret-scan`) — em vez de reescrever o histórico, o que só se justifica quando o segredo vazado é real.
   >
   > **Cuidados com o `.gitleaksignore`:**
   > - Não colem o valor da chave em comentários, no README nem no próprio `.gitleaksignore`: o commit que fizer isso passa a conter uma ocorrência nova, com fingerprint próprio.
   > - Se reescreverem commits já publicados (`amend`, `rebase`, `squash` + force-push), os hashes mudam e as entradas deixam de valer; gerem-nas de novo a partir do log.
   > - Alternativa (opcional): um arquivo `.gitleaks.toml` com `[allowlist]` por regex, que não depende de hash de commit. Só use para chaves de exemplo comprovadamente inofensivas.

2. **Teste unitário:** corrijam a fórmula em `PaymentService.applyDiscount` (dividir por `100`, não por `1000`).

3. **SAST:** o Semgrep também reporta uma **SQL Injection** em `AccountController` (a consulta concatena o parâmetro `id` do usuário). Substituam `Statement` + concatenação por `PreparedStatement` com parâmetro `?`. O achado da chave de API desaparece com a correção do item 1.

4. **SCA:** atualizem `log4j-core` para uma versão corrigida (≥ 2.17.1) ou removam a dependência se não for necessária (o `PaymentService` usa apenas a API do log4j, que já acompanha o Spring Boot). **Isso não basta:** o Trivy também reporta CVEs `HIGH`/`CRITICAL` do próprio Spring Boot 3.2.5 e das bibliotecas que ele traz (Tomcat, Spring, Jackson). Atualizem o `spring-boot-starter-parent` para uma versão suportada mais recente (ex.: 3.5.x) e, se o Trivy ainda apontar `tomcat-embed-core`, sobrescrevam a propriedade `tomcat.version` no `pom.xml`. Repitam até o gate passar. A versão "corrigida" citada pelo Trivy pode ainda não estar publicada no Maven Central; nesse caso usem a mais recente disponível.

5. **Dependência não utilizada:** removam `gson` do `pom.xml` (todas as declarações).

6. **Dockerfile:** fixem a tag da imagem base (ex.: `eclipse-temurin:17-jre` ou `eclipse-temurin:17-jre-alpine`) e adicionem um usuário não-root. A receita depende da distribuição da imagem base:

   - **Alpine** (`...-alpine`): `RUN addgroup -S app && adduser -S app -G app`, seguido de `USER app`.
   - **Debian/Ubuntu** (`eclipse-temurin:17-jre`): `RUN groupadd -r app && useradd -r -g app app`, seguido de `USER app`. O `addgroup -S` **não funciona** nessas imagens.

   O Hadolint pode exibir um aviso `DL3066` (nível *info*) para `USER` não numérico; ele não bloqueia o job. Como melhoria adicional, convertam para **multi-stage build** (uma etapa com JDK completo para compilar, outra com apenas o JRE para rodar). Nesse caso, ajustem o `COPY` do `.jar`; o passo "Empacotar aplicacao" do workflow deixa de ser necessário.

7. (OPCIONAL) Configurem, no GitHub, uma **Branch Protection Rule** na branch `main` exigindo que os checks dos cinco gates (`secret-scan`, `unit-tests`, `sast`, `sca` e `dockerfile-lint`) passem antes de permitir merge de qualquer Pull Request — transformando a pipeline em um **Quality Gate** de verdade, e não apenas em um relatório informativo. Observações:

   - Na tela de configuração, o GitHub lista os checks pelo **nome do job** (ex.: `Deteccao de Segredos (Gitleaks)`), e só os exibe depois de terem rodado ao menos uma vez.
   - Em repositório privado, esse recurso depende do plano da conta (contas gratuitas costumam tê-lo apenas em repositórios públicos). Confirmem antes.
   - O job `generate-readme` faz push direto na `main`. Com a regra exigindo Pull Request, esse push provavelmente será recusado e o job falhará. Isso é esperado e não afeta os gates.

> **Dicas:** com JDK 17 e Maven instalados, `mvn test` reproduz localmente o job `unit-tests`. Se o job `sca` falhar com `429 Too Many Requests` ao consultar o Maven Central, é um limite temporário do repositório: reexecutem o job (*Re-run*).

### 3.6 Discussão final, ligando com Shift Right

Depois que a imagem estiver publicada, façam um curto relatório (meia página) respondendo: quais dos controles que vocês implementaram são **exclusivamente Shift Left**, e quais poderiam ser estendidos para **Shift Right** (ex.: rodar um scan DAST com OWASP ZAP contra um ambiente de staging já implantado; monitorar em produção tentativas de exploração da mesma CVE do log4j; usar feature flags para liberar gradualmente uma correção; observabilidade e alertas de comportamento anômalo pós-deploy)? O que a BancoFácil Digital ainda precisaria implementar para fechar o ciclo completo Shift Left + Shift Right?

---

## 4. Entregáveis

**Formato:** Parte 1 individual; Parte 2 em dupla ou trio. Prazo e canal de entrega: **[PREENCHER]**.

* **Parte 1**

  * Documento com a Atividade de Pesquisa Teórica: respostas dissertativas organizadas por seção, referências bibliográficas e pelo menos um diagrama autoral (ver "Entregável da Parte 1", no fim da seção 2).

* **Parte 2**

  * Prints da aba Actions: (a) a primeira execução, com o `secret-scan` falhando; (b) a falha de cada gate seguinte, à medida que forem alcançados; (c) a execução final, totalmente verde, incluindo `build-and-push`.

  * Print da imagem publicada em **Packages** (`ghcr.io/<seu-usuario>/banco-facil-api`).

  * (Opcional) Print da Branch Protection Rule configurada.

  * Link do repositório no GitHub. Se for privado, o professor precisa estar como colaborador (ver 3.2). O `README.md` gerado pela pipeline deve exibir os nomes da dupla/trio e o resultado do build.

  * Documento com a discussão final (3.6).

---

## 5. Critérios de avaliação

| Critério                                                                                                     | Peso |
| ------------------------------------------------------------------------------------------------------------ | ---- |
| Qualidade e profundidade da pesquisa teórica (Parte 1)                                                       | 30%  |
| Pipeline executada e evidência das falhas detectadas (prints da aba Actions)                                 | 30%  |
| Correção efetiva de todos os problemas listados em 3.5 (pipeline verde e imagem publicada)                   | 25%  |
| Discussão final Shift Left/Shift Right + Branch Protection configurada (item opcional)                       | 15%  |

A Branch Protection é opcional. Sem ela, o último critério é avaliado apenas pela discussão final.
