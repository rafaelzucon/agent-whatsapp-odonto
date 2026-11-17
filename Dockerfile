# syntax=docker/dockerfile:1
# Imagem única: aplicação Java 21 + PostgreSQL 18 no mesmo contêiner (ideal para um consultório, um servidor).

# ---------- Etapa 1: build com Maven Wrapper ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /fonte
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
ARG PULAR_TESTES=true
RUN ./mvnw -B -q package -DskipTests=${PULAR_TESTES} && cp target/agent-whatsapp-odonto-*.jar /fonte/app.jar

# ---------- Etapa 2: runtime (PostgreSQL 18 oficial + JRE 21) ----------
FROM postgres:18
ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:$PATH \
    LANG=C.UTF-8 \
    TZ=America/Sao_Paulo \
    POSTGRES_DB=odonto \
    POSTGRES_USER=odonto
COPY --from=eclipse-temurin:21-jre /opt/java/openjdk /opt/java/openjdk
RUN groupadd --system odonto && useradd --system --gid odonto --home-dir /app --shell /usr/sbin/nologin odonto \
    && mkdir -p /app && chown odonto:odonto /app
WORKDIR /app
COPY --from=build --chown=odonto:odonto /fonte/app.jar /app/app.jar
COPY --chmod=755 docker/entrypoint.sh /usr/local/bin/odonto-entrypoint.sh

EXPOSE 8080
VOLUME ["/var/lib/postgresql"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORTA:-8080} && printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
STOPSIGNAL SIGTERM
ENTRYPOINT ["odonto-entrypoint.sh"]
