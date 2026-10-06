# Security Skill

Treat all client input as untrusted.

## Authentication

- Passwords must never be stored in plaintext.
- Use strong password hashing.
- Never log passwords.
- Never log JWTs.
- Configure token expiration.
- Validate authentication on every protected endpoint.
- Never trust user identity supplied by the frontend.

## Authorization

Prevent IDOR/BOLA vulnerabilities.

For every resource:
- Identify the authenticated user.
- Identify their business.
- Verify the requested resource belongs to that business.
- Reject unauthorized access.

Example:

GET /api/invoices/123

must verify that invoice 123 belongs to the authenticated business.

Do not rely on the frontend hiding resources.

## API security

- Validate all request input.
- Limit request sizes where appropriate.
- Configure CORS intentionally.
- Do not use allow-all CORS in production without a reason.
- Never expose stack traces to clients.
- Never expose passwords, password hashes, tokens, or secrets.
- Avoid sensitive information in logs.
- Use rate limiting for authentication and abuse-prone endpoints.

## Security changes

Before changing authentication/authorization, inspect the complete existing security flow.
Do not implement isolated security changes that bypass existing filters or rules.
