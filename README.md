# Authentication Spring Boot Application

A secure, production-ready authentication and authorization system built with Spring Boot, featuring JWT-based authentication, role-based access control, and comprehensive user management capabilities.

## 🚀 Features

- **JWT Authentication**: Secure token-based authentication with access tokens
- **Refresh Tokens with Rotation**: Long‑lived refresh tokens stored and rotated securely
- **HTTP‑Only Cookie Support**: Refresh tokens can be stored in HTTP‑only, `Secure` cookies via `CookieService`
- **User Registration & Login**: Complete user registration and authentication flow
- **Role-Based Access Control (RBAC)**: Multi-role support with fine-grained permissions
- **User Management**: Full CRUD operations for user management
- **Spring Security Integration**: Robust security configuration with custom filters
- **MySQL Database**: Persistent data storage with JPA/Hibernate
- **RESTful API**: Clean REST API design following best practices
- **Global Exception Handling**: Centralized error handling with proper error responses
- **Environment Profiles**: Support for dev, qa, and prod environments
- **Password Encryption**: BCrypt password encoding for secure password storage
- **User Status Management**: Enable/disable user accounts

## 🛠️ Tech Stack

- **Framework**: Spring Boot 4.0.2
- **Language**: Java 21
- **Security**: Spring Security with JWT (jjwt 0.13.0)
- **Database**: MySQL
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Validation**: Bean Validation
- **Mapping**: ModelMapper 3.2.4
- **Utilities**: Lombok

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## 🔧 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yashrajputs/Authentication-Spring-.git
cd auth-app
```

### 2. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE auth_app_yr;
```

### 3. Configuration

Update the database configuration in `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_app_yr
    username: your_username
    password: your_password
```

### 4. JWT Configuration

Configure JWT settings in `application.yaml` or use environment variables:

```yaml
security:
  jwt:
    secret: your-secret-key-here
    issuer: api.substring.com
    access-ttl-seconds: 3600
    refresh-ttl-seconds: 86400
```

Or set environment variables:
- `JWT_SECRET`: Your JWT secret key
- `JWT_ISSUER`: JWT issuer
- `JWT_ACCESS_TTL_SECONDS`: Access token TTL (default: 3600)
- `JWT_REFRESH_TTL_SECONDS`: Refresh token TTL (default: 86400)

### 5. OAuth2 Configuration (Google & GitHub)

Configure OAuth2 clients in `src/main/resources/application-dev.yml` or via environment variables.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:test-client_id}
            client-secret: ${GOOGLE_CLIENT_SECRET:test-client-secret}
            scope:
              - email
              - profile
              - openid
          github:
            client-id: ${GITHUB_CLIENT_ID:test-client_id}
            client-secret: ${GITHUB_CLIENT_SECRET:test-client-secret}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - user:email
              - read:user
```

Or set environment variables:

- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`

Front‑end redirect URLs after successful/failed OAuth2 login are configured in `application-dev.yml`:

```yaml
app:
  auth:
    frontend:
      success.redirect: http://localhost:5173/auth/success
      faliure.redirect: http://localhost:5173/auth/failure
```

The `OAuth2SuccessHandler` uses `success.redirect` to send the browser back to your front‑end after Google/GitHub login, setting the refresh token cookie on the response.

### 6. Build the Project

```bash
mvn clean install
```

### 7. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/auth-app-0.0.1-SNAPSHOT.jar
```

The application will start on port `8083` (or `8082` in dev profile).

## 📡 API Endpoints

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "roles": ["ROLE_USER"]
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "name": "John Doe",
    "email": "john@example.com",
    "roles": [...]
  }
}
```

On successful login, a refresh token is also attached as an HTTP‑only cookie (by default named `refreshToken`), and `Cache-Control: no-store` headers are added.

#### Refresh Access Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // optional when cookie/header is used
}
```

The refresh token can be provided in multiple ways (checked in this order):

- **HTTP‑only cookie**: The refresh token cookie set during login/previous refresh
- **Request body**: `{"refreshToken": "..."}`
- **Header**: `X-Refresh-Token: <token>`
- **Authorization header**: `Authorization: Bearer <refresh-token>` (only if the token is a refresh token)

**Response:**
```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "name": "John Doe",
    "email": "john@example.com",
    "roles": [...]
  }
}
```

The old refresh token is revoked and replaced with a new one (refresh token rotation). The new refresh token is again attached as an HTTP‑only cookie.

#### Logout
```http
POST /api/v1/auth/logout
```

If a valid refresh token is found (cookie/body/header), it is revoked in the database, the refresh cookie is cleared, cache headers are set to `no-store`, and the Spring Security context is cleared. The endpoint returns HTTP `204 No Content` on success.

### User Management Endpoints

All user endpoints require authentication (JWT token in Authorization header).

#### Get All Users
```http
GET /api/v1/users
Authorization: Bearer {accessToken}
```

#### Get User by ID
```http
GET /api/v1/users/{userId}
Authorization: Bearer {accessToken}
```

#### Get User by Email
```http
GET /api/v1/users/email/{email}
Authorization: Bearer {accessToken}
```

#### Create User
```http
POST /api/v1/users
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "password123",
  "roles": ["ROLE_USER"]
}
```

#### Update User
```http
PUT /api/v1/users/{userId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Jane Smith",
  "email": "jane@example.com"
}
```

#### Delete User
```http
DELETE /api/v1/users/{userId}
Authorization: Bearer {accessToken}
```

## 📁 Project Structure

```
auth-app/
├── src/
│   ├── main/
│   │   ├── java/com/substring/auth/
│   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── ProjectConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controllers/     # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dtos/            # Data Transfer Objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── TokenResponse.java
│   │   │   │   ├── UserDto.java
│   │   │   │   └── ...
│   │   │   ├── entities/        # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   └── Provider.java
│   │   │   ├── exceptions/      # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repositories/    # Data access layer
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/        # Security components
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── services/        # Business logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── impl/
│   │   │   └── AuthAppApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yml
│   │       ├── application-qa.yml
│   │       └── application-prod.yml
│   └── test/                    # Test files
└── pom.xml
```

## 🔐 Security Features

- **JWT Token Authentication**: Stateless authentication using JSON Web Tokens
- **Password Encryption**: BCrypt password hashing
- **Role-Based Authorization**: Multi-role support with Spring Security
- **CORS Configuration**: Cross-origin resource sharing support
- **CSRF Protection**: Disabled for stateless API (can be enabled if needed)
- **Session Management**: Stateless sessions (no server-side session storage)
- **Custom Authentication Filter**: JWT validation filter for protected endpoints
- **User Status Check**: Validates user account is enabled before authentication

## 🌍 Environment Profiles

The application supports multiple environment profiles:

- **dev**: Development environment (port 8082)
- **qa**: Quality assurance environment
- **prod**: Production environment

Set the active profile using:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or set in `application.yaml`:
```yaml
spring:
  profiles:
    active: dev
```

## 🧪 Testing

Run tests using Maven:

```bash
mvn test
```

## 📝 License

This project is open source and available for use.

## 👤 Author

**Yash Rajput**

- GitHub: [@yashrajputs](https://github.com/yashrajputs)

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

## 📞 Support

For support, please open an issue in the GitHub repository.

---

**Note**: Make sure to change default database credentials and JWT secrets before deploying to production!
