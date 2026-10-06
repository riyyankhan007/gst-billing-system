# Production Readiness Skill

Before considering a feature production-ready, check the following.

## Security
- No secrets committed.
- No passwords in source code.
- No JWTs logged.
- Authorization enforced.
- CORS configured correctly.
- Sensitive endpoints protected.
- Rate limiting considered.

## Database
- Appropriate indexes.
- Appropriate constraints.
- Transactions where required.
- No obvious N+1 queries.
- Migration strategy considered.

## API
- Validation implemented.
- Consistent error responses.
- Correct HTTP status codes.
- No stack traces exposed.

## Reliability
- Exceptions handled appropriately.
- External services have failure handling.
- Timeouts are configured where applicable.
- Database connection pool is configured.
- Health checks exist.

## Configuration
- Production configuration uses environment variables/secrets.
- Development credentials are not reused in production.

## Logging
- Logs are useful and structured.
- Sensitive information is not logged.
- Important business/security events can be investigated.

## Deployment
- Application starts correctly in the production environment.
- Port binding is configurable.
- Database connectivity is verified.
- Environment variables are documented.

Do not declare something production-ready without checking these areas.
