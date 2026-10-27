# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

- **Build project:** `./mvnw clean compile`
- **Run the application:** `./mvnw spring-boot:run`
- **Package application:** `./mvnw clean package`

## Architecture & Design

- **Type:** Server-rendered Spring Boot 3.x modular monolith (Rural Mother & Child Vaccination Tracker / ChildShield).
- **Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA / Hibernate, Thymeleaf, MySQL 8.4 LTS, Lombok, Jakarta Bean Validation (Flyway and unit testing are omitted; functional testing is used instead).
- **Package Structure:** Feature-first, layer-separated modular monolith under `com.kaushik.ChildShield/` where features own their subpackages (`controller`, `dto`, `model`, `repository`, `service`).
- **Access Control & Roles:**
  - `ADMIN`: Manages all villages, health workers, assignments, and records.
  - `HEALTH_WORKER`: Manages beneficiaries (guardians and children), maternal visits, and vaccination records within assigned villages.
  - `PARENT`: Views only their linked children, schedules, and notifications.
- **Database Schema Management:** Managed via Hibernate JPA (`spring.jpa.hibernate.ddl-auto=update` / create) without Flyway.
