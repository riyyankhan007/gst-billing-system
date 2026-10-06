# Database Skill

The application uses PostgreSQL.

## Rules

- Design tables with clear ownership and relationships.
- Use primary keys and foreign keys appropriately.
- Add database constraints for important invariants.
- Add indexes based on actual query patterns.
- Avoid unnecessary indexes.
- Never store monetary values using floating-point types.
- Use NUMERIC/DECIMAL for monetary values.
- Use BigDecimal in Java.
- Use timestamps consistently.
- Avoid storing duplicated derived data unless there is a clear reason.
- Avoid N+1 queries.
- Use pagination for large result sets.
- Use transactions for multi-step financial operations.
- Never rely exclusively on application validation for critical data integrity.

## Multi-tenant isolation

Every business-owned resource must be scoped to the authenticated business.

Never allow:

Business A -> Customer B
Business A -> Invoice B
Business A -> Product B

through manipulated IDs.

Ownership checks must happen server-side.

## Schema changes

Before changing the database:
1. Check existing schema.
2. Check existing data.
3. Consider backward compatibility.
4. Avoid destructive migrations unless explicitly required.
5. Consider production migration strategy.
