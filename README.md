# Recipes API — Micronaut on AWS Lambda

A spec-first REST API built with Micronaut, deployed as an AWS Lambda function behind API Gateway.

## Prerequisites

- Java 17
- Maven 3.9+
- Docker (for local testing via SAM)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)

## Build

```bash
mvn clean package -DskipTests
```

## Local testing

There is no embedded server. Local testing runs the actual Lambda runtime in Docker via SAM CLI.

```bash
sam build
sam local start-api
```

The API is available at `http://localhost:3000`.

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
