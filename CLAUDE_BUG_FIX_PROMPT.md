# Claude Prompt: Fix and End-to-End Test Storefront Issues

Work on both repositories:

- Backend: `C:\Users\iambh\Desktop\Shades_world\CODEX\sunglass-store-backend`
- Frontend: `C:\Users\iambh\Desktop\Shades_world\CODEX\sunglass-store-frontend`

Act as a senior full-stack engineer. Investigate, implement, and verify every issue below. Do not merely hide UI symptoms or mock successful behavior—trace each issue through the frontend, API, database, authentication/session handling, and relevant business logic, then fix its root cause.

## Required fixes

1. **Cart persistence**
   - Items added to the cart currently disappear after a hard refresh.
   - Persist carts correctly for authenticated users in the database and restore them after page reload/login.
   - If guest carts are supported, persist them safely in browser storage and merge them with the authenticated cart according to the existing product requirements.
   - Ensure quantities, selected variants, prices, and product availability remain correct after restoration.

2. **Variant-specific product information**
   - Description, Details, and Shipping content currently do not change when a different product variant is selected.
   - Make each section display the data belonging to the active variant.
   - Ensure variant selection updates all dependent UI and purchase data consistently, without requiring a reload.

3. **Header search icon**
   - Remove the search icon from the top header.
   - Remove associated unused imports, handlers, and inaccessible empty controls without breaking header layout or responsiveness.

4. **Shop and Collections pages**
   - Add functional Shop and Collections pages matching the existing design system.
   - Add correct routes and update navigation links.
   - Shop must display purchasable products and support the filtering/sorting/pagination patterns already present in the project, where applicable.
   - Collections must list available collections and allow users to open a collection and view its products.
   - Include loading, empty, error, and responsive states.

5. **Order cancellation modal**
   - Clicking the cancel-order action must open a confirmation modal before cancellation.
   - Clearly show the consequence of the action and provide Confirm and Keep Order/Close actions.
   - Prevent duplicate submissions, display progress, handle API errors, and update the order UI only after successful cancellation.
   - Preserve keyboard navigation, focus handling, Escape behavior, and accessibility attributes.

6. **Numeric pincode input**
   - Pincode must accept digits only.
   - Reject or sanitize letters, symbols, pasted invalid content, and unsupported whitespace.
   - Preserve leading zeroes by treating the value as a numeric string rather than a number.
   - Apply appropriate length and validation rules for the application's supported country, with matching frontend and backend validation.

7. **Remove add-to-cart counter UI**
   - Remove the counter/quantity control shown after adding a product to the cart from both the main/home page and product listing/product pages.
   - Keep the intended Add to Cart behavior working.
   - Do not remove the cart quantity editor from the cart itself unless the current product requirements explicitly call for it.

8. **Inventory update after order placement**
   - Inventory currently does not decrease after a successful order.
   - Update stock for the exact purchased product variant and quantity when an order is successfully placed.
   - Make the operation atomic/transactional so order creation and inventory mutation cannot become inconsistent.
   - Prevent overselling with server-side stock validation and concurrency-safe updates.
   - Ensure retries, payment callbacks, or duplicate requests cannot decrement inventory more than once; use the project's existing idempotency/payment status model.
   - Do not decrement inventory for failed or abandoned orders. Restore stock on cancellation only if that matches the existing order lifecycle and has not already been restored.
   - Return useful API errors for insufficient stock and refresh affected frontend inventory states.

9. **Reviews**
   - Diagnose and repair the complete review flow: form display, authentication/authorization, validation, API request, database persistence, error handling, and refreshed review list/rating summary.
   - Users must receive clear success and failure feedback.
   - Prevent duplicate or unauthorized reviews according to existing business rules.
   - Ensure ratings are validated to the supported range and review data is rendered safely.

## Implementation expectations

1. First inspect both repositories, their local instruction files, schemas/migrations, API contracts, existing tests, and current UI patterns.
2. Reproduce each bug before changing code and document the root cause in your final report.
3. Reuse existing components and conventions. Avoid unrelated refactors or visual redesigns.
4. Keep frontend and backend contracts synchronized. Add a database migration only when required, and make it safe for existing data.
5. Preserve existing user changes and secrets. Do not reset the worktree, replace environment files, or commit credentials.
6. Add or update automated tests for every corrected behavior, emphasizing backend integration tests for cart, orders/inventory, cancellation, pincode validation, and reviews.
7. Run linting, type checking, unit/integration tests, and production builds for both projects. Fix any failures caused by your work.

## Mandatory end-to-end verification

Run the actual frontend and backend against a test database and complete end-to-end tests in a real browser. Do not report an item as fixed based only on code inspection or unit tests. Create stable automated E2E coverage with the project's existing browser test framework; if none exists, add a minimal Playwright setup.

Verify at minimum:

- Add multiple variants and quantities to a guest cart, hard-refresh, and confirm the cart remains correct.
- Log in with a cart and verify authenticated persistence and any supported guest-cart merge behavior across refresh/logout/login.
- Change variants and confirm Description, Details, Shipping, displayed price/stock, and the variant added to cart all match the selection.
- Confirm the header has no search icon at desktop and mobile widths and its layout remains correct.
- Navigate to Shop and Collections through the header; test direct URLs, refreshes, product/collection navigation, and loading/empty/error states.
- Trigger order cancellation; verify no cancellation occurs before confirmation, closing preserves the order, confirming cancels once, and error/loading states work.
- Try typing and pasting valid and invalid pincode values; confirm both client and server reject invalid input while preserving valid leading zeroes.
- Add products from the home page, listing page, and product detail page; confirm the unwanted counter is absent while cart actions still work.
- Place an order containing more than one variant and quantity; verify exact inventory decrements in both the database and UI. Test insufficient stock and a repeated submission/callback to prove there is no overselling or double decrement.
- Submit a valid review and confirm persistence after refresh and correct rating summary. Also test invalid, unauthenticated, duplicate, and server-error cases as applicable.
- Re-run important existing purchase, cart, navigation, and account flows to catch regressions.

Use deterministic test data and clean it up safely. Capture useful failure evidence while debugging. Do not weaken assertions, skip failing tests, or replace real API/database interactions with mocks in the final E2E suite.

## Completion criteria and final response

Continue until all nine issues work correctly and the relevant automated checks pass. In your final response provide:

- A concise root cause and fix summary for each numbered issue.
- Files and migrations changed.
- Exact commands run and their pass/fail results.
- E2E scenarios executed and their results.
- Any assumptions, remaining limitations, or environment blockers.

If an external credential or service prevents a test, complete every locally testable part, clearly identify the exact blocker, and provide the precise command and setup needed to finish verification. Do not claim full success for anything that was not actually tested.

---

# Additional Required Fixes

Investigate, implement, and end-to-end test these issues in the same frontend and backend repositories. Treat them as required completion criteria alongside the original nine issues.

## 10. Publish customer reviews globally without admin approval

Current behavior:

- A signed-in customer purchases a product.
- After the order reaches the Delivered state, the customer can submit a review and can see their own review.
- Other users cannot see the review until an administrator approves it.

Required behavior:

- A valid review from an eligible customer must be published globally immediately after successful submission.
- Admin approval must not be required.
- Remove the approval dependency consistently from backend creation/query logic, frontend filtering, rating aggregation, and caches.
- Preserve all other review rules, including authentication, delivered-order eligibility, product/order association, rating validation, duplicate-review prevention, safe rendering, and edit/delete permissions if supported.
- Existing reviews waiting for approval must not remain unintentionally hidden. Implement a safe migration or compatibility rule so legitimate historical reviews become globally visible, while preserving reviews explicitly rejected, deleted, flagged, or moderated for abuse if such states exist.
- Do not simply hard-code the UI to show pending reviews; update the underlying review lifecycle and API contract cleanly.

Mandatory E2E tests:

- Customer A buys a product, the order becomes Delivered, and Customer A submits a review.
- Confirm the review appears immediately after submission and remains visible after a hard refresh.
- Sign out, visit the product as a guest, and confirm the review is visible.
- Sign in as Customer B and confirm the same review is visible.
- Confirm the product's review count and average rating update immediately and remain correct after refresh.
- Confirm a customer without a delivered purchase cannot review, and confirm duplicate/invalid reviews are still rejected.
- Verify legitimate historical unapproved reviews follow the new visibility rule without exposing rejected, deleted, or moderated reviews.

## 11. Sign-out/navigation error after admin or customer sign-in

Current error:

```text
ERROR
Something went wrong. Please try again.
    at parseResponse (http://localhost:3000/Ziggy/static/js/bundle.js:19453:19)
    at async signOut (http://localhost:3000/Ziggy/static/js/bundle.js:4259:7)
```

The error occurs when navigating back after signing in as an administrator. Investigate the same flow for customer accounts as well.

Required behavior:

- Browser Back, Forward, refresh, sign-in, and explicit sign-out must work without uncaught errors for both administrator and customer sessions.
- Determine whether navigation is unexpectedly triggering sign-out, whether the session/token is stale or missing, and whether the sign-out endpoint returns an empty/non-JSON response or an error status that `parseResponse` mishandles.
- Correct the root cause across routing/history handling, authentication state hydration, API response parsing, cookies/tokens, and the logout endpoint as applicable.
- Make sign-out idempotent: an already expired or missing session must still result in a safe signed-out frontend state rather than an exception.
- Do not broadly swallow API errors. Handle expected empty responses such as HTTP 204 correctly, preserve useful handling for genuine failures, clear credentials safely, and redirect users to the correct public/auth page.
- Ensure admin and customer roles cannot see stale protected content from browser history after signing out.

Mandatory E2E tests for both admin and customer accounts:

- Sign in, navigate through multiple protected pages, then use browser Back and Forward repeatedly.
- Refresh a protected page after sign-in and verify the session restores correctly.
- Sign out normally and confirm there is no console error, error screen, or rejected request left unhandled.
- Press Back after sign-out and confirm protected data is not exposed and the user is redirected appropriately.
- Repeat sign-out or call it with an expired/missing session and confirm it remains safe.
- Test relevant logout response forms, including the actual server status/body, and verify the frontend parser handles them correctly.
- Inspect browser console errors and failed network requests; do not mark this fixed while related uncaught exceptions remain.

## 12. Fix the Disapprove button in admin Returns & Refunds

Current behavior:

- In the admin Returns & Refunds order detail view, the green Approve button renders correctly.
- The Disapprove button does not render or display correctly.

Required behavior:

- Make the Disapprove action clearly visible, correctly aligned, responsive, and visually consistent with the existing design system.
- Use an appropriate destructive/rejection style that is distinguishable from the green Approve action and meets accessible contrast requirements.
- Ensure the label is not clipped, hidden, overlapped, transparent, or affected by an incorrect disabled/loading style.
- Confirm the issue is not only cosmetic: clicking Disapprove must invoke the correct confirmation and API workflow, prevent duplicate submissions, show loading/error feedback, and update the return/refund status only after success.
- Approve and Disapprove must remain separate actions and must send the correct status without sharing stale handlers or request payloads.
- Preserve keyboard accessibility, focus visibility, and mobile usability.

Mandatory E2E tests:

- Open an eligible return/refund request in the admin interface at desktop and mobile viewport sizes.
- Confirm both Approve and Disapprove buttons are fully visible, readable, aligned, and keyboard accessible.
- Disapprove a test request, confirm the intended modal/prompt if the project uses one, and verify the correct API request and persisted database status.
- Refresh the page and confirm the disapproved state remains correct.
- Test an API failure and confirm the request does not appear disapproved and the administrator receives useful feedback.
- Separately approve another request to prove the existing Approve flow still works and was not regressed.

## Additional completion report requirements

For issues 10–12, include in the final report:

- The reproduced behavior and exact root cause.
- Frontend, backend, schema/migration, and test files changed.
- API status codes and response shapes involved in the sign-out fix.
- The final review visibility/moderation rule, including treatment of existing pending, rejected, deleted, or flagged reviews.
- Browser/viewports used to verify the Returns & Refunds actions.
- Automated test commands and results, plus confirmation that browser console and network logs were checked during E2E testing.
