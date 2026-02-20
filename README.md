# Distributed Transactional Saga

A microservices-based order processing system implementing the **Saga Orchestration Pattern** using Spring Boot and Spring Statemachine. The system coordinates distributed transactions across three services — Order, Payment, and Inventory — with automatic compensation (rollback) on failures.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose Network                    │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐   ┌───────────────┐   │
│  │ Order Service │───▶│Payment Service│   │Inventory Svc  │   │
│  │   (8080)      │───▶│   (8081)      │   │   (8082)      │   │
│  │              │    └──────────────┘   └───────────────┘   │
│  │  - REST API  │                                           │
│  │  - State     │    ┌──────────────┐                       │
│  │    Machine   │───▶│ PostgreSQL   │                       │
│  │  - Saga      │    │   (5432)     │                       │
│  │    Orchestr. │    └──────────────┘                       │
│  └──────────────┘                                           │
└─────────────────────────────────────────────────────────────┘
```

### Design Pattern: Saga Orchestration

The **Order Service** acts as the saga orchestrator, using a Spring Statemachine to manage the order lifecycle. Each state transition triggers a corresponding action (e.g., process payment, reserve inventory). If a step fails, compensating transactions are automatically executed to maintain data consistency.

## State Machine Flow

```
ORDER_CREATED ──[CREATE_ORDER]──▶ PAYMENT_PENDING
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                                   │
              [PAYMENT_SUCCESS]                  [PAYMENT_FAILURE]
                    │                                   │
                    ▼                                   ▼
            PAYMENT_COMPLETED                     ORDER_FAILED
                    │
        ┌───────────┼───────────┐
        │                       │
  [INVENTORY_SUCCESS]    [INVENTORY_FAILURE]
        │                       │
        ▼                  (compensate payment)
  INVENTORY_RESERVED            │
        │                       ▼
  [COMPLETE_ORDER]         ORDER_FAILED
        │
        ▼
  ORDER_COMPLETED
```

### Compensation Logic

| Failure Scenario        | Compensating Action            |
|-------------------------|--------------------------------|
| Payment fails           | No compensation needed (nothing to undo) |
| Inventory fails         | Payment is automatically cancelled/refunded |

## Technology Stack

| Component            | Technology                        |
|----------------------|-----------------------------------|
| Language             | Java 17                           |
| Framework            | Spring Boot 3.2.3                 |
| State Machine        | Spring Statemachine 4.0.0-M1      |
| Database             | PostgreSQL 15                     |
| ORM                  | Spring Data JPA / Hibernate 6.4   |
| Containerization     | Docker, Docker Compose            |
| Build Tool           | Maven 3.9                         |

## Project Structure

```
DistributedTransactionalSaga/
├── docker-compose.yml          # Multi-service orchestration
├── .env.example                # Environment variable template
├── .env                        # Environment variables (not committed)
├── submission.json             # Submission metadata
├── README.md                   # This file
│
├── order-service/              # Saga Orchestrator (port 8080)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/orderservice/
│       ├── OrderServiceApplication.java
│       ├── config/
│       │   ├── StateMachineConfig.java       # State machine definition
│       │   └── OrderStateMachineInterceptor.java  # Persists state changes
│       ├── controller/
│       │   └── OrderController.java          # REST API endpoint
│       ├── entity/
│       │   └── Order.java                    # JPA entity
│       ├── repository/
│       │   └── OrderRepository.java          # Data access layer
│       ├── service/
│       │   ├── OrderService.java             # Business logic
│       │   └── SagaActions.java              # Saga step actions
│       └── state/
│           ├── OrderState.java               # State enum
│           └── OrderEvent.java               # Event enum
│
├── payment-service/            # Payment processor (port 8081)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/paymentservice/
│       ├── PaymentServiceApplication.java
│       └── controller/
│           └── PaymentController.java        # Payment endpoints
│
└── inventory-service/          # Inventory manager (port 8082)
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/inventoryservice/
        ├── InventoryServiceApplication.java
        └── controller/
            └── InventoryController.java      # Inventory endpoints
```

## Prerequisites

- **Docker** (20.10+)
- **Docker Compose** (v2+)
- No local Java or Maven installation required (builds happen inside Docker)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd DistributedTransactionalSaga
```

### 2. Configure Environment Variables

```bash
cp .env.example .env
```

Edit `.env` if you want to customize the database credentials (defaults work out of the box).

### 3. Build and Run

```bash
docker compose up --build
```

This will:
- Build all three microservices using multi-stage Docker builds
- Start a PostgreSQL database with health checks
- Launch all services after the database is healthy

### 4. Verify Services Are Running

```bash
docker compose ps
```

All four containers should show status `Up` (or `Healthy` for the database).

## API Endpoints

### Order Service (port 8080)

| Method | Endpoint       | Description        |
|--------|----------------|--------------------|
| POST   | `/api/orders`  | Create a new order |

#### Create Order

**Request:**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "productId": 101,
    "quantity": 5,
    "amount": 100.00
  }'
```

**Response (Success):**
```json
{
  "id": "72562a9a-711d-4449-b7ef-aa191a4520b0",
  "customerId": 1,
  "productId": 101,
  "quantity": 5,
  "amount": 100.0,
  "status": "INVENTORY_RESERVED"
}
```

### Payment Service (port 8081)

| Method | Endpoint           | Description             |
|--------|--------------------|-------------------------|
| POST   | `/payment`         | Process a payment       |
| POST   | `/payment/cancel`  | Cancel/refund a payment |

### Inventory Service (port 8082)

| Method | Endpoint              | Description            |
|--------|-----------------------|------------------------|
| POST   | `/inventory/reserve`  | Reserve inventory      |
| POST   | `/inventory/release`  | Release reserved stock |

## Test Scenarios

### 1. Successful Order (Happy Path)

Payment succeeds (amount ≤ 1000), inventory is reserved (quantity ≤ 100):

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1, "productId":101, "quantity":5, "amount":100.0}'
```

**Expected:** Status = `INVENTORY_RESERVED`  
**Flow:** `ORDER_CREATED → PAYMENT_PENDING → PAYMENT_COMPLETED → INVENTORY_RESERVED`

### 2. Payment Failure

Payment fails when amount > 1000 (insufficient funds):

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1, "productId":101, "quantity":5, "amount":2000.0}'
```

**Expected:** Status = `ORDER_FAILED`  
**Flow:** `ORDER_CREATED → PAYMENT_PENDING → ORDER_FAILED`

### 3. Inventory Failure with Payment Compensation

Payment succeeds but inventory fails (quantity > 100). The saga automatically compensates by cancelling the payment:

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1, "productId":101, "quantity":150, "amount":100.0}'
```

**Expected:** Status = `ORDER_FAILED`  
**Flow:** `ORDER_CREATED → PAYMENT_PENDING → PAYMENT_COMPLETED → ORDER_FAILED` (with payment compensation)

## Environment Variables

| Variable                    | Description                              | Default                           |
|-----------------------------|------------------------------------------|-----------------------------------|
| `DB_NAME`                   | PostgreSQL database name                 | `saga_db`                         |
| `DB_USER`                   | PostgreSQL username                      | `saga_user`                       |
| `DB_PASSWORD`               | PostgreSQL password                      | `saga_pass`                       |
| `PAYMENT_SERVICE_URL`       | Internal URL of payment service          | `http://payment-service:8081`     |
| `INVENTORY_SERVICE_URL`     | Internal URL of inventory service        | `http://inventory-service:8082`   |
| `SPRING_DATASOURCE_URL`     | JDBC connection string (set in compose)  | `jdbc:postgresql://db:5432/saga_db` |
| `SPRING_DATASOURCE_USERNAME`| DB username for Spring (set in compose)  | Value of `DB_USER`                |
| `SPRING_DATASOURCE_PASSWORD`| DB password for Spring (set in compose)  | Value of `DB_PASSWORD`            |
| `SERVER_PORT`               | Server port for each service             | `8080` / `8081` / `8082`         |

## Key Implementation Details

### State Machine Configuration

The state machine is configured with **state entry actions** rather than transition actions. This is critical for Spring Statemachine 4.x because events sent from within transition actions are silently ignored (the machine is mid-transition). State entry actions execute after the machine has settled into the new state, allowing subsequent events to be processed correctly.

### Reactive Event Handling

Spring Statemachine 4.x uses a reactive (Project Reactor) API. All `sendEvent()` calls use `Mono`-based reactive streams with explicit `.subscribe()` to ensure events are actually processed:

```java
stateMachine.sendEvent(Mono.just(message)).subscribe();
```

### State Persistence

The `OrderStateMachineInterceptor` intercepts state transitions and persists the current state to the PostgreSQL database via JPA. This ensures the order status in the database always reflects the current state machine state.

### Inter-Service Communication

Services communicate via synchronous REST calls using `RestTemplate`. The Order Service (saga orchestrator) calls Payment and Inventory services during saga execution. Service URLs are resolved via Docker Compose's built-in DNS.

## Stopping the Application

```bash
# Stop all services
docker compose down

# Stop and remove volumes (reset database)
docker compose down -v
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port already in use | Stop conflicting services or change ports in `docker-compose.yml` |
| Database connection refused | Ensure `db` service is healthy: `docker compose ps` |
| Slow first build | Maven downloads dependencies on first build; subsequent builds use Docker cache |
| Services not finding each other | All services must be on the same Docker network (`saga-net`) |

## License

This project is submitted as part of an academic assignment.
