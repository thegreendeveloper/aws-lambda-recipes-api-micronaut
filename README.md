# Recipes API — Micronaut on AWS Lambda

A spec-first REST API built with Micronaut, structured as microservices — one Lambda function per endpoint — behind API
Gateway, with DynamoDB as the data store.

## Architecture decisions

**Micronaut over Spring Boot**
Chose Micronaut for its compile-time dependency injection, which avoids the
runtime reflection that makes Spring Boot cold starts on Lambda 5–8 seconds.
Micronaut keeps cold starts under a second on the JVM, which matters when
each endpoint is its own Lambda function.

**One Lambda per endpoint (microservices) over a single monolithic Lambda**
Each endpoint deploys as an independent function. Trades higher cold-start
surface area and more configuration for independent scaling, deployment,
and blast-radius isolation per endpoint.

**Layered module structure (repository / service / api)**
Split into three Maven modules to enforce separation of concerns and make
each layer independently testable. The api module depends on service,
service depends on repository — no upward dependencies.

**Maven over Gradle**
Chose Maven for its widespread use in the Java and AWS Lambda ecosystem
and to deepen familiarity with it. Gradle would have been viable but
offered no advantage for this project's build complexity.

**Spec-first OpenAPI**
The OpenAPI spec is the source of truth — handlers and clients are
generated from it. Gives consistent documentation, generated client code,
and a single place to review API changes.

**DynamoDB as the starting data store**
Picked DynamoDB as the path-of-least-resistance choice for a serverless
API. Native AWS integration means Lambda accesses the table via IAM
permissions defined in the SAM template — no connection string, no
secrets to manage, no external service. The table itself is also defined
in the same SAM template, which keeps configuration in one place.

This project was later migrated to MongoDB Atlas in a follow-up repo
once richer query support was needed — DynamoDB's key-value model made
queries like "recipes containing all of these ingredients" awkward,
requiring either full table scans or a GSI per ingredient.

**S3 event-driven import with DLQ**
Recipe imports are triggered by S3 object uploads, invoking
`ImportRecipesFunction` asynchronously. Lambda retries twice on failure,
then routes the original event to an SQS Dead Letter Queue so failures
are recoverable rather than silently lost. Direct S3 → Lambda invocation
was sufficient for this use case; SQS or EventBridge in front would add
buffering or fan-out flexibility that wasn't needed here.

## Prerequisites

- Java 17
- Maven 3.9+
- Docker (for local testing via SAM)
- AWS SAM CLI

## Local testing

There is no embedded server. Local testing runs the actual Lambda runtime in Docker via SAM CLI. `sam build` compiles
the project and packages the Lambda function in one step.

```
sam build
sam local start-api
```

The API is available at `http://localhost:3000`.

### Start local DynamoDB

A `docker-compose.yml` is included that starts DynamoDB Local and creates the Recipes table:

```
docker compose up
```

Then in a second terminal:

```
sam build
sam local start-api --env-vars env.json
```

Note: `sam local start-api` runs entirely in Docker on your machine — it never touches AWS and incurs no cost.

### Debugging with IntelliJ

Start SAM with the JDWP debug port exposed:

```
sam build
sam local start-api --debug-port 5858 --debug-args "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5858"
```

Then in IntelliJ create a Remote JVM Debug run configuration:

| Setting       | Value                |
|---------------|----------------------|
| Host          | localhost            |
| Port          | 5858                 |
| Debugger mode | Attach to remote JVM |

With `suspend=y` the Lambda container waits for the debugger to attach before processing each request. Send a request (e.g. `curl http://localhost:3000/recipes`), then launch the debug configuration in IntelliJ — execution will stop at your breakpoints.

## Endpoints

| Method | Path            | Description        |
|--------|-----------------|--------------------|
| GET    | `/recipes`      | List all recipes   |
| GET    | `/recipes/{id}` | Get a recipe by ID |
| POST   | `/recipes`      | Create a recipe    |

### Example requests

```
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

### First deploy

```
sam build
sam deploy --guided
```

Follow the prompts — your answers are saved to `samconfig.toml` for future deploys.

After deploy completes, wire the S3 trigger for the import function:

```
make configure-recipes-import-trigger
```

This is a one-time step that tells the S3 bucket to invoke `ImportRecipesFunction` on upload.

### Subsequent deploys

```
sam build
sam deploy
```

## S3 import

`ImportRecipesFunction` is wired to an S3 bucket (`recipes-import-<account-id>-<region>`)
and triggered asynchronously when a JSON file is uploaded. The function reads
the file, parses it, and writes the recipes to DynamoDB.

Upload a recipes file to trigger an import:

```
aws s3 cp recipes.json s3://recipes-import-<account-id>-<region>/
```

The expected file format is a JSON array of recipe objects matching the
`POST /recipes` schema.

## Dead Letter Queue

`ImportRecipesFunction` is invoked asynchronously by S3. If the handler throws on every attempt (after 2 retries),
Lambda forwards the original S3 event JSON to an SQS Dead Letter Queue so the failure is not silently lost.

### Verify the DLQ is wired after deploy

In the AWS console: **Lambda → Functions → recipes-import-<stack-name> → Configuration → Asynchronous invocation**

Check that:

- Retry attempts is 2
- Under Destinations, there is an entry labelled **Async inv** with an SQS ARN ending in
  `recipes-import-dlq-<stack-name>`

The DLQ URL is also printed as a stack output at the end of `sam deploy`.

### Trigger a test failure

Upload a file with invalid JSON to the import bucket:

```
echo "not-valid-json" | aws s3 cp - s3://recipes-import-<account-id>-<region>/bad-import.json
```

Lambda will attempt the invocation 3 times total with exponential back-off (~3–5 minutes), then route the event to the
DLQ.

### Confirm the message landed

```
aws sqs receive-message \
  --queue-url <dlq-url> \
  --attribute-names All
```

The DLQ URL is printed as a stack output after `sam deploy`. The message body is the original S3 event JSON. The
`ErrorCode` and `ErrorMessage` message attributes describe why it failed.

In the AWS console: **SQS → Queues → recipes-import-dlq-<stack> → Send and receive messages → Poll for messages**.

### Replay a failed event

Once you have fixed the underlying problem (e.g. replaced the bad file), re-invoke the function manually using the DLQ
message body:

```
aws lambda invoke \
  --function-name $(aws cloudformation describe-stack-resource \
    --stack-name <stack-name> \
    --logical-resource-id ImportRecipesFunction \
    --query "StackResourceDetail.PhysicalResourceId" --output text) \
  --invocation-type RequestResponse \
  --payload '<message-body-from-dlq>' \
  response.json
```

Then purge the message from the DLQ so it is not processed again:

**SQS console → recipes-import-dlq-<stack> → Purge**.

## Tearing down

Before running `sam delete`, you must manually empty the S3 import bucket — CloudFormation cannot delete a non-empty bucket:

**S3 → recipes-import-<account-id>-<region> → select all files → Delete**

Then:

```
sam delete
```

Note: The DynamoDB table has `DeletionPolicy: Retain` — it survives `sam delete` and keeps its data. If you want a clean
redeploy, manually delete the Recipes table from the DynamoDB console before running `sam deploy` again, otherwise
CloudFormation will fail trying to create a table that already exists.

## Project structure

```
recipes-repository/   DynamoDB entity + repository
recipes-service/      Business logic + domain models
recipes-api/          Lambda handlers + OpenAPI spec + fat JAR
template.yaml         SAM template (3 Lambda functions + API Gateway + DynamoDB)
```