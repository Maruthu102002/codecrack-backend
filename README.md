 # 🚀 CodeCrack - Distributed Online Judge Platform

> A production-grade, FAANG-level distributed online judge backend built with Spring Boot, Docker, Redis, and RabbitMQ.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue)](https://www.docker.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Message%20Queue-orange)](https://www.rabbitmq.com/)
[![Redis](https://img.shields.io/badge/Redis-Cache%20%26%20Leaderboard-red)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Production%20DB-blue)](https://www.postgresql.org/)
[![GCP](https://img.shields.io/badge/GCP-Live%20Deployed-4285F4)](http://34.14.128.25:8080/actuator/health)

---

## 🌐 Live Deployment

| | |
|---|---|
| **GCP Live URL** | http://34.14.128.25:8080 |
| **Health Check** | http://34.14.128.25:8080/actuator/health |
| **Region** | asia-south1-c (Mumbai) |
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
Spring Boot API (Port 8080)
      │
      ├── JWT Authentication (HS512)
      │
      ├── Rate Limiting (Redis)
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
      ├── JavaExecutor  (openjdk:17-alpine)
      ├── PythonExecutor (python:3.11-alpine)
      └── CppExecutor   (gcc:alpine)
      │
      ▼
Verdict → PostgreSQL DB + Redis Leaderboard
```

---

## ⚙️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend Framework** | Spring Boot 3.2.1, Java 21 |
| **Security** | Spring Security 6.2, JWT (jjwt 0.12.3, HS512) |
| **Message Queue** | RabbitMQ with DLQ & retry logic |
| **Cache & Leaderboard** | Redis (Lettuce client) |
| **Code Execution** | Docker (Java, Python, C++ containers) |
| **Database** | PostgreSQL (prod), H2 file-based (dev) |
| **Monitoring** | Prometheus + Actuator health checks |
| **Build Tool** | Maven |
| **Containerization** | Docker Compose |
| **Cloud** | Google Cloud Platform (GCP VM) |

---

## 🔥 Key Features

### Distributed Code Execution
- Isolated Docker containers per submission (Java, Python, C++)
- Time limit enforcement (TLE detection)
- Memory limit enforcement (MLE detection)
- Auto-scaling worker support

### Message Queue Architecture
- RabbitMQ with priority queue support
- Dead Letter Queue (DLQ) with exponential retry
- Async processing for high throughput

### Security & Auth
- Stateless JWT authentication (HS512, 24h expiry)
- JWT blacklist via Redis
- BCrypt password hashing
- Spring Security filter chain

### Leaderboard & Analytics
- Real-time Redis-based leaderboard
- User submission history
- Problem difficulty tracking
- Acceptance rate calculation

### Monitoring
- Prometheus metrics endpoint
- Spring Boot Actuator health checks
- Centralized logging support

---

## 📡 API Endpoints

### Authentication
```
POST /api/auth/register    - Register new user
POST /api/auth/login       - Login & get JWT token
GET  /api/auth/me          - Get current user info
```

### Problems
```
POST /api/problems                    - Create problem (admin)
GET  /api/problems/{id}              - Get problem by ID
POST /api/problems/{id}/testcases    - Add test cases
```

### Submissions
```
POST /api/submissions                - Submit code
GET  /api/submissions/{id}           - Get submission verdict
GET  /api/submissions/my/{userId}    - Get user submissions
```

### Health & Monitoring
```
GET /actuator/health     - Health check (public)
GET /actuator/metrics    - Prometheus metrics
```

---

## 🧪 Supported Languages

| Language | Docker Image | File Extension |
|----------|-------------|----------------|
| Java | `openjdk:17-alpine` | `.java` |
| Python | `python:3.11-alpine` | `.py` |
| C++ | `gcc:alpine` | `.cpp` |

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
# Health check
curl http://34.14.128.25:8080/actuator/health

# Register
curl -X POST http://34.14.128.25:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"Test@123"}'

# Login
curl -X POST http://34.14.128.25:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test@123"}'

# Submit code
curl -X POST http://34.14.128.25:8080/api/submissions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"problemId":1,"language":"JAVA","code":"class Solution{...}"}'
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
  - Spring Boot API    → Port 8080
  - Redis              → Port 6379
  - RabbitMQ           → Port 5672 (UI: 15672)
  - PostgreSQL         → Port 5432
```

---

## 👨‍💻 Author

**Maruthu** — CS Graduate Student
📍 Tamil Nadu, India
🔗 [GitHub](https://github.com/Maruthu102002)

---
