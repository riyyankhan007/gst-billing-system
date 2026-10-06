# Financial Integrity Skill

This application handles financial information.

## Money

Never use:
- float
- double

for monetary calculations.

Use:
- Java BigDecimal
- PostgreSQL NUMERIC/DECIMAL

Always define explicit rounding behavior.

## Calculations

Never trust client-provided:
- subtotal
- tax
- discount total
- grand total
- CGST
- SGST
- IGST

Recalculate them on the backend.

## Transactions

Operations that create or modify financial records should use appropriate database transactions.

A failed invoice operation must not leave partially-created financial data.

## Precision

Use a consistent scale and rounding policy.

Do not randomly round individual values unless the business rule requires it.

## Auditability

Important financial changes should be traceable.

Avoid destructive updates to finalized financial records.

## Concurrency

Financial operations must remain correct under concurrent requests.

Consider database constraints, locking, transactions, and unique indexes where appropriate.
