ifeq ($(OS),Windows_NT)
build-ListRecipesFunction build-GetRecipeByIdFunction build-CreateRecipeFunction:
	mvn clean package -DskipTests
	cmd /c if not exist "$(ARTIFACTS_DIR)\lib" mkdir "$(ARTIFACTS_DIR)\lib"
	cmd /c copy /Y recipes-api\target\recipes-api-rest-1.0.0.jar "$(ARTIFACTS_DIR)\lib"
else
build-ListRecipesFunction build-GetRecipeByIdFunction build-CreateRecipeFunction:
	mvn clean package -DskipTests
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp recipes-api/target/recipes-api-rest-1.0.0.jar $(ARTIFACTS_DIR)/lib/
endif
