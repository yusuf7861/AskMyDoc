# AskMyDoc

Modern UI + backend for asking questions against uploaded documents with Gemini.

## Quick Start

1. Set environment variables (Windows CMD example):
```
set GEMINI_API_KEY=YOUR_KEY_HERE
set DB_URL=jdbc:postgresql://localhost:5432/askmydoc
set DB_USER=admin
set DB_PASSWORD=admin123
```
2. Start Postgres locally with a database named `askmydoc` (adjust `application.yaml` if different). Ensure user/password matches.
3. Run the app:
```
mvnw spring-boot:run
```
4. Open the UI:
```
http://localhost:8082/
```
(Served by `src/main/resources/static/index.html`.)

## UI Features
- Upload PDF/TXT/DOC/DOCX/RTF (max 10MB) and auto-ingest into chunks.
- List documents, select subset for scoped question answering.
- Configure Top-K retrieval and optional translation of source snippets.
- System Status panel: repository stats, Gemini connectivity test, embedding test.
- Swagger/OpenAPI UI at: `http://localhost:8082/swagger-ui/index.html`

## REST Endpoints
Base path: `/api/v1`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/documents` | POST (multipart) | Upload a document. Form field name: `file` |
| `/documents` | GET | List documents (id, file_name, page_count, size_bytes) |
| `/documents/{id}/file` | GET | Stream PDF inline |
| `/chat/ask` | POST (JSON) | Ask a question using RAG retrieval + generation |
| `/debug/stats` | GET | Basic counts (documents, chunks) |
| `/test/gemini` | GET | Test text generation model connectivity |
| `/test/embed` | GET | Test embedding model connectivity |

### /chat/ask Request JSON
```
{
  "question": "string (required)",
  "document_ids": [1,2],       // optional subset
  "translate_sources": true,   // optional
  "top_k": 5                   // optional (default 5)
}
```
### Response JSON
```
{
  "answer": "string",
  "citations": [
    {
      "document_id": 1,
      "file_name": "sample.pdf",
      "page_number": 3,
      "snippet": "text snippet...",
      "translated_snippet": "(if requested)"
    }
  ]
}
```

## Frontend Notes
- `API_BASE` switched to relative path (`/api/v1`) to avoid hard-coded host/port.
- Styles in `styles.css` provide dark, responsive layout.
- JS gracefully handles errors and updates status areas.

## Environment / Configuration
- Adjust DB config in `application.yaml` if needed.
- GEMINI_API_KEY must be valid for Gemini models `models/gemini-2.5-flash` and `models/text-embedding-004`.
- The application reads `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `GEMINI_API_KEY` from the environment, so mirror those locally and inside CI (GitHub secrets recommended).
- Hibernate `ddl-auto: update` will manage schema evolution; for production consider `validate` + migrations.

## CI
- GitHub Actions workflow `.github/workflows/ci.yml` runs on push/PR to `main`, using Temurin JDK 17.
- Secrets required: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `GEMINI_API_KEY`.
- The workflow runs `mvn verify`, packages the jar, and uploads artifacts (jar + surefire reports) for inspection.

## Future Improvements (Optional)
- Pagination for documents list.
- Delete document endpoint + UI button.
- Streaming responses for long answers.
- Auth (API keys / OAuth) and rate limiting.
- Persist user queries & answers for history.

## Troubleshooting
- 404 on Swagger: ensure `springdoc-openapi-starter-webmvc-ui` dependency is downloaded; rebuild with `mvnw clean package`.
- Gemini errors: verify internet access and correct API key; inspect logs at DEBUG level.
- Slow startup: first time Hibernate schema + dependency downloads.

## Testing
Run full tests:
```
mvnw test
```
Current test only verifies context loads; add more unit tests for retrieval & generation services as they evolve.
