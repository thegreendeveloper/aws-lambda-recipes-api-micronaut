package com.recipes.api.controller;

import com.recipes.api.api.RecipesApi;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.generated.model.RecipeSummaryResponse;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.service.RecipesService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;

import java.util.List;

@Controller
public class RecipesController implements RecipesApi {

    private final RecipesService service;

    public RecipesController(RecipesService service) {
        this.service = service;
    }

    @Override
    public HttpResponse<List<RecipeSummaryResponse>> listRecipes() {
        List<RecipeSummaryResponse> responses = service.listRecipes().stream()
                .map(this::toSummaryResponse)
                .toList();
        return HttpResponse.ok(responses);
    }

    @Override
    public HttpResponse<RecipeDetailResponse> getRecipeById(String id) {
        return HttpResponse.ok(toDetailResponse(service.getRecipeById(id)));
    }

    @Override
    public HttpResponse<RecipeDetailResponse> createRecipe(CreateRecipeRequest createRecipeRequest) {
        RecipeDetail detail = service.createRecipe(
                createRecipeRequest.getName(),
                createRecipeRequest.getCuisine(),
                createRecipeRequest.getPrepTimeMinutes(),
                createRecipeRequest.getIngredients(),
                createRecipeRequest.getSteps()
        );
        return HttpResponse.created(toDetailResponse(detail));
    }

    private RecipeSummaryResponse toSummaryResponse(RecipeSummary summary) {
        return RecipeSummaryResponse.builder()
                .id(summary.getId())
                .name(summary.getName())
                .cuisine(summary.getCuisine())
                .prepTimeMinutes(summary.getPrepTimeMinutes())
                .build();
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
}
