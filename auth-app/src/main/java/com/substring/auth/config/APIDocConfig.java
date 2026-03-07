package com.substring.auth.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Application build by Yash.",
                description = "A secure user authentication system with login, registration, JWT security, and protected APIs built using modern backend frameworks.",
                contact = @Contact(
                        name = "Yash",
                        url ="https://yash-protfolio-one-lac.vercel.app/",
                        email = "support@substringauth.com"
                ),
                version = "1.0",
                summary = "A secure, production-ready authentication and authorization system built with Spring Boot, featuring JWT-based authentication, role-based access control, and comprehensive user management capabilities"
        ),
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)

@SecurityScheme(
        name ="bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",// Authorization:Bearer
        bearerFormat = "JWT"
)

public class APIDocConfig {
}
