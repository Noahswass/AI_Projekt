# ui-ai: Setup Guide

Goal: a Spring Boot 3.3.x / Maven app named `ui-ai` that builds, starts, reports healthy, connects to PostgreSQL or H2, and can reach the Rebrickable API and the LLM API. Java 21.

> Renamed from the earlier `ai-projekt` skeleton. Artifact `ui-ai`, package `com.example.uiai`. If you already created the old package, rename it in IntelliJ (Refactor → Rename) before continuing.

## 1. Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 21 (LTS) | `java -version` |
| Maven | 3.9+ | `mvn -v` (must report the same JDK 21) |
| Git | any recent | `git --version` |
| Docker | optional, for PostgreSQL | `docker --version` |
| IDE | IntelliJ IDEA (Community is enough) | |
| Rebrickable API key | create in your Rebrickable account | stored in `.env`, never in git |
| LLM API key | Claude Console | stored in `.env`, never in git |

IntelliJ settings:
- **Settings → Build Tools → Maven → Importing → JDK for importer** = 21.
- **Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing** (required for Lombok).

## 2. Create the repository

```bash
mkdir ui-ai && cd ui-ai
git init -b main
mkdir -p src/main/java/com/example/uiai/{config,security,catalog,rebrickable,sync,collection,ai,web,error}
mkdir -p src/main/resources/{templates,static,db/migration}
mkdir -p src/test/java/com/example/uiai
```

### `.gitignore`

```gitignore
target/
.idea/
*.iml
.vscode/
.env
*.log
.DS_Store
```

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version> <!-- use the latest 3.3.x patch -->
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>ui-ai</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>ui-ai</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Web + UI -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Persistence -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Ops -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Boilerplate reduction -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock-standalone</artifactId>
            <version>3.9.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Spring Boot manages the versions of Spring, Lombok, Flyway, H2 and PostgreSQL. Only WireMock has an explicit version.

## 3. Configuration

### `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: ui-ai
  datasource:
    url: ${DB_URL:jdbc:h2:mem:uiai;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate            # Flyway owns the schema
  flyway:
    enabled: true
    locations: classpath:db/migration
  thymeleaf:
    cache: false

app:
  rebrickable:
    base-url: ${REBRICKABLE_BASE_URL:https://rebrickable.com/api/v3}
    api-key: ${REBRICKABLE_API_KEY:}
    timeout: 10s
  sync:
    cron: "0 0 2 * * *"            # 02:00 every day
    zone: Europe/Copenhagen
  security:
    encryption-key: ${APP_ENCRYPTION_KEY:}   # 32-byte key, base64. Needed for Rebrickable tokens (Sprint 2)
  llm:
    base-url: ${LLM_BASE_URL:https://api.anthropic.com}
    api-key: ${LLM_API_KEY:}
    model: ${LLM_MODEL:claude-haiku-5-5}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### `src/main/resources/application-dev.yml`

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
logging:
  level:
    com.example.uiai: DEBUG
```

### `.env.example` (commit this, not `.env`)

```bash
REBRICKABLE_API_KEY=replace-me
LLM_API_KEY=replace-me
LLM_MODEL=claude-haiku-5-5
APP_ENCRYPTION_KEY=replace-with-base64-32-bytes
SPRING_PROFILES_ACTIVE=dev
```

Generate the encryption key once (needed from Sprint 2, harmless before):

```bash
openssl rand -base64 32
```

Load `.env` before running:

```bash
set -a; source .env; set +a          # macOS / Linux
```

PowerShell:

```powershell
Get-Content .env | ForEach-Object { if ($_ -match '^(\w+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }
```

IntelliJ alternative: **Run → Edit Configurations → Environment variables**.

## 4. Verify the external APIs

Rebrickable authenticates with a header named `Authorization` whose value starts with the text `key ` followed by your key (the query parameter `key=` also works, but the header keeps the key out of logs).

```bash
curl -s -H "Authorization: key $REBRICKABLE_API_KEY" "https://rebrickable.com/api/v3/lego/colors/?page_size=5"
```

Check the LLM key:

```bash
curl -s https://api.anthropic.com/v1/messages \
  -H "x-api-key: $LLM_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{"model":"claude-haiku-5-5","max_tokens":20,"messages":[{"role":"user","content":"Say hi"}]}'
```

Save one sample response from each into `src/test/resources/fixtures/` for WireMock tests. Do not paste keys into files, chats or commits. If a key leaks, revoke it and create a new one.

## 5. Minimal code to prove the setup

### `src/main/java/com/example/uiai/UiAiApplication.java`

```java
package com.example.uiai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class UiAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UiAiApplication.class, args);
    }
}
```

### `src/main/java/com/example/uiai/config/RebrickableProperties.java`

```java
package com.example.uiai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rebrickable")
public record RebrickableProperties(String baseUrl, String apiKey, Duration timeout) {
}
```

### `src/main/java/com/example/uiai/config/HttpClientConfig.java`

```java
package com.example.uiai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final RebrickableProperties rebrickable;

    @Bean
    RestClient rebrickableRestClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(rebrickable.timeout());
        factory.setReadTimeout(rebrickable.timeout());
        return builder
                .baseUrl(rebrickable.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "key " + rebrickable.apiKey())
                .build();
    }
}
```

The LLM client gets its own `RestClient` bean in Sprint 2 (`LlmProperties`). Do not share one `RestClient` between upstreams.

### `src/main/java/com/example/uiai/error/GlobalExceptionHandler.java`

```java
package com.example.uiai.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        // Log the stack trace server-side; never expose it to the client.
        var problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Unexpected error");
        problem.setDetail("Something went wrong. Check the server logs for details.");
        return problem;
    }
}
```

### `src/main/resources/db/migration/V1__init.sql`

Placeholder so Flyway has a first migration. The real catalog tables come on Tuesday (PB-03).

```sql
-- V1: baseline. Catalog and user tables are added in V2 and V3.
SELECT 1;
```

### `src/test/java/com/example/uiai/UiAiApplicationTests.java`

```java
package com.example.uiai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UiAiApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Run the build and the app:

```bash
mvn clean verify
mvn spring-boot:run
```

## 6. Verify the setup

```bash
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}
```

Checklist:
- [ ] `java -version` and `mvn -v` both show Java 21
- [ ] `mvn clean verify` passes, including `contextLoads`
- [ ] `/actuator/health` returns `UP`
- [ ] H2 console opens at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:uiai`, user `sa`, blank password)
- [ ] Rebrickable `curl` returns colours
- [ ] LLM `curl` returns a reply
- [ ] Sample responses saved under `src/test/resources/fixtures/`

## 7. PostgreSQL (recommended before Sprint 1 Friday)

### `docker-compose.yml`

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: uiai
      POSTGRES_USER: uiai
      POSTGRES_PASSWORD: uiai
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
volumes:
  pgdata:
```

```bash
docker compose up -d db
export DB_URL=jdbc:postgresql://localhost:5432/uiai
export DB_USERNAME=uiai
export DB_PASSWORD=uiai
mvn spring-boot:run
```

Flyway runs on startup. Use H2 for quick tests and Postgres for anything you want to keep.

## 8. Git and first commit

```bash
git add .
git commit -m "chore: bootstrap ui-ai with health endpoint, Flyway and Rebrickable client config"
```

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `invalid target release: 21` | Maven running on an older JDK | Set `JAVA_HOME` to JDK 21, check `mvn -v` |
| `cannot find symbol` for getters or constructors | Lombok annotation processing off | Enable annotation processing in IntelliJ, then `mvn clean compile` |
| Port 8080 in use | Another process | `server.port=8081` or stop the other process |
| Rebrickable `401` | Missing or wrong key, or the `key ` prefix is missing | Check `.env` is loaded. Header must be `Authorization: key <KEY>` |
| Rebrickable `429` | Rate limit hit | Back off and retry. Reduce sync page sizes |
| LLM `401` or `404 model` | Wrong key or model string | Check `LLM_API_KEY`. Use `claude-haiku-5-5` or the current one from the provider docs |
| Flyway: `Unsupported Database: PostgreSQL` | Missing `flyway-database-postgresql` | It is in the pom. Run `mvn clean compile` again |
| `Schema-validation: missing table` | Migration not added | Add a `V<n>__*.sql` file in `db/migration`. Do not switch to `ddl-auto=update` |
| `Failed to determine a suitable driver class` | Wrong `DB_URL` | Check the scheme (`jdbc:postgresql://` or `jdbc:h2:`) |
| Health shows `DOWN` | A dependency check failed | Run with `--debug` and read the first error |

## 10. You are ready when

- The app starts with one command and `/actuator/health` returns `UP`.
- `mvn clean verify` is green.
- Both API keys work from `curl`.
- Code is committed and no secrets are in git.

Next: Sprint 1, Tuesday (PB-03 Flyway schema, PB-04 Rebrickable client). See `sprint-and-product-backlog.md`.
