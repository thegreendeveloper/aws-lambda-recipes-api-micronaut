# Recipes API — Micronaut on AWS Lambda

A spec-first REST API built with Micronaut, deployed as an AWS Lambda function behind API Gateway.

## Prerequisites

- Java 17
- Maven 3.9+
- Docker (for local testing via SAM)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)

## Local testing

There is no embedded server. Local testing runs the actual Lambda runtime in Docker via SAM CLI.
`sam build` compiles the project and packages the Lambda function in one step.

```bash
sam build
sam local start-api
```

The API is available at `http://localhost:3000`.

> **Note:** `sam local start-api` runs entirely in Docker on your machine — it never touches AWS and incurs no cost.

### Debugging with IntelliJ

Start SAM with the JDWP debug port exposed:

```bash
sam build
sam local start-api --debug-port 5858 --debug-args "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5858"
```

Then in IntelliJ create a **Remote JVM Debug** run configuration:

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5858` |
| Debugger mode | Attach to remote JVM |

With `suspend=y` the Lambda container waits for the debugger to attach before processing each request. Send a request (e.g. `curl http://localhost:3000/recipes`), then launch the debug configuration in IntelliJ — execution will stop at your breakpoints.

### Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/recipes` | List all recipes |
| `GET` | `/recipes/{id}` | Get a recipe by ID |
| `POST` | `/recipes` | Create a recipe |

### Example requests

```bash
# List all recipes
curl http://localhost:3000/recipes

# Get by ID
curl http://localhost:3000/recipes/1

# Create
curl -X POST http://localhost:3000/recipes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pasta Carbonara",
    "cuisine": "Italian",
    "prepTimeMinutes": 20,
    "ingredients": ["pasta", "eggs", "pancetta", "parmesan"],
    "steps": ["boil pasta", "fry pancetta", "mix eggs and cheese", "combine"]
  }'
```

## Deploy

```bash
sam deploy --guided
```

## Project structure

```
recipes-repository/   Entity + repository (JPA)
recipes-service/      Business logic + domain models
recipes-api/          Controllers + OpenAPI spec + Lambda fat JAR
template.yaml         SAM template (Lambda + API Gateway)
```
