# Recipes API — Micronaut Lambda

## Project structure

Multi-module Maven project:

| Module | Artifact | Purpose |
|---|---|---|
| `recipes-repository` | `recipes-repository` | JPA entity + repository |
| `recipes-service` | `recipes-service` | Business logic, domain models, exceptions |
| `recipes-api` | `recipes-api-rest` | REST controllers, OpenAPI spec, Lambda fat JAR |

## Build

```bash
mvn clean package -DskipTests
```

The shade plugin in `recipes-api/pom.xml` produces the deployable fat JAR at
`recipes-api/target/recipes-api-rest-1.0.0.jar`.

## Testing

There is no embedded HTTP server and no `main()` class. Local testing is done
exclusively via SAM CLI, which emulates the Lambda runtime in Docker.

### Prerequisites

- Docker running
- [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) installed
- Fat JAR already built (`mvn clean package -DskipTests`)

### Start local API

```bash
sam build
sam local start-api
```

API is available at `http://localhost:3000`.

### Example requests

```bash
# List recipes
curl http://localhost:3000/recipes

# Get recipe by ID
curl http://localhost:3000/recipes/{id}

# Create recipe
curl -X POST http://localhost:3000/recipes \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta","cuisine":"Italian","prepTimeMinutes":20,"ingredients":["pasta","sauce"],"steps":["boil","mix"]}'
```

### Invoke a single function

```bash
sam local invoke RecipesFunction --event events/list-recipes.json
```

## Lambda handler

`io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction`

Micronaut's built-in handler — no custom handler class required. It initialises
the Micronaut application context and routes each API Gateway proxy event to the
appropriate controller.

## Design decisions

- **No main class** — this is a Lambda-only project. There is no embedded server
  and no `main()` entry point. Local development uses `sam local start-api`.
- **OpenAPI generator produces models only** — `generateApis=false` in the plugin
  config. The routing interface `RecipesApi` (`com.recipes.api.api`) is
  hand-written because `java-micronaut-server` does not reliably generate a
  standalone interface with Micronaut annotations.
- **No Netty dependency** — excluded from the fat JAR to keep it lean. Lambda
  does not need an embedded HTTP server.
