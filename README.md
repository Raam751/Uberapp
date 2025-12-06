# Uber Ride Sharing Backend

A mini ride sharing backend built with Spring Boot, MongoDB, JWT authentication, input validation, and global exception handling.

## Tech Stack
- Java + Spring Boot
- Spring Web MVC
- Spring Security (JWT auth)
- Spring Data MongoDB
- Jakarta Bean Validation

## Domain Model

### User
- `id: String`
- `username: String`
- `password: String` (BCrypt hashed)
- `role: String` (`ROLE_USER` or `ROLE_DRIVER`)

### Ride
- `id: String`
- `userId: String` (passenger)
- `driverId: String` (driver, nullable until accepted)
- `pickupLocation: String`
- `dropLocation: String`
- `status: String` (`REQUESTED`, `ACCEPTED`, `COMPLETED`)
- `createdAt: Date`

## Folder Structure (main packages)

- `com.Ram.uber.model` – `User`, `Ride`
- `com.Ram.uber.repository` – `UserRepository`, `RideRepository`
- `com.Ram.uber.dto` – `Signup`, `Login`, `CreateRideRequest`
- `com.Ram.uber.controller` – `AuthController`, `RideController`
- `com.Ram.uber.security` – `JwtService`, `JwtAuthenticationFilter`
- `com.Ram.uber.config` – `SecurityConfig`
- `com.Ram.uber.exception` – global error handling

## Configuration

`src/main/resources/application.yaml`:
- MongoDB (change as needed):
  ```yaml
  spring:
    data:
      mongodb:
        uri: mongodb://localhost:27017/rideAppDB
  ```
- (Optional) set port to `8081` to match sample curl:
  ```yaml
  server:
    port: 8081
  ```

## Authentication & JWT

- Register and login via `/api/auth/**`.
- On successful login, API returns a JWT:
  ```json
  { "token": "<jwt>" }
  ```
- Send the token on all protected requests:
  ```http
  Authorization: Bearer <jwt>
  ```
- Token contains:
  - `sub` (username)
  - `role` claim (`ROLE_USER` / `ROLE_DRIVER`)
  - `iat` and `exp`

## API Endpoints

### Auth

#### POST `/api/auth/register`
Create a user or driver.

Request body:
```json
{
  "username": "john",
  "password": "1234",
  "role": "ROLE_USER"   // or ROLE_DRIVER
}
```

Rules:
- Password stored BCrypt-encoded.
- Role must be `ROLE_USER` or `ROLE_DRIVER`.

#### POST `/api/auth/login`
Authenticate and receive JWT.

Request body:
```json
{
  "username": "john",
  "password": "1234"
}
```

Response:
```json
{
  "token": "<jwt>"
}
```

---

### Rides (USER)

#### POST `/api/v1/rides`
Create a ride request (passenger).

Headers:
- `Authorization: Bearer <token of ROLE_USER>`

Body:
```json
{
  "pickupLocation": "Koramangala",
  "dropLocation": "Indiranagar"
}
```

Behavior:
- Must be logged in as `ROLE_USER`.
- `status = REQUESTED`.
- `userId = logged-in user id`.

#### GET `/api/v1/user/rides`
Get rides for the logged-in user.

Headers:
- `Authorization: Bearer <token of ROLE_USER>`

Behavior:
- Filters rides by `userId` = logged-in user.

---

### Rides (DRIVER)

#### GET `/api/v1/driver/rides/requests`
View all pending ride requests.

Headers:
- `Authorization: Bearer <token of ROLE_DRIVER>`

Behavior:
- Returns rides with `status = REQUESTED`.

#### POST `/api/v1/driver/rides/{rideId}/accept`
Accept a ride.

Headers:
- `Authorization: Bearer <token of ROLE_DRIVER>`

Rules:
- Ride must exist.
- Current `status` must be `REQUESTED`.
- Sets `driverId = logged-in driver id`.
- Sets `status = ACCEPTED`.

---

### Complete Ride (USER or DRIVER)

#### POST `/api/v1/rides/{rideId}/complete`

Headers:
- `Authorization: Bearer <token of ROLE_USER or ROLE_DRIVER>`

Rules:
- Ride must exist.
- Current `status` must be `ACCEPTED`.
- Sets `status = COMPLETED`.

## Validation

DTOs use Jakarta Bean Validation (`spring-boot-starter-validation`):
- `@NotBlank`
- `@Size(min = 3)`
- Controllers use `@Valid` on request bodies.

Example:
```java
public class CreateRideRequest {
    @NotBlank(message = "Pickup is required")
    private String pickupLocation;

    @NotBlank(message = "Drop is required")
    private String dropLocation;
}
```

## Global Exception Handling

`com.Ram.uber.exception`:
- `GlobalExceptionHandler`
- `NotFoundException`
- `BadRequestException`

Validation error example response:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Pickup is required",
  "timestamp": "2025-01-20T12:00:00Z"
}
```

## Running the Application

From the project root:

```bash
./mvnw spring-boot:run
```

The app will start on `http://localhost:8080` (or `8081` if configured).

## Sample curl Commands

Register USER:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"1234","role":"ROLE_USER"}'
```

Register DRIVER:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"abcd","role":"ROLE_DRIVER"}'
```

Login:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"1234"}'
```

Create Ride:
```bash
curl -X POST http://localhost:8081/api/v1/rides \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"pickupLocation":"A","dropLocation":"B"}'
```

You can import these endpoints into Postman and attach the JWT in the `Authorization` header as a Bearer token.
