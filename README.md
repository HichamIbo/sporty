# Sporty - Live Sports Event Tracker

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.1.0-black)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Build](https://img.shields.io/badge/Build-Passing-success)

A microservice for tracking live sports events and publishing real-time score updates to Apache Kafka. This service periodically fetches event data from external APIs and streams updates to downstream consumers.

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [Running Tests](#-running-tests)
- [API Documentation](#-api-documentation)
- [Design Decisions](#-design-decisions)
- [Docker Support](#-docker-support)
- [Monitoring](#-monitoring)
- [Contributing](#-contributing)

---

## 🚀 Features

- **Live Event Tracking**: Dynamically start/stop tracking sports events
- **Kafka Integration**: Real-time event updates published to Kafka topics
- **Scheduled Updates**: Periodic fetching of score data (configurable interval)
- **Idempotent Publishing**: Guaranteed exactly-once Kafka message delivery
- **REST API**: Simple endpoints to manage event status
- **Mock External API**: Built-in mock API for development and testing
- **Health Check**: Spring Actuator health endpoint at `/actuator/health`
- **OpenAPI Documentation**: Interactive Swagger UI for API exploration
- **Comprehensive Testing**: Unit, integration, and embedded Kafka tests

---

## 🏗️ Architecture

```
┌─────────────────┐
│  REST Client    │
└────────┬────────┘
         │ POST /api/events/status
         ▼
┌─────────────────────────────────────────┐
│       EventController                   │
│  (Validates input, returns response)    │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│     EventManagementService              │
│  (Manages event lifecycle)              │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│     EventSchedulerService               │
│  (Schedules periodic tasks - 10s)       │
└────────┬────────────────────────────────┘
         │ Every 10 seconds
         ▼
┌─────────────────────────────────────────┐
│     EventDataFetchService               │
│  (Orchestrates fetch & publish)         │
└────────┬────────────────────────────────┘
         │
         ├──────────────────┬──────────────────┐
         ▼                  ▼                  ▼
┌────────────────┐  ┌──────────────┐  ┌──────────────────┐
│ ExternalAPI    │  │    Kafka     │  │ KafkaEvent       │
│ Client         │  │   Template   │  │ Publisher        │
│ (Fetches data) │  │              │  │ (Publishes data) │
└────────────────┘  └──────────────┘  └─────────┬────────┘
                                                 ▼
                                        ┌────────────────┐
                                        │ Kafka Topic    │
                                        │ score-updates  │
                                        └────────────────┘
```

### Key Components

| Component | Responsibility |
|-----------|---------------|
| **EventController** | REST endpoints for event status management |
| **EventManagementService** | Event lifecycle and state management |
| **EventSchedulerService** | Scheduled periodic data fetching (10-second intervals) |
| **EventDataFetchService** | Orchestrates fetching from external API and publishing to Kafka |
| **ExternalApiClient** | WebClient-based HTTP client for external API calls |
| **KafkaEventPublisher** | Kafka message publishing with idempotent producer |
| **MockExternalApiController** | Development mock API for testing |

---

## 📦 Prerequisites

- **Java 17** or higher
- **Maven 3.6+** 
- **Docker & Docker Compose** (for Kafka)
- **Git** (for cloning)

---

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Sporty
```

### 2. Start Kafka Infrastructure

```bash
docker-compose up -d
```

This will start:
- Zookeeper on port `2181`
- Kafka broker on port `9092`

### 3. Build the Application

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 5. Test the API

```bash
# Set an event to LIVE (starts publishing)
curl -X POST http://localhost:8080/api/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId":"event-123","status":"live"}'

# Get event status
curl http://localhost:8080/api/events/event-123/status

# Set event to NOT_LIVE (stops publishing)
curl -X POST http://localhost:8080/api/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId":"event-123","status":"not_live"}'
```

---

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Application HTTP port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker addresses |
| `KAFKA_TOPIC_SCORE_UPDATES` | `sports-score-updates` | Kafka topic name |
| `KAFKA_TOPIC_PARTITIONS` | `3` | Number of topic partitions |
| `KAFKA_REPLICATION_FACTOR` | `1` | Topic replication factor |
| `KAFKA_PUBLISH_TIMEOUT` | `5000` | Kafka publish timeout (ms) |
| `EXTERNAL_API_BASE_URL` | `http://localhost:8080` | External API endpoint |
| `EXTERNAL_API_TIMEOUT` | `5000` | API request timeout (ms) |
| `EXTERNAL_API_MAX_RETRIES` | `2` | Max retry attempts |
| `MOCK_EXTERNAL_API_ENABLED` | `true` | Enable mock API |

### Application Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

kafka:
  topic:
    score-updates: sports-score-updates
    partitions: 3
    replication-factor: 1

external:
  api:
    base-url: http://localhost:8080
    timeout: 5000
    max-retries: 2
```

---

## 🏃 Running the Application

### Option 1: Maven (Development)

```bash
mvn spring-boot:run
```

### Option 2: JAR File (Production)

```bash
# Build
mvn clean package

# Run
java -jar target/Sporty-1.0-SNAPSHOT.jar
```

### Option 3: Docker

```bash
# Build image
docker build -t sporty:latest .

# Run container
docker run -p 8080:8080 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  --network sporty-network \
  sporty:latest
```

### Option 4: Docker Compose (Full Stack)

```bash
# Start everything (Kafka + Sporty)
docker-compose up -d

# View logs
docker-compose logs -f sporty

# Stop everything
docker-compose down
```

---

## 🧪 Running Tests

The project includes comprehensive test coverage with unit, integration, and embedded Kafka tests.

### Run All Tests

```bash
mvn test
```

**Expected Output:**
```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Breakdown

| Test Suite | Tests | Type | Description |
|------------|-------|------|-------------|
| **EventControllerTest** | 7 | Unit | REST API endpoint validation |
| **EventManagementServiceTest** | 7 | Unit | Event lifecycle management |
| **EventSchedulerServiceTest** | 8 | Unit | Task scheduling logic |
| **EventDataFetchServiceTest** | 5 | Unit | Data fetch orchestration |
| **KafkaEventPublisherTest** | 3 | Unit | Kafka publishing logic |
| **EventTrackingIntegrationTest** | 3 | Integration | End-to-end flow with embedded Kafka |

### Run Specific Test Class

```bash
mvn test -Dtest=EventControllerTest
mvn test -Dtest=EventTrackingIntegrationTest
```

### Run Tests with Coverage

```bash
mvn clean test jacoco:report
```

Coverage report will be available at: `target/site/jacoco/index.html`

### Integration Tests

The integration tests use **Spring Kafka Test** with embedded Kafka broker:

```bash
# Run only integration tests
mvn test -Dtest=EventTrackingIntegrationTest

# These tests verify:
# - HTTP endpoints return correct responses
# - Kafka messages are actually published to topics
# - Message content is correctly serialized
# - Events stop publishing when set to NOT_LIVE
```

---

## 📚 API Documentation

### Interactive Documentation

Once the application is running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### REST Endpoints

#### Update Event Status

**POST** `/api/events/status`

Set an event to LIVE or NOT_LIVE status.

**Request:**
```json
{
  "eventId": "event-123",
  "status": "live"
}
```

**Response:**
```json
{
  "eventId": "event-123",
  "status": "live",
  "lastUpdated": "2025-12-18T00:00:00Z",
  "message": "Event event-123 is now LIVE. Updates will be published every 10 seconds."
}
```

**Status Values:**
- `live` - Start tracking and publishing updates
- `not_live` - Stop tracking and publishing

---

#### Get Event Status

**GET** `/api/events/{eventId}/status`

Retrieve current status of an event.

**Response:**
```json
{
  "eventId": "event-123",
  "status": "live",
  "lastUpdated": "2025-12-18T00:00:00Z"
}
```

---

### Health Endpoint

#### Health Check

**GET** `/actuator/health`

Check the application health status.

**Response:**
```json
{
  "status": "UP"
}
```

---

## 🎯 Design Decisions

### 1. Architecture Pattern: Layered Architecture

**Decision**: Use a clean layered architecture with clear separation of concerns.

**Layers:**
- **API Layer** (`api.controller`, `api.dto`) - REST endpoints and data transfer objects
- **Service Layer** (`service`) - Business logic and orchestration
- **Integration Layer** (`integration`) - External API clients and Kafka publishers
- **Domain Layer** (`domain.model`) - Core business entities

**Rationale:**
- Clear separation of concerns
- Easy to test each layer independently
- Better maintainability and scalability
- Follows Spring Boot best practices

---

### 2. Event Scheduling: Spring Task Scheduler

**Decision**: Use Spring's `ThreadPoolTaskScheduler` for periodic event updates.

**Implementation:**
- Each LIVE event gets its own scheduled task
- Tasks run every 10 seconds
- Tasks are cancelled when events transition to NOT_LIVE
- Thread pool size: 10 (configurable)

**Rationale:**
- Built-in Spring framework component (no external dependencies)
- Thread-safe and production-ready
- Easy cancellation and rescheduling
- Suitable for hundreds of concurrent live events

**Alternatives Considered:**
- ❌ `@Scheduled` annotation - Not dynamic, can't schedule/unschedule at runtime
- ❌ Quartz Scheduler - Too heavyweight for this use case
- ❌ Manual thread management - Reinventing the wheel

---

### 3. Kafka Configuration: Idempotent Producer

**Decision**: Enable Kafka idempotent producer with `acks=all`.

**Configuration:**
```java
ENABLE_IDEMPOTENCE_CONFIG = true
ACKS_CONFIG = "all"
RETRIES_CONFIG = 3
```

**Rationale:**
- Prevents duplicate messages during retries
- Ensures exactly-once delivery semantics
- Safe for retry logic (won't create duplicates)
- Industry best practice for critical data

**Trade-offs:**
- Slightly higher latency (waiting for all replicas)
- Higher reliability and data consistency

---

### 4. External API Client: WebClient (Reactive)

**Decision**: Use Spring WebFlux `WebClient` instead of `RestTemplate`.

**Rationale:**
- Non-blocking I/O (better performance)
- Modern Spring recommendation (RestTemplate is in maintenance mode)
- Better timeout and retry control
- Reactive streams support (for future scalability)

**Configuration:**
- Timeout: 5 seconds
- Max retries: 2
- Exponential backoff: Not implemented (kept simple)

---

### 5. Message Format: JSON

**Decision**: Use JSON for Kafka messages with Jackson serialization.

**Message Structure:**
```json
{
  "eventId": "event-123",
  "currentScore": "2:1",
  "timestamp": "2025-12-18T00:00:00Z"
}
```

**Rationale:**
- Human-readable for debugging
- Wide ecosystem support
- Easy schema evolution
- Built-in Spring Boot support

**Alternatives Considered:**
- ❌ Avro - Requires schema registry, too complex for this use case
- ❌ Protobuf - Better performance but less debugging-friendly
- ✅ JSON - Best balance of simplicity and functionality

---

### 6. Error Handling: Multi-Level Strategy

**Decision**: Implement error handling at multiple levels.

**Strategy:**
1. **Global Exception Handler** - Catches unhandled exceptions, returns proper HTTP status codes
2. **Service Level** - Logs errors and wraps in runtime exceptions
3. **Kafka Producer** - Built-in retries (3 attempts with backoff)

**HTTP Status Codes:**
- `200 OK` - Successful operation
- `400 BAD REQUEST` - Invalid input (validation errors, malformed JSON)
- `404 NOT FOUND` - Event not found
- `500 INTERNAL SERVER ERROR` - Unexpected errors

**Rationale:**
- Fail fast with clear error messages
- Prevent cascading failures
- Proper HTTP semantics for REST API
- Detailed logging for debugging

---

### 7. State Management: In-Memory ConcurrentHashMap

**Decision**: Store event state in-memory using `ConcurrentHashMap`.

**Rationale:**
- Simple and fast (no external database needed)
- Thread-safe for concurrent access
- Sufficient for MVP/prototype
- Easy to replace with Redis/Database later

**Limitations:**
- State lost on restart
- Not suitable for distributed deployment
- No persistence

**Future Enhancement:**
- Replace with Redis for distributed state
- Add persistence layer (PostgreSQL/MongoDB)

---

### 8. Testing Strategy: Comprehensive Coverage

**Decision**: Implement unit tests, integration tests, and embedded Kafka tests.

**Test Types:**

1. **Unit Tests** (Mocked dependencies)
   - Fast execution
   - Test individual components
   - 100% code coverage for business logic

2. **Integration Tests** (Embedded Kafka)
   - Test end-to-end flow
   - Verify actual Kafka message publishing
   - Realistic scenarios

3. **Contract Tests** (MockMvc)
   - Validate API contracts
   - Test HTTP request/response
   - Input validation

**Rationale:**
- High confidence in code quality
- Catch bugs early
- Safe refactoring
- Documentation through tests

---

### 9. Mock External API: Built-in Development Mode

**Decision**: Include a mock external API controller for development.

**Features:**
- Random score generation
- Simulated API errors
- Configurable via `mock.external-api.enabled=true`

**Rationale:**
- No dependency on external services during development
- Easier onboarding for new developers
- Faster local testing
- Predictable behavior in tests

---

### 10. Configuration: Externalized & Environment-Based

**Decision**: Use Spring Boot's externalized configuration with environment variables.

**Approach:**
```yaml
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

**Rationale:**
- 12-factor app methodology
- Easy to deploy to different environments
- No code changes for configuration
- Docker-friendly

**Environments Supported:**
- Local development (defaults)
- Docker Compose (environment variables)
- Kubernetes (ConfigMaps/Secrets)

---

### 11. API Design: RESTful Principles

**Decision**: Follow REST best practices.

**Principles Applied:**
- Resource-based URLs (`/api/events/{eventId}`)
- HTTP verbs match operations (GET, POST)
- Proper status codes (200, 400, 404, 500)
- JSON request/response bodies
- Idempotent operations where possible

**Rationale:**
- Industry standard
- Easy to understand and use
- Wide tooling support
- Self-documenting with OpenAPI

---

### 12. Kafka Topic Design: Partitioning by Event ID

**Decision**: Use event ID as the message key for Kafka partitioning.

**Implementation:**
```java
kafkaTemplate.send(topic, eventId, messageJson)
```

**Benefits:**
- Events for same eventId always go to same partition (ordering guaranteed)
- Natural load distribution across partitions
- Enables partition-based parallel processing

**Configuration:**
- 3 partitions (configurable)
- Replication factor: 1 (increase for production)

---

### 13. Logging: SLF4J with Logback

**Decision**: Use SLF4J facade with Logback implementation.

**Log Levels:**
- `INFO` - Important state changes (event transitions, successful publishes)
- `DEBUG` - Detailed operation tracking
- `WARN` - Recoverable errors (retries, validation failures)
- `ERROR` - Unrecoverable errors (Kafka failures, unexpected exceptions)

**Rationale:**
- Spring Boot default
- Industry standard
- Excellent performance
- Flexible configuration

---

## 🐳 Docker Support

### Dockerfile

The project includes a multi-stage Dockerfile for optimized builds:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

Full stack deployment with Kafka:

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
    
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
      
  sporty:
    build: .
    ports:
      - "8080:8080"
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9093
    depends_on:
      - kafka
```

**Commands:**
```bash
# Start full stack
docker-compose up -d

# View logs
docker-compose logs -f sporty

# Stop and remove
docker-compose down -v
```

---

## 📊 Monitoring

### Health Check

The application exposes a health check endpoint via Spring Boot Actuator:

```bash
# Check application health status
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### Kafka Monitoring

Monitor Kafka messages:

```bash
# List topics
docker exec sporty-kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume messages
docker exec sporty-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic sports-score-updates \
  --from-beginning \
  --property print.key=true \
  --property key.separator=": "
```

---

## 🔧 Development

### Project Structure

```
Sporty/
├── src/
│   ├── main/
│   │   ├── java/org/example/sporty/
│   │   │   ├── api/                 # REST controllers & DTOs
│   │   │   ├── config/              # Spring configuration
│   │   │   ├── domain/              # Domain models
│   │   │   ├── integration/         # External integrations
│   │   │   ├── mock/                # Mock APIs
│   │   │   ├── service/             # Business logic
│   │   │   └── SportyApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/org/example/sporty/
│       │   ├── api/                 # Controller tests
│       │   ├── integration/         # Integration tests
│       │   └── service/             # Service tests
│       └── resources/
│           └── application-test.yml
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

### Code Quality

```bash
# Run tests with coverage
mvn clean test jacoco:report

# Check for dependency updates
mvn versions:display-dependency-updates

# Format code (if configured)
mvn spotless:apply
```

