package com.recipes.api.api;

import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.generated.model.RecipeSummaryResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Routing contract for the Recipes API, derived from openapi.yaml.
 *
 * <p>This interface carries the Micronaut HTTP routing annotations and serves the same
 * purpose as the auto-generated Spring {@code RecipesApi} interface in the sibling project:
 * it is the spec-first contract that {@code RecipesController} implements.
 *
 * <p>Models ({@code CreateRecipeRequest}, {@code RecipeDetailResponse},
 * {@code RecipeSummaryResponse}) are generated from {@code openapi.yaml} at build time.
 * Only the routing interface is hand-written, because the OpenAPI {@code java} generator
 * (used here for model-only generation) does not produce Micronaut-annotated API stubs.
 */
public interface RecipesApi {

    @Get("/recipes")
    @Produces(MediaType.APPLICATION_JSON)
    HttpResponse<List<RecipeSummaryResponse>> listRecipes();

    @Get("/recipes/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    HttpResponse<RecipeDetailResponse> getRecipeById(@PathVariable("id") String id);

    @Post("/recipes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    HttpResponse<RecipeDetailResponse> createRecipe(@Body @Valid CreateRecipeRequest createRecipeRequest);
}
