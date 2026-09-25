# CORRECAO (Dockerfile Hardening — DL3007): tag "latest" substituida por uma
# versao fixada (eclipse-temurin:17.0.15_6-jre-alpine).
# Usar "latest" viola DL3007 porque:
#   1. Nao e reproducivel: o mesmo "latest" pode apontar para imagens diferentes
#      ao longo do tempo, introduzindo regressoes ou vulnerabilidades silenciosas.
#   2. Dificulta auditorias: nao ha como saber exatamente qual versao do JRE
#      foi usada em cada build.
# Escolhemos a variante "-alpine" por ser significativamente menor (~90 MB vs
# ~300 MB da variante Debian), reduzindo a superficie de ataque.
FROM eclipse-temurin:17.0.15_6-jre-alpine

WORKDIR /app

COPY target/banco-facil-api-0.0.1-SNAPSHOT.jar app.jar

# CORRECAO (Dockerfile Hardening — DL3002 / Principio do Menor Privilegio):
# o container NAO mais roda como root.
# Um processo root dentro do container pode, em caso de fuga de container
# (container escape), comprometer o host. Um usuario sem privilegios limita
# o raio de explosao de um ataque.
#
# Criamos o grupo "app" e o usuario "app" sem shell de login (-s /sbin/nologin)
# e sem diretorio home (-H), seguindo o padrao Alpine (addgroup/adduser -S).
# Em seguida, ajustamos a propriedade do arquivo .jar para esse usuario.
RUN addgroup -S app && adduser -S app -G app
RUN chown app:app app.jar
USER app

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
