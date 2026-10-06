# UI Consistency Audit Skill

The purpose of this skill is to identify and eliminate visual and UX inconsistencies across the GST Billing application.

Do NOT immediately rewrite the UI.

First inspect the existing frontend.

---

# Audit Areas

Compare all major pages for:

## Layout
- Page width
- Page padding
- Header placement
- Section spacing
- Grid spacing
- Sidebar behavior
- Content alignment

## Typography
- Font family
- Page title size
- Section title size
- Body text
- Labels
- Muted text
- Font weights

## Colors
- Primary color
- Background
- Surface
- Border
- Text
- Muted text
- Success
- Warning
- Error

Identify colors that are visually similar but implemented using different values.

## Components

Compare:

- Buttons
- Inputs
- Selects
- Tables
- Cards
- Badges
- Modals
- Dropdowns
- Tabs
- Toasts
- Alerts
- Pagination
- Date inputs
- Icons (STRICT: Verify ZERO emojis are used anywhere; mandate standard SVG icons)

Identify duplicated implementations.

---

# Audit Process

First inspect the entire frontend structure.

Identify:

1. Shared components.
2. Page-specific components.
3. Duplicate components.
4. Duplicate CSS.
5. Hardcoded colors.
6. Hardcoded spacing.
7. Different border radii.
8. Different button heights.
9. Different input heights.
10. Different typography.
11. Different loading states.
12. Different error handling.
13. Different empty states.

Create a consistency report before making large changes.

---

# Consolidation

When multiple components perform the same function:

Prefer one shared component.

Example:

Bad:

CustomerButton
InvoiceButton
ProductButton

Better:

Button

with appropriate variants.

---

# Visual Rules

## Strict Prohibition of Emojis
- NEVER use unicode emojis (such as 📥, ⚠️, ⚡, 💳, 🚚, 📞, 🔄, ✓, ✕, 🚫, 📋, etc.) anywhere in the UI.
- All actions, alerts, badges, modals, and buttons must use standard SVG icons.
- Emojis render inconsistently across operating systems and degrade the professional appearance of enterprise software.

Do not normalize everything blindly.

Some differences may be intentional.

Before changing something, determine whether the difference represents:

- A real UX requirement
- A responsive requirement
- A different interaction type
- Or accidental inconsistency

Only remove accidental inconsistency.

---

# Priority

Fix in this order:

1. Navigation consistency
2. Page layout
3. Typography
4. Buttons
5. Forms
6. Tables
7. Modals
8. Notifications
9. Loading states
10. Empty states
11. Error states
12. Responsive behavior
13. Minor visual polish

---

# Final Goal

The user should be able to move between:

Dashboard
Customers
Products
Invoices
Business Settings
User Settings

without feeling like they are using different applications.

The application should feel like one coherent SaaS product.
