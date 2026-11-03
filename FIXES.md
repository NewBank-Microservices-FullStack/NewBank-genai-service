# FIXES — todo → owner file (this repo)

| # | Todo | Fixed in |
|---|------|----------|
| P0-1 | Unauthenticated writes | `SecurityConfig.java` (JWT resource server), `BankTools.java` (`@PreAuthorize`), `AIDataProvider.java` (JWT propagation), `RateLimitConfig.java` + `NewBankChatClient.java` (per-user limits) |
| P0-2 | Shared chat memory | `AIBeanConfiguration.java` + `NewBankChatClient.java` (`CONVERSATION_ID` from conversation/session/user). Tested in `ChatScopingGuardrailsTest` |
| P0-3 | Injection → mutations | `BankTools.java` (propose → confirm, roles, `@Valid`), `ToolConfirmationService.java`, `Guardrails.java` (moderation, do-not-know prompt); constraints added to `dto/CustomerRequest.java`, `dto/AccountRequest.java` |
| P0-4 | Committed embeddings | `src/main/resources/vectorstore.json` deleted; runtime build + validation in `VectorStoreController.java`; blocked by `.gitignore` |
| P1-5 | Dead PetClinic package | `org/springframework/samples/petclinic/genai/*` deleted; `pom.xml` parent is now `spring-boot-starter-parent` |
| P1-6 | Provider mismatch | `pom.xml` keeps only `spring-ai-starter-model-openai`; `application.yml` has no `azure` block and no `creds.yaml` import; secrets from env (`.env.example`) |
| P1-7 | PetClinic service names | `AIDataProvider.java` + `VectorStoreController.java` use configurable `newbank.genai.*-url` + load-balanced `RestClient` |
| P1-8 | `getInstances().get(0)` + no timeouts | `ResilientClientConfig.java` (`@LoadBalanced` + timeouts); `AIDataProvider` uses circuit breakers + fallbacks; `DiscoveryClient` removed |
| P1-9 | Dependency bloat | `pom.xml` drops gateway-webflux, data-jpa, hsqldb, mysql, chaos-monkey, jolokia, caffeine, zipkin |
| P1-10 | `advisorRequest==null` → `"null"` | `AIDataProvider.getAdvisors/listAdvisorsPage` returns paginated list; tested in `AdvisorNullWireMockTest` |
| P2-11 | String contract | `dto/ChatRequest.java` / `dto/ChatResponse.java`, `NewBankChatClient` (JSON + SSE `/chatclient/stream` + `@Valid`), `GlobalExceptionHandler.java` (RFC 7807) |
| P2-12 | Docs/tooling | `springdoc-openapi` + `OpenApiConfig.java` (bearer-jwt); `.env.example` |
| P2-13 | Zero tests | `AdvisorNullWireMockTest` (WireMock), `BankToolsConfirmationTest`, `ChatScopingGuardrailsTest`, `smoke.sh` + `docker-compose.yml` |
| P2-14 | Observability | `ModelMetrics.java` (latency/tokens/tool outcome), OTel tracing in `application.yml` |
| P2-15 | PII in logs | `MaskingAdvisor.java` replaces `SimpleLoggerAdvisor`; advisor/AI loggers pinned to WARN; tools log ids/flags only |
| P2-16 | Hygiene | `docker/Dockerfile` (pom `dockerfile.dir` fixed), `docker-compose.yml`, `.github/workflows/ci.yml`, `LICENSE`, `CONTRIBUTING.md`, `.gitignore`, `FIXES.md` |
| P2-17 | In-memory scale-out | `VectorStoreController.java` — pgvector-ready, async refresh, no `block()` on startup, validation before swap |
| P2-18 | Guardrails | `Guardrails.java` — length caps, moderation, do-not-know policy, cost estimate |
