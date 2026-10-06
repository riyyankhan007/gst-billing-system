# API Design Skill

Design REST APIs that are predictable, secure, and maintainable.

## Rules

- Use meaningful resource-oriented URLs.
- Use appropriate HTTP methods.
- Use correct HTTP status codes.
- Validate request bodies.
- Return consistent error responses.
- Do not expose internal exceptions.
- Do not expose database implementation details unnecessarily.
- Use DTOs where appropriate.
- Use pagination for large collections.
- Support filtering/search only where useful.
- Avoid breaking existing API contracts unnecessarily.

## Example

POST /api/invoices
GET /api/invoices
GET /api/invoices/{id}

The GET /api/invoices/{id} endpoint must enforce ownership authorization.

## Errors

Use a consistent structure such as:

{
  "timestamp": "...",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "...",
  "path": "..."
}

Do not return stack traces to clients.

## API changes

Before modifying an endpoint:
1. Search for frontend consumers.
2. Search for tests.
3. Check authentication requirements.
4. Check existing response structure.
5. Preserve compatibility where practical.
