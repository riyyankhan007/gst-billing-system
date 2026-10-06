# Frontend UI/UX Skill

You are working on a production-grade GST Billing SaaS.

The frontend must feel like ONE cohesive product.

The most important rule:

DO NOT design each page independently.

Every page, component, modal, form, table, button, card, navigation element, notification, and empty state must follow the existing application design system.

Before creating or modifying UI, inspect existing components and pages and reuse established patterns.

---

# 1. Design System First

Before adding a new UI element:

1. Search the codebase for an existing equivalent.
2. Reuse the existing component if possible.
3. If no component exists, create a reusable component.
4. Do not create one-off styles for individual pages unless genuinely necessary.

Prefer:

Button
Input
Select
DatePicker
Modal
Dialog
Card
Table
Badge
Dropdown
Toast
Alert
Tabs
Breadcrumb
PageHeader
EmptyState
LoadingState
ErrorState
ConfirmDialog

over repeatedly implementing these elements inside individual pages.

---

# 2. Visual Consistency

Maintain consistency in:

- Colors
- Typography
- Font sizes
- Font weights
- Border radius
- Borders
- Shadows
- Spacing
- Button sizes
- Input heights
- Icon sizes
- Table styling
- Modal styling
- Navigation
- Page headers
- Cards
- Empty states
- Error states
- Loading states

If an existing page uses a particular visual pattern, new pages should follow it.

Do not introduce a new visual style simply because it looks attractive in isolation.

---

# 3. Design Tokens

Use centralized design tokens wherever possible.

Examples:

--color-primary
--color-background
--color-surface
--color-border
--color-text
--color-muted
--color-success
--color-warning
--color-danger

--radius-sm
--radius-md
--radius-lg

--spacing-xs
--spacing-sm
--spacing-md
--spacing-lg
--spacing-xl

--font-xs
--font-sm
--font-md
--font-lg
--font-xl

Do not scatter arbitrary values throughout the application.

Avoid situations such as:

margin: 13px
padding: 17px
border-radius: 11px

unless there is a strong design reason.

Prefer the existing spacing system.

---

# 4. Typography

Use a consistent typography hierarchy.

The application should have clear visual levels for:

- Page title
- Section title
- Card title
- Body text
- Secondary text
- Labels
- Help text
- Error text
- Table text
- Numeric/financial values

Do not randomly change font sizes between pages.

Do not use excessive font weights.

Typography should communicate hierarchy, not decoration.

---

# 5. Layout

Every page should have a predictable structure.

Prefer:

Page
 +-- Page Header
 ¦    +-- Title
 ¦    +-- Description
 ¦    +-- Primary Action
 ¦
 +-- Filters / Controls
 ¦
 +-- Main Content
 ¦
 +-- Supporting Content

Maintain consistent:

- Maximum content width
- Page padding
- Section spacing
- Grid spacing
- Sidebar width
- Header height

Do not make one page significantly more cramped or spacious than another without a reason.

---

# 6. Navigation

Navigation must remain consistent across the application.

Use the same:

- Sidebar
- Header
- Logo
- Navigation item style
- Active state
- Hover state
- Collapsed state
- Mobile behavior

The current page must always be obvious.

Avoid different navigation behavior on different pages.

---

# 7. Buttons

Create a consistent button hierarchy.

Primary:
Used for the main action.

Secondary:
Used for supporting actions.

Danger:
Used for destructive actions.

Ghost:
Used for low-emphasis actions.

Every button should have consistent:

- Height
- Padding
- Border radius
- Typography
- Icon size
- Hover state
- Disabled state
- Loading state

Do not create five visually different "Save" buttons.

---

# 8. Forms

All forms should follow the same structure.

Example:

Label
Input
Helper text / validation message

Maintain consistent:

- Label positioning
- Input height
- Border
- Focus state
- Error state
- Disabled state
- Required indicator
- Spacing

Never rely exclusively on placeholder text instead of labels.

Validation errors should appear close to the field that caused them.

Do not make the user guess what went wrong.

---

# 9. Tables

Tables are extremely important for billing software.

Maintain a consistent table system.

Tables should support appropriate:

- Column alignment
- Header styling
- Row height
- Hover state
- Pagination
- Empty state
- Loading state
- Error state
- Responsive behavior

Financial numbers should generally be aligned consistently.

Dates should use one format throughout the application.

Currency should use one consistent format.

GST percentages should use one consistent format.

Do not create a completely different table style on every page.

---

# 10. Financial UI

Financial information must have strong visual hierarchy.

Examples:

Subtotal
?10,000.00

CGST
?900.00

SGST
?900.00

Grand Total
?11,800.00

Use consistent:

- Currency formatting
- Decimal precision
- Alignment
- Font weight
- Tax labels

Never make the frontend the source of truth for financial calculations.

Display values returned by the backend.

---

# 11. Invoice Creation UX

Invoice creation is a core workflow.

Optimize it for speed and clarity.

The user should easily understand:

Customer
Invoice details
Products
Quantity
Price
Discount
Tax
Totals
Payment information
Final action

Avoid unnecessary steps.

Avoid excessive modals.

Avoid forcing users to navigate away from the invoice unnecessarily.

Important totals should remain visible while the user is creating the invoice where practical.

---

# 12. Loading States

Never leave the user staring at an empty page while an API request is running.

Use:

- Skeletons
- Spinners
- Button loading states
- Table loading states
- Page loading states

Do not randomly mix different loading indicators.

Use one consistent loading language throughout the application.

---

# 13. Empty States

Every data-driven page must handle zero data.

Examples:

No invoices yet.
No customers yet.
No products yet.

Empty states should explain:

1. What is missing.
2. Why it matters.
3. What the user can do next.

Whenever possible provide a clear CTA.

Example:

"No customers yet"

"Add your first customer to start creating invoices."

[Add Customer]

---

# 14. Error States

Errors must be understandable to normal business users.

Do not display:

"500 Internal Server Error"

as the only message.

Prefer:

"Unable to create invoice"

"Something went wrong while saving the invoice. Please try again."

Technical details may be logged but should not normally be exposed to users.

---

# 15. Notifications

Use one notification system.

Do not mix:

alert()
toast
custom banners
random modals

for similar events.

Success:

"Invoice created successfully."

Error:

"Unable to create invoice."

Warning:

"This invoice has not been finalized."

Notifications should be:

- Consistent
- Short
- Useful
- Non-blocking when possible

---

# 16. Modals

Do not overuse modals.

Use modals for:

- Confirmation
- Short focused forms
- Important decisions

Do not put large multi-step workflows inside tiny modal windows.

Every modal should have:

- Clear title
- Clear purpose
- Close action
- Primary action
- Secondary/cancel action
- Loading state
- Error handling

Destructive actions should require appropriate confirmation.

---

# 17. Responsive Design

The application must work on:

- Desktop
- Laptop
- Tablet
- Mobile

Do not treat mobile as an afterthought.

Check:

- Navigation
- Tables
- Forms
- Invoice creation
- Modals
- Buttons
- Cards
- Dashboard
- Sidebar

Avoid horizontal scrolling where it can reasonably be prevented.

For complex tables, use responsive strategies instead of simply shrinking everything until it becomes unreadable.

---

# 18. Accessibility

Follow basic accessibility standards.

Use:

- Semantic HTML
- Proper labels
- Keyboard navigation
- Visible focus states
- Accessible buttons
- Accessible dialogs
- Appropriate ARIA attributes when necessary
- Sufficient color contrast

Never communicate important information through color alone.

Example:

Do not rely only on:

Red = error
Green = success

Also provide text/icons/context.

---

# 19. Icons (STRICT: NEVER USE EMOJIS)

CRITICAL RULE: NEVER use unicode emojis (such as 📥, ⚠️, ⚡, 💳, 🚚, 📞, 🔄, ✓, ✕, 🚫, 📋, etc.) anywhere in the application.

1. Use standard SVG icons (e.g. Feather / Lucide style line icons) consistently.
2. Maintain consistent:
   - Stroke width (default `strokeWidth="2"` or `1.75`)
   - Standard sizes (e.g., 14px, 16px, 18px, 20px)
   - Proper vertical alignment (`display: inline-flex; align-items: center;`)
   - `currentColor` stroke inheritance
3. Do not mix random icon sets or use OS-rendered emojis.
4. Emojis look amateur, render inconsistently across operating systems and browsers, and break professional SaaS enterprise design consistency.
5. In alerts, banners, and toasts, use structured SVG icons (`check`, `alert-triangle`, `info`, `x-circle`) paired with semantic CSS classes.
6. In action buttons and tables, use clean standard SVG icons or plain text.

Icons should support meaning.

Do not add icons purely for decoration.

---

# 20. Responsive Tables

For billing data, never blindly force tables into tiny mobile screens.

Possible approaches:

1. Horizontal scrolling for genuinely tabular information.
2. Responsive row/card transformation for simpler data.
3. Hide low-priority columns when appropriate.
4. Provide a details view.

Do not sacrifice readability simply to avoid horizontal scrolling.

---

# 21. Accessibility and UX for Destructive Actions

For:

- Delete customer
- Delete product
- Cancel invoice
- Remove business data

show an appropriate confirmation.

Clearly explain what will happen.

Do not use vague:

"Are you sure?"

Prefer:

"Delete customer?"

"This will remove the customer from your active customer list. Existing invoices will not be deleted."

[Cancel] [Delete Customer]

---

# 22. User Feedback

Every user action should have an understandable result.

Examples:

Save:
? loading
? success/error

Delete:
? confirmation
? loading
? success/error

Login:
? loading
? success/error

Invoice creation:
? loading
? success
? navigate/show invoice

Never allow the UI to appear frozen.

---

# 23. Avoid UI Drift

Before implementing a new page, inspect at least:

- Existing dashboard
- Existing forms
- Existing tables
- Existing buttons
- Existing modals
- Existing navigation

Reuse their visual language.

If you discover inconsistency in existing components, fix the shared component rather than creating another variant.

Prefer fixing:

<Button>

over creating:

<NewButton>
<InvoiceButton>
<CustomerButton>
<DashboardButton>

unless their behavior genuinely differs.

---

# 24. Component Architecture

Build reusable components around repeated patterns.

Good:

components/ui/Button
components/ui/Input
components/ui/Modal
components/ui/Table

components/forms/CustomerForm
components/forms/ProductForm
components/forms/InvoiceForm

components/invoices/InvoiceSummary

Avoid huge page components.

Pages should compose components rather than contain hundreds of lines of UI markup.

---

# 25. Before Implementing Any UI

Always perform this checklist:

1. Inspect existing UI.
2. Identify reusable components.
3. Identify existing design tokens.
4. Identify existing spacing/typography.
5. Identify existing responsive patterns.
6. Reuse existing components.
7. Implement the smallest consistent change.
8. Check desktop.
9. Check mobile.
10. Check loading state.
11. Check empty state.
12. Check error state.
13. Check keyboard/accessibility.
14. Check visual consistency with neighboring pages.

---

# 26. Strong Rule

DO NOT create a new design language.

DO NOT introduce arbitrary colors.

DO NOT introduce arbitrary spacing.

DO NOT introduce arbitrary typography.

DO NOT create one-off buttons.

DO NOT create one-off modals.

DO NOT create one-off tables.

DO NOT redesign an existing page unless explicitly asked.

The goal is not to make every page individually beautiful.

The goal is to make the ENTIRE APPLICATION feel like it was designed by the SAME PRODUCT DESIGN TEAM.

When in doubt, copy the established pattern rather than inventing a new one.
