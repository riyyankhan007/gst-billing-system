# GST Compliance Skill

This application generates GST invoices for businesses in India.

GST-related functionality must be implemented carefully and should not invent legal requirements.

## Tax calculation

For taxable supplies:

### Intra-state
Use:
- CGST
- SGST/UTGST where applicable

### Inter-state
Use:
- IGST

Determine tax treatment using the relevant supplier/customer state information and applicable business rules.

## GSTIN

- Validate GSTIN format before accepting it.
- Do not claim that format validation proves that a GSTIN is active or belongs to a particular taxpayer.
- External GSTIN verification should be implemented through an authorized/reliable integration when required.
- Never invent GST verification API responses.

## Invoice data

Support appropriate invoice information including:
- Supplier details
- Customer details
- GSTIN where applicable
- Invoice number
- Invoice date
- Place of supply
- Product/service description
- HSN/SAC where applicable
- Quantity
- Unit price
- Discount where applicable
- Taxable value
- Applicable GST rate
- CGST/SGST/IGST
- Total amount

## Important

Do not represent the application as legally GST-compliant merely because these fields exist.

When implementing compliance-related functionality:
1. Identify the relevant official requirement.
2. Keep legal/compliance assumptions explicit.
3. Avoid hardcoding uncertain rules.
4. Make rules configurable where appropriate.
5. Preserve invoice history.

Never silently alter a finalized invoice.

When external GST/IRP/e-invoicing integration is required, isolate it behind an interface so it can be replaced with a real provider later.
