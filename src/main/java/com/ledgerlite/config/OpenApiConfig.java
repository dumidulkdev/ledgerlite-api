package com.ledgerlite.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @Bean
    public OpenAPI ledgerLiteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LedgerLite API")
                        .description("Digital wallet and money transfer API with idempotent operations and double-entry ledger.")
                        .version("1.0.0"));
    }

    @Bean
    public OperationCustomizer idempotencyKeyDocumentation() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(com.ledgerlite.controller.annotation.RequireIdempotencyKey.class)) {
                Parameter parameter = new Parameter()
                        .in("header")
                        .name(IDEMPOTENCY_KEY_HEADER)
                        .required(true)
                        .description("Unique key for safely retrying money-moving requests without double-charging.")
                        .schema(new StringSchema().example("deposit-550e8400-e29b-41d4-a716-446655440000"));
                operation.addParametersItem(parameter);
            }
            return operation;
        };
    }

    @Bean
    public Components openApiComponents() {
        return new Components();
    }
}