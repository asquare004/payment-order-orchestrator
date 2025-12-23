# Payment Order Orchestrator

This project implements a distributed payment order orchestration system using **Spring Boot**, **Apache Camel**, **Kafka**, and **MongoDB**. The system models the lifecycle of a payment order from ingestion through asynchronous execution, with durable state management and fault handling.

The design follows an **event-driven architecture**, where order processing is decoupled from execution using Kafka, enabling scalability, concurrency, and resilience.

---

## System Architecture

The application consists of two independent services:

### Order Ingestion Service
Responsible for external interaction and orchestration:
- Exposes REST APIs for creating and querying payment orders
- Persists payment orders and their state in MongoDB
- Orchestrates payment lifecycle transitions using Apache Camel
- Publishes execution commands to Kafka

### Order Processor Service
Responsible for asynchronous execution:
- Consumes execution commands from Kafka
- Processes payment orders concurrently
- Updates final order state in MongoDB
- Handles retries and failure scenarios

Each service can be deployed and scaled independently.

---

## Payment Order Lifecycle

A payment order progresses through the following states:

1. **PENDING** – Order created and persisted
2. **VALIDATED** – Basic validation completed
3. **ACCEPTED** – Order accepted for processing
4. **EXECUTING** – Execution command dispatched asynchronously
5. **EXECUTED** – Execution completed successfully  
   **or**
6. **FAILED** – Execution failed after retry exhaustion

All state transitions are persisted in MongoDB and are externally observable via APIs.

---

## Event Flow

1. A client submits a payment order via the REST API.
2. The ingestion service:
    - Persists the initial order state
    - Executes a Camel route to manage state transitions
    - Publishes an execution command to Kafka (`payment.execute`)
3. The processor service:
    - Consumes execution commands from Kafka
    - Executes orders concurrently
    - Updates the order state to `EXECUTED` or `FAILED`
    - Routes failed executions to a Dead Letter Topic

This approach decouples ingestion from execution and allows processing to scale independently.

---

## Key Capabilities

### REST API
- Create payment orders
- Retrieve current order state
- Designed for asynchronous processing semantics

### Apache Camel Orchestration
- Explicit orchestration of the payment lifecycle
- Centralized error handling with retries and exponential backoff
- Clear separation between orchestration and execution concerns

### Kafka Messaging
- Command-based messaging for execution requests
- Dead Letter Topic for failed executions
- Partitioned topics to enable concurrent processing

### Concurrency and Scalability
- Kafka partitions enable parallel processing
- Consumer groups allow horizontal scaling of the processor service
- Safe under retries and message re-delivery

### Idempotent Processing
- Execution logic is idempotent
- Duplicate or replayed messages do not corrupt state
- Ensures correctness under retries and concurrent consumption

### Persistence
- MongoDB used for durable storage of payment orders
- State transitions are explicitly persisted
- System remains consistent across restarts and failures

### Fault Handling
- Automatic retries for transient failures
- Failed executions routed to a Dead Letter Topic
- Orders marked as `FAILED` after retry exhaustion

---

## Technology Stack

- Java 21
- Spring Boot 
- Apache Camel 
- Apache Kafka 
- MongoDB
- Docker & Docker Compose
- Maven

---

## Design Principles

- Event-driven and asynchronous by default
- Clear separation of concerns between services
- Durable state management
- Explicit lifecycle modeling
- Failure isolation and observability

---

## Notes

- Both services can be run locally using Docker Compose.
- Kafka topics are configured with multiple partitions to demonstrate parallel processing.
- The system is designed to remain consistent under retries, failures, and restarts.

---

## Future Enhancements

- Emit and consume processed events (`payment.executed`)
- Introduce schema-based messaging
- Add integration and end-to-end tests
- Add Kubernetes manifests
- Improve operational observability
