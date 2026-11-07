# ADR-008: Java 25 e Spring Boot 4.1

Status: aceita (Fase 1, 07/11/2026)

## Contexto
O prompt da Fase 1 supunha Java 21 e Maven, e o documento técnico não fixa versões. A máquina de desenvolvimento só tem o JDK 25 (LTS) instalado, e o Maven não está instalado.

## Decisão
Java 25, Spring Boot 4.1.2 (última versão estável disponível no Maven Central em 07/11/2026) e Maven pelo Maven Wrapper (`mvnw`) dentro de `backend/`. O CI usa Temurin 25.

## Consequências
Sem instalar JDK adicional nem Maven global; versão LTS. Spring Boot 4 usa módulos de autoconfiguração com nomes de pacote novos (por exemplo `org.springframework.boot.flyway.autoconfigure`), e parte da documentação antiga não se aplica. Se preferir Java 21, basta trocar `java.version` no `pom.xml` e `java-version` no workflow, desde que haja um JDK 21 instalado.

## Alternativa descartada
Java 21 com toolchain: exigiria instalar um segundo JDK só para o build.
