# 🚌 PassengerFlow
---

## Overview

A web service designed to optimize public transportation by analyzing and predicting passenger flow on various routes.
The system collects real-time data, analyzes it, and provides forecast.

---

## API endpoints:



#### 1. POST /api/passengers

Submits passenger count data collected from sensors at bus stops.

#### Request Body:

``` json
{
"bus_id": 1,
"stop_id": 1,
"entered": 15,
"exited": 8,
"timestamp": "2025-12-20T08:30:00"
}
```

#### 2. GET /api/predictios?route=7A&time=15:00&stop=Kampi

Submits passenger count data collected from sensors at bus stops.

#### 3. GET /api/predictions?route=7A

Submits passenger count data collected from sensors at bus stops.

#### 4. GET /api/stops/nearby?lat=60.3256&lon=23.2144

Submits passenger count data collected from sensors at bus stops.