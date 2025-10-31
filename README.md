# PassengerFlow

### Overview

PassengerFlow is a Spring Boot web service designed to optimize public transportation by
analyzing and predicting passenger flow on various routes. The system collects real-time data, analyzes it, and provides
forecasting capabilities.

**Technologies:** Java 17, Spring Boot 3.5.5, PostgreSQL, Maven, Testcontainers

## Setup

### Prerequisites

- Java 17 or higher
- PostgreSQL database

### Environment Variables

You need these environment variables to run the app:

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
```

### Running the Application

#### Option 1: Build and Run with Maven

```bash
# Run with Maven
mvn spring-boot:run

# Run with Maven with passing environment variables directly
mvn spring-boot:run

# Using Maven wrapper (Unix/Linux/Mac)
./mvnw spring-boot:run

# Using Maven wrapper (Windows)
mvnw.cmd spring-boot:run
```

#### Option 2: Download Pre-built JAR

Alternatively, you can download a pre-built JAR file from
the [GitHub Releases](https://github.com/ryamal4/passengerflow/releases) page and run it directly:

```bash
java -jar passengerflow.jar
```

#### Option 3: Running with Docker

Run the application using Docker Compose:

```bash
# If you have your own PostgreSQL database
docker-compose -f passengerflow-compose.yml up

# Or run with included PostgreSQL database
# In that case you don't need DB_HOST and DB_PORT env variables
docker-compose -f passengerflow-with-db-compose.yml up
```

## API Endpoints

### Submit Passenger Data

```http
POST /api/passengers
```

Submits passenger count data collected from sensors at bus stops.

**Request Body Fields:**

| Field       | Type    | Required | Description                    | Example               |
|-------------|---------|----------|--------------------------------|-----------------------|
| `bus_id`    | integer | Yes      | Unique identifier for the bus  | `1`                   |
| `stop_id`   | integer | Yes      | Unique identifier for the stop | `1`                   |
| `entered`   | integer | Yes      | Number of passengers entered   | `15`                  |
| `exited`    | integer | Yes      | Number of passengers exited    | `8`                   |
| `timestamp` | string  | Yes      | Timestamp in ISO format        | `2025-12-20T08:30:00` |

**Request Body Example:**

```json
{
  "bus_id": 1,
  "stop_id": 1,
  "entered": 15,
  "exited": 8,
  "timestamp": "2025-12-20T08:30:00"
}
```

---

### Get Specific Prediction

```http
GET /api/predictions?route={route}&time={time}&stop={stop}
```

Returns passenger flow prediction for a specific time and bus stop.

**Parameters:**

| Parameter | Type   | Required | Description      | Example |
|-----------|--------|----------|------------------|---------|
| `route`   | string | Yes      | Route identifier | `7A`    |
| `time`    | string | Yes      | Time in HH:MM    | `15:00` |
| `stop`    | string | Yes      | Bus stop name    | `Kampi` |

---

### Get Daily Route Predictions

```http
GET /api/predictions?route={route}
```

Returns hourly passenger flow predictions for all stops on a route.

**Parameters:**

| Parameter | Type   | Required | Description      | Example |
|-----------|--------|----------|------------------|---------|
| `route`   | string | Yes      | Route identifier | `7A`    |

---

### Find Nearby Stops

```http
GET /api/stops/nearby?lat={latitude}&lon={longitude}
```

Returns nearby bus stops based on geographic coordinates.

**Parameters:**

| Parameter | Type   | Required | Description          | Example   |
|-----------|--------|----------|----------------------|-----------|
| `lat`     | number | Yes      | Latitude coordinate  | `60.3256` |
| `lon`     | number | Yes      | Longitude coordinate | `23.2144` |
