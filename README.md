# Authentication Service for JobHub

A robust, secure authentication and user management microservice built with Spring Boot, providing JWT-based authentication, user profiles, MFA support, and comprehensive security features.

## 🚀 Features

- **JWT Authentication**: Secure token-based authentication with RSA signing
- **User Management**: Registration, login, profile management, and account deactivation
- **Multi-Factor Authentication (MFA)**: TOTP-based two-factor authentication
- **Rate Limiting**: Protection against brute force attacks using Bucket4j
- **Email Verification**: Account verification and password reset via email
- **Audit Logging**: Comprehensive security event logging
- **Role-Based Access Control**: User roles and permissions system
- **Password Security**: Strong password policies with bcrypt hashing
- **Session Management**: Refresh token rotation and session tracking

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.x
- **Security**: Spring Security, JWT (RS256), BCrypt
- **Database**: PostgreSQL with Flyway migrations
- **Rate Limiting**: Bucket4j
- **Build Tool**: Maven
- **Java Version**: 17+

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- SMTP server (for email functionality)

## 🔧 Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Authentication-Service-JobHub
```

### 2. Configure Database
Create a PostgreSQL database named `AuthenticationDb` and a user with appropriate permissions.

### 3. Configure Application Properties
Copy the template and configure your settings:

```bash
cp application.properties.template src/main/resources/application.properties
```

Edit `src/main/resources/application.properties` with your actual values:
- Database credentials
- Keystore password
- Email SMTP settings (optional)

### 4. Generate JWT Keystore
Generate a new RSA keypair for JWT signing:

```bash
keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 -storetype JKS \
  -keystore src/main/resources/keystore.jks -validity 3650
```

When prompted:
- Enter your keystore password (must match `jwt.keystore.password` in properties)
- Fill in certificate details

### 5. Run Database Migrations
The application will automatically run Flyway migrations on startup to create required tables.

### 6. Build and Run
```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

## 🔒 Security Configuration

### Environment-Specific Properties
- **Development**: Use `application-local.properties` for local development
- **Production**: Use environment variables or secure property sources

### Important Security Notes
- Never commit `application.properties` or keystore files to version control
- Use strong, unique passwords for database and keystore
- Rotate keystore keys regularly in production
- Enable HTTPS in production environments

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "StrongP@ssw0rd123",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "StrongP@ssw0rd123"
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Authorization: Bearer <refresh_token>
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

### User Profile Endpoints

#### Get Profile
```http
GET /api/auth/profile
Authorization: Bearer <access_token>
```

#### Update Profile
```http
PUT /api/auth/profile
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "firstName": "Updated Name",
  "lastName": "Updated Last Name",
  "phone": "+1234567890"
}
```

### MFA Endpoints

#### Setup MFA
```http
POST /api/auth/mfa/setup
Authorization: Bearer <access_token>
```

#### Enable MFA
```http
POST /api/auth/mfa/enable
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "code": "123456"
}
```

#### MFA Login
```http
POST /api/auth/login/mfa
Content-Type: application/json

{
  "mfaToken": "<mfa_token>",
  "code": "123456"
}
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing
Use tools like Postman or curl to test the API endpoints.

## 🔍 Monitoring & Logging

- **Audit Logs**: All security events are logged to the `audit_logs` table
- **Application Logs**: Structured logging with configurable levels
- **Health Checks**: Spring Boot Actuator endpoints available at `/actuator/health`

## 🚀 Deployment

### Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Production Considerations
- Use environment variables for sensitive configuration
- Implement proper secrets management (Vault, AWS Secrets Manager)
- Configure load balancing and horizontal scaling
- Set up monitoring and alerting
- Enable HTTPS/TLS termination
- Implement backup and disaster recovery

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the troubleshooting section below

## 🔧 Troubleshooting

### Common Issues

**Database Connection Failed**
- Verify PostgreSQL is running
- Check database credentials in `application.properties`
- Ensure database exists

**JWT Token Errors**
- Verify keystore file exists and password is correct
- Check JWT configuration in properties

**Email Not Sending**
- Configure SMTP settings in `application.properties`
- Check email server connectivity

### Debug Mode
Enable debug logging by setting:
```properties
logging.level.com.example=DEBUG
```

## 📈 Future Enhancements

- [ ] OAuth 2.0 / OpenID Connect integration
- [ ] Social login (Google, GitHub)
- [ ] Account recovery via SMS
- [ ] Advanced audit and compliance reporting
- [ ] API rate limiting per user
- [ ] Geographic access restrictions
- [ ] Password breach checking
- [ ] Session management dashboard