# Code Review Skill

When reviewing or modifying code, do not only check whether it compiles.

Look for:

## Correctness
- Business logic bugs
- Incorrect GST calculations
- Incorrect totals
- Incorrect invoice numbering
- Race conditions
- Transaction problems

## Security
- IDOR/BOLA
- Missing authorization
- Authentication bypass
- Sensitive information exposure
- Unsafe input handling
- Secret leakage

## Database
- N+1 queries
- Missing indexes
- Incorrect relationships
- Missing constraints
- Unnecessary queries

## Spring Boot
- Incorrect transaction boundaries
- Incorrect bean scopes
- Poor exception handling
- Fat controllers
- Fat services
- Unnecessary database calls

## Frontend
- Trusting client calculations
- Broken authentication handling
- Missing loading/error states
- Exposed secrets

## Maintainability
- Duplicated logic
- Overly complex methods
- Poor naming
- Dead code
- Unnecessary abstractions

## Review principle

Prioritize:
1. Security vulnerabilities
2. Financial correctness
3. Data integrity
4. Production failures
5. Performance
6. Maintainability
7. Style

Do not waste review effort on minor formatting issues while a security or financial bug exists.
