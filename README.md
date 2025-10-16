# Demo Multiple Services - ScalarDB Distributed Transactions

A Spring Boot application demonstrating distributed transaction patterns using ScalarDB. This project showcases three distinct architectural approaches for managing transactions across microservices.

## Overview

This application demonstrates how to implement distributed transactions using ScalarDB with different patterns:

- **Standard Services**: Traditional CRUD operations with local transaction management
- **One-Phase Commit (1PC) Services**: Microservices that participate in distributed transactions
- **BFF (Backend for Frontend) Services**: Coordinators that orchestrate distributed transactions across multiple services

## Prerequisites

- Java 17 or higher
- Gradle 7.x or higher
- ScalarDB Cluster (configured via properties files)
- Access to ScalarDB cluster at `envoy.coe.scalar.local` (or update configuration)

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd demo-multiple-services
```

### 2. Configure ScalarDB

Update the ScalarDB configuration files with your cluster details:

**`scalardb.properties`** - For ScalarDB Java API:
```properties
scalar.db.username=admin
scalar.db.password=admin
scalar.db.transaction_manager=cluster
scalar.db.contact_points=indirect:envoy.coe.scalar.local
scalar.db.cluster.auth.enabled=true
```

**`scalardb_sql.properties`** - For ScalarDB SQL API:
```properties
scalar.db.sql.connection_mode=cluster
scalar.db.sql.cluster_mode.username=admin
scalar.db.sql.cluster_mode.password=admin
scalar.db.sql.cluster_mode.contact_points=indirect:envoy.coe.scalar.local
scalar.db.cluster.auth.enabled=true
```

### 3. Build the application

```bash
./gradlew build
```

### 4. Run the application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 5. Access API Documentation

Once running, visit the Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

## Architecture Patterns

### Pattern 1: Standard Services

**Endpoints**: `/product`, `/order`

Traditional microservice pattern where each service manages its own transactions independently.

**Example - Create a Product**:
```bash
curl -X POST http://localhost:8080/product \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "Laptop",
    "price": 1200.00,
    "stock": 10
  }'
```

**Example - Get a Product**:
```bash
curl http://localhost:8080/product/1
```

**Example - Execute SQL**:
```bash
curl -X POST http://localhost:8080/product/execute-sql \
  -H "Content-Type: application/json" \
  -d '{
    "sqlCommand": "SELECT * FROM product WHERE id = 1"
  }'
```

### Pattern 2: One-Phase Commit (1PC) Services

**Endpoints**: `/product-one-pc`, `/order-one-pc`

Services designed to participate in distributed transactions. They accept a transaction ID via header and resume the existing transaction instead of starting a new one.

**Key Characteristics**:
- Accept `ScalarDB-Transaction-ID` header
- Resume existing transactions (don't start new ones)
- Don't commit transactions (caller is responsible)
- Pure CRUD operations only

**Example** (typically called by BFF services):
```bash
curl -X POST http://localhost:8080/product-one-pc \
  -H "Content-Type: application/json" \
  -H "ScalarDB-Transaction-ID: <transaction-id>" \
  -d '{
    "id": 1,
    "name": "Laptop",
    "price": 1200.00,
    "stock": 10
  }'
```

### Pattern 3: BFF (Backend for Frontend) Services

**Endpoints**: `/product-one-pc-bff`, `/order-one-pc-bff`, `/place-order-one-pc-bff`

Orchestrators that coordinate distributed transactions across multiple 1PC services. They start a transaction, propagate the transaction ID to 1PC services, and handle commit/rollback.

**Transaction Flow**:
1. BFF starts a distributed transaction
2. Extracts transaction ID
3. Calls one or more 1PC services with the transaction ID
4. Commits if all succeed, or rolls back if any fail

**Example - Create a Product via BFF**:
```bash
curl -X POST http://localhost:8080/product-one-pc-bff \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "Laptop",
    "price": 1200.00,
    "stock": 10
  }'
```

**Example - Place an Order (Cross-Service Transaction)**:
```bash
curl -X POST http://localhost:8080/place-order-one-pc-bff \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "productId": 1,
    "quantity": 2,
    "customerId": 123
  }'
```

This demonstrates an atomic operation across multiple services (e.g., creating an order and updating product inventory).

## API Response Format

All endpoints return a standardized `ApiResponse` structure:

**Success Response**:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": 1,
    "name": "Laptop",
    "price": 1200.00,
    "stock": 10
  },
  "errorCode": null,
  "timestamp": "2025-10-16T11:02:42"
}
```

**Error Response**:
```json
{
  "success": false,
  "message": "Product not found",
  "data": null,
  "errorCode": 9100,
  "timestamp": "2025-10-16T11:02:42"
}
```

### Error Codes

| Code | Exception Type | HTTP Status | Description |
|------|---------------|-------------|-------------|
| 9100 | UnsatisfiedConditionException | 400 | Invalid request or constraint violation |
| 9200 | UnknownTransactionStatusException | 500 | Transaction status unknown |
| 9300 | TransactionException | 500 | General transaction error |
| 9400 | RuntimeException | 400 | Runtime error |
| 9500 | Other | 500 | Unexpected error |

## Available Endpoints

### Product Endpoints

#### Standard Service
- `POST /product` - Create product
- `POST /product/upsert` - Create or update product
- `GET /product/{id}` - Get product by ID
- `PUT /product` - Update product
- `DELETE /product/{id}` - Delete product
- `GET /product/scan-by-pk/{id}` - Scan products by partition key
- `GET /product/scan-all` - Get all products
- `POST /product/execute-sql` - Execute custom SQL

#### 1PC Service
- `POST /product-one-pc` - Create product (requires transaction ID)
- `POST /product-one-pc/upsert` - Upsert product (requires transaction ID)
- `GET /product-one-pc/{id}` - Get product (requires transaction ID)
- `PUT /product-one-pc` - Update product (requires transaction ID)
- `DELETE /product-one-pc/{id}` - Delete product (requires transaction ID)
- `GET /product-one-pc/scan-by-pk/{id}` - Scan products (requires transaction ID)
- `GET /product-one-pc/scan-all` - Get all products (requires transaction ID)

#### BFF Service
- `POST /product-one-pc-bff` - Create product with distributed transaction
- `POST /product-one-pc-bff/upsert` - Upsert product with distributed transaction
- `GET /product-one-pc-bff/{id}` - Get product with distributed transaction
- `PUT /product-one-pc-bff` - Update product with distributed transaction
- `DELETE /product-one-pc-bff/{id}` - Delete product with distributed transaction
- `GET /product-one-pc-bff/scan-by-pk/{id}` - Scan products with distributed transaction
- `GET /product-one-pc-bff/scan-all` - Get all products with distributed transaction

### Order Endpoints

Similar endpoints are available for orders:
- `/order/*` - Standard service
- `/order-one-pc/*` - 1PC service
- `/order-one-pc-bff/*` - BFF service

### Generic SQL Endpoint

- `POST /generic-sql/execute` - Execute arbitrary SQL commands

## Development

### Running Tests

```bash
./gradlew test
```

### Running a Single Test

```bash
./gradlew test --tests "com.example.demo_multiple_services.DemoMultipleServicesApplicationTests"
```

### Clean Build

```bash
./gradlew clean build
```

## Technology Stack

- **Spring Boot** 3.5.6 - Application framework
- **ScalarDB** 3.16.1 - Distributed transaction management
- **Lombok** - Reduce boilerplate code
- **ModelMapper** 3.2.2 - Object mapping
- **SpringDoc OpenAPI** 2.3.0 - API documentation
- **Apache Commons Text** 1.13.0 - Text processing utilities

## Project Structure

```
src/
├── main/
│   ├── java/com/example/demo_multiple_services/
│   │   ├── config/          # ScalarDB and RestTemplate configuration
│   │   ├── controller/      # REST controllers (standard, 1PC, BFF)
│   │   ├── service/         # Business logic (standard, 1PC, BFF)
│   │   ├── repository/      # Data access layer
│   │   ├── model/           # Domain entities
│   │   ├── dto/             # Data transfer objects
│   │   ├── mapper/          # DTO-Entity mappers
│   │   ├── exception/       # Custom exceptions
│   │   └── util/            # Utility classes
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/example/demo_multiple_services/
```

## Key Concepts

### Transaction ID Propagation

The distributed transaction flow:

1. **BFF Service** starts a transaction using ScalarDB's `DistributedTransactionManager`
2. **Transaction ID** is extracted from the transaction object
3. **HTTP Header** `ScalarDB-Transaction-ID` is set with the transaction ID
4. **1PC Services** receive the header and resume the transaction
5. **1PC Services** perform operations but don't commit
6. **BFF Service** commits or rolls back based on the overall success

### When to Use Each Pattern

- **Standard Services**: Simple CRUD operations within a single service boundary
- **1PC Services**: When you need to participate in distributed transactions coordinated by a BFF
- **BFF Services**: When orchestrating operations across multiple microservices that must succeed or fail together

