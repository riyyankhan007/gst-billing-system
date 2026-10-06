# Testing Skill

Every meaningful feature should have automated tests.

## Required testing areas

### Unit tests
Test:
- GST calculations
- Invoice calculations
- Validation
- Business rules
- Utility logic

### Integration tests
Test:
- PostgreSQL persistence
- Repository behavior
- Transactions
- Invoice creation

### API tests
Test:
- Authentication
- Authorization
- Validation
- Success responses
- Error responses

### Security tests

Explicitly test cross-business access.

Example:

Business A creates Invoice 1.
Business B attempts GET /api/invoices/1.

Expected:
403 or 404 according to the application's security design.

### Concurrency tests

Test:
- Concurrent invoice creation
- Invoice numbering
- Rate limiting
- Simultaneous updates where relevant

## Regression rule

When fixing a bug:
1. Reproduce it with a test.
2. Fix the implementation.
3. Keep the regression test permanently.

Never remove a failing test merely to make the build pass.

Run the relevant test suite after modifications.
