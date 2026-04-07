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
 * Routing contract for the Recipes API, derived from {@code openapi.yaml}.
 *
 * <p>This interface carries the Micronaut HTTP routing annotations ({@code @Get}, {@code @Post},
 * {@code @Produces}, {@code @Consumes}, etc.) and is the spec-first contract that
 * {@link com.recipes.api.controller.RecipesController} implements.
 *
 * <p><b>Why this interface is hand-written:</b><br>
 * The {@code java-micronaut-server} OpenAPI Generator (v7.5.0) does not support generating a
 * standalone routing interface. It generates a single concrete {@code @Controller} class with
 * stub implementations ({@code throw NOT_IMPLEMENTED}), with no option to produce an interface
 * only. The {@code generateImplementationFiles=false} config option silently suppresses all API
 * file generation rather than producing just the interface, making automated generation of this
 * contract impossible with this generator version.
 *
 * <p>As a result, models ({@code CreateRecipeRequest}, {@code RecipeDetailResponse},
 * {@code RecipeSummaryResponse}) are generated from the spec at build time, but this routing
 * interface must be kept in sync with {@code openapi.yaml} manually when endpoints change.
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
