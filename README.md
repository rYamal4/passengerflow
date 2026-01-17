# PassengerFlow

## Overview

PassengerFlow is a Spring Boot web service designed to optimize public transportation by analyzing and predicting passenger flow on various routes. The system collects real-time data, aggregates it, and provides forecasting capabilities with weather adjustments.

**Technologies:** Java 17, Spring Boot 3.5.5, PostgreSQL, Spring Security, JWT, Caffeine Cache, Maven, Testcontainers

## Features

- Real-time passenger data collection and analysis
- Occupancy prediction with weather adjustments
- PDF and Excel report generation
- JWT-based authentication with role-based access control
- Geolocation-based nearby stops search
- Interactive API documentation (Swagger UI)

## Setup

### Prerequisites

- Java 17 or higher
- PostgreSQL database
- Docker (optional)

### Environment Variables

```properties
# Application Configuration
SPRING_PROFILE=dev
SERVER_PORT=8080

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=passengerflow
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT Security Configuration
JWT_TOKEN_SECRET=your_base64_secret

# Telegram Bot (optional)
TELEGRAM_BOT_TOKEN=your_bot_token
```

### Running the Application

#### Option 1: Maven

```bash
# Unix/Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

#### Option 2: Pre-built JAR

Download from [GitHub Releases](https://github.com/ryamal4/passengerflow/releases):

```bash
java -jar passengerflow.jar
```

#### Option 3: Docker Compose

```bash
# With your own PostgreSQL
docker-compose -f passengerflow-compose.yml up

# With included PostgreSQL
docker-compose -f passengerflow-with-db-compose.yml up
```

## API Documentation

Interactive API documentation is available at:
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/login` | User login | No |
| POST | `/api/auth/refresh` | Refresh access token | No |
| POST | `/api/auth/logout` | User logout | Yes |
| GET | `/api/auth/info` | Current user info | Yes |
| PUT | `/api/auth/change_password` | Change password | Yes |

### Passenger Data

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/passengers` | Submit passenger count | Yes |
| GET | `/api/passengers` | List with filters and pagination | Yes |
| GET | `/api/passengers/{id}` | Get by ID | Yes |
| PUT | `/api/passengers/{id}` | Update record | Yes |
| DELETE | `/api/passengers/{id}` | Delete record | Yes |

### Predictions

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/predictions?route={route}` | Daily predictions for route | Yes |
| GET | `/api/predictions?route={route}&stop={stop}&time={time}` | Specific prediction | Yes |

### Stops

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/stops` | List all stops | Yes |
| GET | `/api/stops/nearby?lat={lat}&lon={lon}` | Find nearby stops | Yes |

### Buses

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/buses` | List all buses | Yes |

### Bus Models

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/bus-models/{id}/upload` | Upload bus image | Admin |
| POST | `/api/bus-models/import` | Import from CSV | Admin |

### Reports

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/reports/heatmap` | Generate PDF heatmap report | Yes |
| GET | `/api/reports/heatmap/excel` | Generate Excel heatmap report | Yes |

### Files

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/files/{filename}` | Download file | No |

### Aggregation

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/aggregation` | Trigger manual aggregation | Admin |

## Example Requests

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'
```

### Submit Passenger Data

```bash
curl -X POST http://localhost:8080/api/passengers \
  -H "Content-Type: application/json" \
  -H "Cookie: accessToken=..." \
  -d '{
    "bus_id": 1,
    "stop_id": 1,
    "entered": 15,
    "exited": 8,
    "timestamp": "2025-12-20T08:30:00"
  }'
```

### Get Prediction

```bash
curl "http://localhost:8080/api/predictions?route=7A&stop=Kampi&time=15:00" \
  -H "Cookie: accessToken=..."
```

### Find Nearby Stops

```bash
curl "http://localhost:8080/api/stops/nearby?lat=60.3256&lon=23.2144" \
  -H "Cookie: accessToken=..."
```

## Architecture

```
controller/ → service/ → repository/ → model/
              ↓
            dto/
```

**Key components:**
- **Controllers** — REST API endpoints
- **Services** — Business logic layer
- **Repositories** — Data access layer (Spring Data JPA)
- **DTOs** — Data transfer objects for API contracts
- **JWT Filter** — Token-based authentication
- **Scheduler** — Daily data aggregation job (4:00 AM)

## License

MIT
