# codecrack-backend
# CodeCrack 🚀

## Overview
AI-powered coding platform backend with scalable architecture.

## Tech Stack
- Spring Boot
- Redis (Caching)
- RabbitMQ (Async processing)
- Docker
- H2 / PostgreSQL

## Features
- Code submission queue (async processing)
- Priority queue support
- Dead Letter Queue (DLQ)
- Retry mechanism
- JWT Authentication
- Rate limiting
- Metrics & Monitoring (Prometheus-ready)

## Architecture
(mention flow)

User → API → Queue → Worker → Result → Cache

## Run Locally
docker-compose up -d
mvn spring-boot:run
