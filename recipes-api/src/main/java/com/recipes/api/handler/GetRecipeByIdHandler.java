package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.generated.model.ApiError;
import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.Map;

public class GetRecipeByIdHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            String id = input.getPathParameters().get("id");
            RecipeDetail detail = service.getRecipeById(id);
            return buildResponse(200, objectMapper.writeValueAsString(toDetailResponse(detail)));
        } catch (RecipeNotFoundException e) {
            return buildErrorResponse(404, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "Internal server error");
        }
    }

    private RecipeDetailResponse toDetailResponse(RecipeDetail detail) {
        return RecipeDetailResponse.builder()
                .id(detail.getId())
                .name(detail.getName())
                .cuisine(detail.getCuisine())
                .prepTimeMinutes(detail.getPrepTimeMinutes())
                .ingredients(detail.getIngredients())
                .steps(detail.getSteps())
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
