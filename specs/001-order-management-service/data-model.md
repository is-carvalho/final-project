# Data Model: E-Commerce Order Management

## Order

- Fields: `id`, `customerId`, `status`, `totalAmount`, `currency`, `paymentRejectionCount`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `correlationId`, `version`.
- Relationships: One order has many `OrderItem` records and zero or more `PaymentTransaction` records.
- Validation rules: `customerId` is required. `status` must be one of `DRAFT`, `CONFIRMED`, `PAYMENT_PENDING`, `PAYMENT_APPROVED`, `PAYMENT_REJECTED`, `CANCELLED`. Active statuses are `DRAFT`, `CONFIRMED`, `PAYMENT_PENDING`, and `PAYMENT_REJECTED`. Editable status is `DRAFT` only.
- Persistence rules: Use optimistic locking through `version`. Enforce at most one active order per customer with a database-level uniqueness strategy suitable for active statuses.

## OrderItem

- Fields: `id`, `orderId`, `productId`, `quantity`, `unitPrice`, `lineTotal`, `createdAt`, `updatedAt`.
- Relationships: Many items belong to one order.
- Validation rules: `productId` is required. `quantity` must be greater than zero. One order can contain only one item entry for the same product.
- State behavior: Adding an existing product increases quantity instead of creating another row. `unitPrice` and `lineTotal` are finalized/recalculated when the order is confirmed.

## PaymentTransaction

- Fields: `id`, `orderId`, `status`, `attemptNumber`, `idempotencyKey`, `providerTransactionId`, `amount`, `currency`, `requestedAt`, `processedAt`, `failureReason`, `correlationId`, `version`.
- Relationships: Many payment transactions may belong to one order across retries; a repeated initiation with the same idempotency identity returns the same transaction outcome.
- Validation rules: Payment initiation is allowed only for `CONFIRMED` or retryable `PAYMENT_REJECTED` orders before the third rejection. Status values are `PENDING`, `APPROVED`, and `REJECTED`.

## PaymentResultEvent

- Fields: `id`, `paymentTransactionId`, `providerEventId`, `eventType`, `receivedAt`, `correlationId`, `payloadHash`.
- Relationships: Belongs to one payment transaction.
- Validation rules: `providerEventId` is unique. Replayed approval/rejection events must produce no additional business side effects.

## IdempotencyRecord

- Fields: `id`, `operation`, `resourceId`, `callerId`, `idempotencyKey`, `requestHash`, `responseStatus`, `responseBody`, `createdAt`, `expiresAt`.
- Relationships: References the operation result resource when available.
- Validation rules: Every mutable endpoint requires `Idempotency-Key`. Reusing the same key with the same request returns the stored response. Reusing the same key with a different request hash returns a conflict Problem Details response.

## External Read Models

### Customer Validation Result

- Fields: `customerId`, `exists`, `active`, `blocked`.
- Validation rules: An order can be created only when the customer exists, is active, and is not blocked.

### Product Validation Result

- Fields: `productId`, `exists`, `available`, `currentPrice`, `currency`.
- Validation rules: Products can be added or priced only when they exist and are available. Confirmation uses current prices.

## Notification Event

- Fields: `eventId`, `eventType`, `orderId`, `customerId`, `occurredAt`, `correlationId`.
- Event types: `ORDER_CONFIRMED`, `PAYMENT_APPROVED`, `ORDER_CANCELLED`.
- Delivery rule: Emitted through the external notification HTTP adapter after the corresponding committed business transition.

## Order State Transitions

```text
DRAFT -> CONFIRMED
DRAFT -> CANCELLED
CONFIRMED -> PAYMENT_PENDING
CONFIRMED -> CANCELLED
PAYMENT_PENDING -> PAYMENT_APPROVED
PAYMENT_PENDING -> PAYMENT_REJECTED
PAYMENT_REJECTED -> PAYMENT_PENDING
PAYMENT_REJECTED -> CANCELLED
```

Invalid transitions are rejected with RFC 7807 Problem Details. The third payment rejection transitions the order to `CANCELLED` automatically.
