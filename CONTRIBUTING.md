# Contributing — newbank-genai-service

- Java 17, `./mvnw -B verify` must pass before pushing.
- Never commit secrets, `.env`, or `vectorstore.json` (blocked by `.gitignore`).
- New mutations need propose → confirm, a role, and a test.
- No `LOG.info` of requests/records (PII). Metadata only.
- Contract changes need an OpenAPI annotation and a `FIXES.md` row.
