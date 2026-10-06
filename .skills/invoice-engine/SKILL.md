# Invoice Engine Skill

The invoice engine is a financial domain component.

## Core principle

The backend is the source of truth.

Never trust totals, tax amounts, or calculated values supplied by the frontend.

The backend must recalculate:

- Line taxable value
- Discounts
- Subtotal
- GST
- CGST
- SGST
- IGST
- Round-off
- Grand total

## Invoice creation

Validate:
1. Business exists and belongs to authenticated user.
2. Customer exists and belongs to the business.
3. Products belong to the business.
4. Invoice items are valid.
5. Quantities are valid.
6. Prices are valid.
7. GST rates are valid.
8. Tax treatment is valid.

Calculate totals server-side.

## Invoice numbering

Invoice numbering must:
- Avoid accidental duplicates.
- Be scoped appropriately to the business/series.
- Handle concurrent invoice creation safely.
- Never depend on frontend-generated numbers.

## Finalized invoices

Once an invoice is finalized:
- Do not silently modify financial values.
- Do not physically delete it unless the business/legal design explicitly permits it.
- Preserve audit history.

If correction is required, use an appropriate correction workflow such as cancellation, credit note, debit note, or replacement according to the application's supported business rules.

## Concurrency

Invoice creation must remain correct when multiple requests happen simultaneously.

Do not use:

SELECT MAX(invoice_number) + 1

as the sole mechanism for generating invoice numbers.
