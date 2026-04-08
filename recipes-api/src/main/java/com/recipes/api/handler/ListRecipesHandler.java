package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.generated.model.ApiError;
import com.recipes.api.generated.model.RecipeSummaryResponse;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class ListRecipesHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            return buildResponse(200, objectMapper.writeValueAsString(buildSummaryResponses()));
        } catch (Exception e) {
            return buildErrorResponse(500, "Internal server error");
        }
    }

    private List<RecipeSummaryResponse> buildSummaryResponses() {
        return service.listRecipes().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private RecipeSummaryResponse toSummaryResponse(RecipeSummary summary) {
        return RecipeSummaryResponse.builder()
                .id(summary.getId())
                .name(summary.getName())
                .cuisine(summary.getCuisine())
                .prepTimeMinutes(summary.getPrepTimeMinutes())
                .build();
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(body);
        response.setHeaders(Map.of("Content-Type", "application/json"));
        return response;
    }

    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        ApiError error = new ApiError();
        error.setStatus(statusCode);
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        try {
            return buildResponse(statusCode, objectMapper.writeValueAsString(error));
        } catch (JsonProcessingException e) {
            return buildResponse(500, "{\"status\":500,\"message\":\"Internal server error\"}");
        }
    }
}
