# 🚀 CodeCrack - Distributed Online Judge Platform

> A production-grade, FAANG-level distributed online judge backend built with Spring Boot, Docker, Redis, and RabbitMQ.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue)](https://www.docker.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Message%20Queue-orange)](https://www.rabbitmq.com/)
[![Redis](https://img.shields.io/badge/Redis-Cache%20%26%20Leaderboard-red)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Production%20DB-blue)](https://www.postgresql.org/)
[![GCP](https://img.shields.io/badge/GCP-Live%20Deployed-4285F4)](https://codecrack.mooo.com/swagger-ui/index.html)
[![Tests](https://img.shields.io/badge/Tests-121%20Passing-brightgreen)]()
[![Coverage](https://img.shields.io/badge/JaCoCo-35%25-yellow)]()

---

## 🌐 Live Deployment

| | |
|---|---|
| **Live URL** | https://codecrack.mooo.com |
| **Swagger UI** | https://codecrack.mooo.com/swagger-ui/index.html |
| **Region** | asia-south1-c (Mumbai) |
| **SSL** | ✅ HTTPS (Let's Encrypt) |
| **Status** | ✅ LIVE |

---

## 📌 Overview

CodeCrack is a scalable distributed online judge platform similar to LeetCode/HackerRank. It accepts code submissions in multiple languages, executes them in isolated Docker containers, and returns verdicts — all designed for high availability and fault tolerance.

---

## 🏗️ System Architecture

```
Client Request
      │
      ▼
Nginx Reverse Proxy (HTTPS/SSL)
      │
      ▼
Spring Boot API (Port 8080)
      │
      ├── JWT Authentication (HS512)
      ├── Role-Based Access Control (RBAC)
      ├── Input Validation (DTOs + @Valid)
      ├── Rate Limiting (Redis - 10 req/min)
      │
      ▼
RabbitMQ Queue (code.submissions.queue)
      │
      ├── Dead Letter Queue (DLQ) + Retry Logic
      │
      ▼
Submission Worker (RabbitMQ Consumer)
      │
      ▼
Docker Execution Engine
      ├── JavaExecutor  (eclipse-temurin:17-alpine)
      ├── PythonExecutor (python:3.11-alpine)
      └── CppExecutor   (gcc:13)
      │
      ▼
Verdict → PostgreSQL DB + Redis Leaderboard
```

---

## ⚙️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend Framework** | Spring Boot 3.2.1, Java 21 |
| **Security** | Spring Security 6.2, JWT (jjwt 0.12.3, HS512), RBAC |
| **Message Queue** | RabbitMQ with DLQ & retry logic |
| **Cache & Rate Limiting** | Redis (Lettuce client) |
| **Code Execution** | Docker (Java, Python, C++ containers) |
| **Database** | PostgreSQL (prod), H2 in-memory (test) |
| **API Documentation** | Swagger / OpenAPI 3.0 (springdoc) |
| **Monitoring** | Spring Boot Actuator health checks |
| **Validation** | Jakarta Validation, DTOs, GlobalExceptionHandler |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, JaCoCo |
| **Load Testing** | k6 (300 requests, 0% failure, 23ms avg) |
| **Build Tool** | Maven |
| **Containerization** | Docker Compose |
| **Cloud** | Google Cloud Platform (GCP VM, asia-south1-c) |
| **Reverse Proxy** | Nginx + Let's Encrypt SSL |

---

## 🔥 Key Features

### Distributed Code Execution
- Isolated Docker containers per submission (Java, Python, C++)
- Time limit enforcement (TLE detection)
- Memory limit enforcement (MLE detection)
- Code sanitization for dangerous patterns

### Message Queue Architecture
- RabbitMQ with priority queue support
- Dead Letter Queue (DLQ) with retry logic
- Async processing for high throughput

### Security & Auth
- Stateless JWT authentication (HS512)
- Access token + Refresh token support
- Role-Based Access Control (RBAC) with `@PreAuthorize`
- BCrypt password hashing
- Spring Security filter chain

### Rate Limiting
- Redis-based rate limiter per IP
- Returns `429 Too Many Requests` on limit exceeded

### Pagination
- `GET /api/submissions/my` supports `?page=0&size=10`
- Returns totalPages, totalElements, currentPage

### Leaderboard & Analytics
- Real-time Redis-based leaderboard
- User submission history
- Problem difficulty tracking

---

## 📡 API Endpoints

### Authentication
```
POST /api/auth/register    - Register new user
POST /api/auth/login       - Login & get JWT token
POST /api/auth/refresh     - Refresh access token
GET  /api/auth/me          - Get current user info
```

### Problems
```
POST /api/problems                           - Create problem (ADMIN)
GET  /api/problems?page=0&size=10           - Get paginated problems
GET  /api/problems/{id}                     - Get problem by ID
POST /api/problems/{id}/testcases           - Add test cases (ADMIN)
GET  /api/problems/{id}/testcases           - Get test cases
```

### Submissions
```
POST /api/submissions              - Submit code (rate limited)
GET  /api/submissions/{id}         - Get submission verdict
GET  /api/submissions/my?page=0   - Get my submissions (paginated)
```

### Health & Monitoring
```
GET /actuator/health               - Health check
GET /swagger-ui/index.html        - Interactive API docs
GET /v3/api-docs                   - OpenAPI JSON spec
```

---

## 🧪 Supported Languages

| Language | Docker Image | File Extension |
|----------|-------------|----------------|
| Java | `eclipse-temurin:17-alpine` | `.java` |
| Python | `python:3.11-alpine` | `.py` |
| C++ | `gcc:13` | `.cpp` |

---

## 🏆 Verdict Types

| Verdict | Description |
|---------|-------------|
| `ACCEPTED` | All test cases passed |
| `WRONG_ANSWER` | Output mismatch |
| `TIME_LIMIT_EXCEEDED` | Execution timeout |
| `RUNTIME_ERROR` | Runtime crash |
| `COMPILATION_ERROR` | Compile failed |
| `MEMORY_LIMIT_EXCEEDED` | Memory exceeded |
| `PENDING` | Queued for execution |

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Docker Desktop
- Maven 3.8+

### Run Locally

```bash
# Start infrastructure
docker-compose up -d

# Run Spring Boot app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Test Live API

```bash
# Register
curl -X POST https://codecrack.mooo.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"Test@123"}'

# Login
curl -X POST https://codecrack.mooo.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test@123"}'

# Submit code
curl -X POST https://codecrack.mooo.com/api/submissions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"problemId":1,"language":"PYTHON","code":"print(1)"}'
```

---

## 🗃️ Database Schema

```
users          - User accounts & stats
problems       - Problem statements & metadata
test_cases     - Input/output test cases per problem
submissions    - Code submissions & verdicts
```

---

## 📊 Infrastructure

```yaml
Services:
  - Nginx          → Port 80/443 (SSL Termination)
  - Spring Boot    → Port 8080
  - Redis          → Port 6379
  - RabbitMQ       → Port 5672 (UI: 15672)
  - PostgreSQL     → Port 5432
```

---

## 🧪 Testing & Quality

```bash
# Run all tests with coverage
mvn test
```

### Test Coverage Summary

| Package | Coverage |
|---------|----------|
| execution (DockerExecutionService) | 100% |
| filter (RateLimitingFilter) | 100% |
| execution.worker | 98% |
| exception handling | 95% |
| service layer | 62% |
| security | 59% |
| **Overall** | **35%** |

### Test Stats
- **121 tests passing** ✅
- **0 failures** ✅
- **JaCoCo coverage** across 56 classes
- **H2 in-memory DB** for integration tests
- **Mockito** for Redis/RabbitMQ/Docker isolation

### Test Structure
```
src/test/java/com/codecrack/
├── config/ConfigTest.java
├── controller/
│   ├── AuthControllerIntegrationTest.java
│   └── SubmissionControllerTest.java
├── dto/DtoValidationTest.java
├── exception/GlobalExceptionHandlerTest.java
├── execution/
│   ├── DockerExecutionServiceTest.java
│   ├── executor/ExecutorTest.java
│   ├── model/ExecutionModelTest.java
│   └── worker/SubmissionWorkerTest.java
├── filter/RateLimitingFilterTest.java
├── model/ModelTest.java
├── security/
│   ├── JwtAuthFilterTest.java
│   └── JwtUtilTest.java
└── service/
    ├── CodeSanitizationServiceTest.java
    ├── RateLimitServiceTest.java
    ├── RedisServiceTest.java
    ├── SubmissionServiceTest.java
    └── UserServiceTest.java
```

---

## 📈 Load Testing (k6)

```
Tool:       k6 v0.49.0
Target:     https://codecrack.mooo.com
VUs:        10 concurrent users
Duration:   30 seconds
Requests:   300 total
Failures:   0 (0.00% failure rate)
Avg:        23.1ms
p(90):      53.1ms
p(95):      82.96ms
Throughput: 9.7 req/sec
```

---

## 👨‍💻 Author

**Maruthu** — CS Graduate Student (M.Tech)
🔗 [GitHub](https://github.com/Maruthu102002) | 💻 [LeetCode](https://leetcode.com/maruthu2033)

---
