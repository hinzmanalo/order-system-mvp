package com.orderhub.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for OrderHub API documentation.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "OrderHub API", version = "1.0", description = "OrderHub Monolith Ordering System REST API"), tags = {
        @Tag(name = "Auth", description = "Authentication and authorization endpoints"),
        @Tag(name = "Products", description = "Product catalog management"),
        @Tag(name = "Inventory", description = "Inventory operations"),
        @Tag(name = "Orders", description = "Order management"),
        @Tag(name = "Payments", description = "Payment processing"),
        @Tag(name = "Admin", description = "Administrative operations")
})
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
