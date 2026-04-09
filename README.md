 # 🚀 CodeCrack Backend

A scalable backend system for an online coding platform.

## 🔥 Features

- Async code execution using RabbitMQ
- Redis caching for performance
- JWT-based authentication
- Rate limiting
- Monitoring with Actuator & Prometheus
- Dockerized architecture

## 🏗️ Architecture

User → API → Queue → Worker → Execution → DB + Cache

## 🛠️ Tech Stack

Java, Spring Boot, Redis, RabbitMQ, Docker

## 🚀 Run

docker-compose up -d  
mvn spring-boot:run
