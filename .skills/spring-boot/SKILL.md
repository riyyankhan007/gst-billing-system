# Spring Boot Skill

Build Spring Boot code using production-quality conventions.

## Rules

- Use constructor injection.
- Use appropriate Spring annotations.
- Keep controllers thin.
- Put business logic in services.
- Use @Transactional deliberately.
- Do not catch Exception unnecessarily.
- Use meaningful custom exceptions for business failures.
- Use centralized exception handling with @RestControllerAdvice.
- Validate incoming request DTOs using Bean Validation.
- Never trust frontend validation alone.
- Use appropriate HTTP status codes.
- Avoid unnecessary database calls.
- Avoid N+1 queries.
- Use pagination for potentially large collections.
- Keep configuration in application configuration/environment variables.
- Never hardcode secrets, passwords, tokens, or production credentials.
- Use profiles/configuration appropriate for development and production.

## Database operations

- Do not perform unnecessary save() calls.
- Understand transaction boundaries.
- Fetch only the data required.
- Use explicit queries when derived queries become inefficient or unclear.

## Code quality

Prefer readable, boring, maintainable Spring Boot code over clever code.
