package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.exception.BadRequestException;
import com.recipes.api.generated.model.ApiError;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

public class CreateRecipeHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private Validator validator;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            CreateRecipeRequest request = parseRequest(input.getBody());
            String validationError = firstViolationMessage(validator.validate(request));
            if (validationError != null) {
                return buildErrorResponse(400, validationError);
            }
            RecipeDetail detail = service.createRecipe(
                    request.getName(), request.getCuisine(), request.getPrepTimeMinutes(),
                    request.getIngredients(), request.getSteps());
            return buildResponse(201, objectMapper.writeValueAsString(toDetailResponse(detail)));
        } catch (BadRequestException e) {
            return buildErrorResponse(400, e.getMessage());
        } catch (JsonProcessingException e) {
            return buildErrorResponse(400, "Invalid request body");
        } catch (Exception e) {
            return buildErrorResponse(500, "Internal server error");
        }
    }

    private CreateRecipeRequest parseRequest(String body) throws JsonProcessingException {
        return objectMapper.readValue(body, CreateRecipeRequest.class);
    }

    private String firstViolationMessage(Set<ConstraintViolation<CreateRecipeRequest>> violations) {
        if (violations.isEmpty()) {
            return null;
        }
        ConstraintViolation<CreateRecipeRequest> first = violations.iterator().next();
        return first.getPropertyPath() + ": " + first.getMessage();
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
