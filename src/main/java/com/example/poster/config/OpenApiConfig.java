package com.example.poster.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(info = @Info(title = "Poster API", version = "1.0", description = "API for sending POST requests with basic authentication"))
@Configuration
public class OpenApiConfig {
}