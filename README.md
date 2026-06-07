# ⚡ Notification Engine – Enterprise-Grade Multi-Channel Gateway

[![Java](https://img.shields.io/badge/Java-24-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A **production-ready notification hub** built for scale – inspired by Twilio, Courier, and SendGrid. Seamlessly send **Email**, **SMS**, **Push**, **Slack**, and **Teams** notifications through a single REST API with built-in retries, rate limiting, webhooks, and a real-time dashboard.

---

## ✨ Why This Project Stands Out (For SDE Interviews)

- ✅ **System Design Depth** – API keys, rate limiting, circuit breaker, retry with backoff.
- ✅ **Production Patterns** – Asynchronous processing, event-driven architecture, audit logging.
- ✅ **Full-Stack Showcase** – Backend REST APIs + real-time WebSocket dashboard with analytics.
- ✅ **Enterprise Features** – Batch sending, template engine, delivery webhooks, and scheduler.
- ✅ **Deployment Ready** – Single JAR, Docker support, and Swagger documentation.

---

## 🚀 Quick Start (Run in 30 Seconds)

```bash
# Clone the repository
git clone https://github.com/Chattanya28/Notification-Engine.git
cd Notification-Engine/notification-gateway

# Build and run with Maven
mvn clean package
java -jar target/notification-gateway-1.0.0.jar

# Open the dashboard
open http://localhost:8080
```

**That’s it!** No external dependencies required – uses in-memory H2 database.

---

## 📚 API Documentation

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/notifications/send` | Send email, SMS, or push notification |
| `POST` | `/api/v1/notifications/batch` | Send up to 100 notifications in one request |
| `POST` | `/api/v1/notifications/schedule` | Schedule a notification with cron expression |
| `GET`  | `/api/v1/notifications/history` | Fetch paginated history with search filters |
| `GET`  | `/api/v1/notifications/stats` | Get delivery analytics (success rate, daily volume) |
| `POST` | `/api/v1/keys/generate` | Generate a new API key for an application |
| `POST` | `/api/v1/webhooks/register` | Register a webhook URL for delivery events |

📖 **Interactive Swagger Docs:** `http://localhost:8080/swagger-ui.html`

---

## 🖥️ Dashboard & Real-Time Monitoring

Access the dashboard at `http://localhost:8080` after starting the application:

- 📊 **Live analytics chart** – notifications per hour (updates via WebSocket)
- 📝 **One-click sending** – with template selector and schedule picker
- 📜 **Real-time history** – auto-refreshing table with search & export to CSV
- 🔑 **API key manager** – create, view, and revoke keys from the UI
- 🌙 **Dark mode toggle** – modern, responsive design

![Dashboard Preview](https://via.placeholder.com/800x450?text=Real-time+Notification+Dashboard)

---

## 🏗️ Architecture & Technical Decisions

### Patterns & Principles
| Pattern | Implementation | Why It Matters |
|---------|----------------|----------------|
| **Circuit Breaker** | Resilience4j | Prevents cascading failures to downstream services |
| **Retry with Backoff** | Spring Retry + Exponential backoff | Handles transient failures gracefully |
| **Rate Limiting** | Token bucket per API key | Protects against abuse and ensures fairness |
| **Async Processing** | `@Async` with ThreadPoolTaskExecutor | Non-blocking, improves throughput |
| **Event-Driven** | `ApplicationEventPublisher` | Decouples notification sending from logging/auditing |

### Tech Stack

```
Backend:     Spring Boot 3.4.0 | Java 24 | Maven
Database:    H2 (in‑memory) → JPA ready for PostgreSQL
Caching:     ConcurrentHashMap (production: Redis)
Frontend:    React CDN | Chart.js | WebSocket (SockJS/STOMP)
Monitoring:  Actuator | Prometheus endpoint | Swagger UI
Container:   Docker + docker-compose.yml
```

---

## 📦 What's Inside (Code Structure)

```
src/main/java/com/notification/engine/
├── controller/       # REST endpoints (notification, key, webhook, schedule)
├── service/          # Business logic with retry & circuit breaker
├── repository/       # JPA repositories for H2
├── model/            # DTOs, entities, enums
├── config/           # Rate limiting, async, webhook, resilience4j
├── event/            # Custom events for audit & webhooks
├── security/         # API key authentication filter
└── scheduler/        # Cron job handler for scheduled notifications
```

---

## 🧪 Testing & Verification

The project includes **5+ unit tests** and **2 integration tests** covering:
- API key validation
- Rate limiting behavior
- Retry logic on failure
- Batch processing correctness

```bash
# Run all tests
mvn test
```

---

## 🐳 Deployment Options

### Option 1: Local (JAR)
```bash
java -jar target/notification-gateway-1.0.0.jar
```

### Option 2: Docker
```bash
docker build -t notification-engine .
docker run -p 8080:8080 notification-engine
```

### Option 3: Docker Compose (app + Redis for rate limiting)
```bash
docker-compose up -d
```

---

## 🤝 Contributing

This project was built as a **Software Development Engineer** portfolio piece. Feedback and suggestions are welcome!

---

## 📄 License

MIT License – free to use, modify, and distribute.

---

## 📫 Contact & Portfolio

**Chattanya** – [GitHub](https://github.com/Chattanya28)
