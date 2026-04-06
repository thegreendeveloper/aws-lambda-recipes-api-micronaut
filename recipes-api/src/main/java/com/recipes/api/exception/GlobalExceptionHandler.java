package com.recipes.api.exception;

import com.recipes.api.generated.model.ApiError;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;

import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;

/**
 * Global exception handler for the Recipes API.
 *
 * <p>Micronaut's {@code @Error(global = true)} annotation on methods in a {@code @Controller}
 * class applies the handler to all controllers, equivalent to Spring's
 * {@code @RestControllerAdvice}. Each method is matched to the most specific exception type.
 */
@Controller
public class GlobalExceptionHandler {

    @Error(global = true, exception = RecipeNotFoundException.class)
    public HttpResponse<ApiError> handleNotFound(HttpRequest<?> request, RecipeNotFoundException ex) {
        return HttpResponse.notFound(buildError(404, ex.getMessage()));
    }

    @Error(global = true, exception = BadRequestException.class)
    public HttpResponse<ApiError> handleBadRequest(HttpRequest<?> request, BadRequestException ex) {
        return HttpResponse.<ApiError>status(io.micronaut.http.HttpStatus.BAD_REQUEST)
                .body(buildError(400, ex.getMessage()));
    }

    @Error(global = true, exception = ConstraintViolationException.class)
    public HttpResponse<ApiError> handleValidation(HttpRequest<?> request, ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .findFirst()
                .orElse("Validation failed");
        return HttpResponse.<ApiError>status(io.micronaut.http.HttpStatus.BAD_REQUEST)
                .body(buildError(400, message));
    }

    @Error(global = true)
    public HttpResponse<ApiError> handleGeneral(HttpRequest<?> request, Exception ex) {
        return HttpResponse.serverError(buildError(500, "Internal server error"));
    }

    private ApiError buildError(int status, String message) {
        ApiError error = new ApiError();
        error.setStatus(status);
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        return error;
    }
}
