# Recipes API — Micronaut Lambda

## Project structure

Multi-module Maven project:

| Module | Artifact | Purpose |
|---|---|---|
| `recipes-repository` | `recipes-repository` | JPA entity + repository |
| `recipes-service` | `recipes-service` | Business logic, domain models, exceptions |
| `recipes-api` | `recipes-api-rest` | REST controllers, OpenAPI spec, Lambda fat JAR |

## Testing

There is no embedded HTTP server and no `main()` class. Local testing is done
exclusively via SAM CLI, which emulates the Lambda runtime in Docker.

`sam build` runs the full Maven build internally (via the Makefile in `recipes-api/`)
— no separate `mvn package` step is needed.

### Prerequisites

- Docker running
- [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) installed

### Start local API

```bash
sam build
sam local start-api
```

API is available at `http://localhost:3000`.

> `sam local start-api` runs entirely in Docker — no AWS calls, no cost.

### Debug with IntelliJ

```bash
sam local start-api --debug-port 5858 --debug-args "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5858"
```

Create a **Remote JVM Debug** run configuration in IntelliJ (`Run → Edit Configurations → + → Remote JVM Debug`) with host `localhost` and port `5858`. With `suspend=y` the container pauses until the debugger attaches — send a request first, then attach.

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

## Coding conventions

### Extract complex chains into named methods

Any builder chain or stream pipeline longer than 2 steps must be extracted into
a private method with a descriptive name.

Extract the builder/stream call into a private method named after what it does, and call that method from the public one.

## Known limitations

### No integration tests for `RecipesRepository`

Integration tests using Testcontainers (to spin up `amazon/dynamodb-local` in Docker) were
attempted but could not be made to work on Docker Desktop 4.67.0 on Windows. Both the TCP proxy
(`localhost:2375`) and the named pipe (`//./pipe/docker_engine`) return HTTP 400 for the Docker
`/info` call that Testcontainers uses to validate the connection, even though the Docker daemon
itself is healthy and the Docker CLI works normally.

The unit tests in `RecipesRepositoryUnitTest` cover the mapping logic and SDK call verification
via Mockito mocks. If integration tests are needed in future, consider:
- Installing [Testcontainers Desktop](https://testcontainers.com/desktop/) which provides a
  bridge service that resolves this Docker Desktop compatibility issue
- Running tests inside WSL2 where the Docker Unix socket is directly accessible

## Design decisions

- **No main class** — this is a Lambda-only project. There is no embedded server
  and no `main()` entry point. Local development uses `sam local start-api`.
- **OpenAPI generator produces models only** — `generateApis=false` in the plugin
  config. The routing interface `RecipesApi` (`com.recipes.api.api`) is
  hand-written because `java-micronaut-server` does not reliably generate a
  standalone interface with Micronaut annotations.
- **No Netty dependency** — excluded from the fat JAR to keep it lean. Lambda
  does not need an embedded HTTP server.
