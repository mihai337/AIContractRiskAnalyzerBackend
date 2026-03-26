# AI Contract Risk Analyzer Backend

Production-oriented Spring Boot backend implementing the mobile API contract for:
- contract upload and retrieval
- deterministic contract analysis pipeline
- clause detection and classification
- missing clause detection
- risk scoring
- AI suggestions (rule-based placeholder)
- custom rule engine (keyword + required-clause checks)

> ML model inference is intentionally **not implemented** yet. A dedicated extension point exists via `MlInferencePort`.

## Tech Stack

- Java 21
- Spring Boot 4 (Web MVC, Validation, Security, Data JPA)
- H2 (default local), PostgreSQL (runtime-ready)
- OpenAPI via springdoc

## API

Base path: `/v1`

### Contracts
- `POST /v1/contracts/extract-text`
- `POST /v1/contracts/upload`
- `GET /v1/contracts`
- `GET /v1/contracts/{contractId}`
- `POST /v1/contracts/{contractId}` (upsert for client cache sync)
- `POST /v1/contracts/{contractId}/analyze`

### Rules
- `GET /v1/rules`
- `POST /v1/rules/{ruleId}`
- `POST /v1/rules/{ruleId}/enabled`

Swagger UI: `/swagger-ui.html`

## Architecture

`src/main/java/licenta/mihai/aicontractriskanalyzerbackend`

- `api` - controllers, DTOs, error handling, mappers
- `application` - orchestration services and analysis engine components
- `domain` - enums and core analysis result model
- `infrastructure` - persistence, security, seed data, ML adapter placeholder
- `shared` - reusable exceptions

## Run

```bash
cd /Users/mihai/Desktop/labs/AIContractRiskAnalyzerBackend
./gradlew bootRun
```

## Test

```bash
cd /Users/mihai/Desktop/labs/AIContractRiskAnalyzerBackend
./gradlew test
```

## Notes

- Text extraction currently decodes `base64Content` as UTF-8 placeholder text.
- Rule engine is deterministic and explainable; each alert includes a reason.
- Default startup seeds 3 example custom rules if no rules exist.
- Security is currently permissive for `/v1/**` to match mobile integration during development; replace with JWT/Firebase verification before production rollout.

