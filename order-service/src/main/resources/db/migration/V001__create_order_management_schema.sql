create table orders (
    id uuid primary key,
    customer_id varchar(120) not null,
    status varchar(40) not null,
    total_amount numeric(19,2) not null,
    currency varchar(3) not null,
    payment_rejection_count integer not null,
    cancellation_reason varchar(200),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    created_by varchar(120),
    updated_by varchar(120),
    correlation_id varchar(120),
    version bigint not null
);

create unique index uq_orders_active_customer on orders(customer_id)
where status in ('DRAFT', 'CONFIRMED', 'PAYMENT_PENDING', 'PAYMENT_REJECTED');

create table order_items (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    product_id varchar(120) not null,
    quantity integer not null,
    unit_price numeric(19,2),
    line_total numeric(19,2),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    unique(order_id, product_id)
);

create table payment_transactions (
    id uuid primary key,
    order_id uuid not null references orders(id),
    status varchar(40) not null,
    attempt_number integer not null,
    idempotency_key varchar(200) not null unique,
    provider_transaction_id varchar(200),
    amount numeric(19,2) not null,
    currency varchar(3) not null,
    requested_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    failure_reason varchar(300),
    correlation_id varchar(120),
    version bigint not null
);

create table payment_result_events (
    id uuid primary key,
    payment_transaction_id uuid not null references payment_transactions(id),
    provider_event_id varchar(200) not null unique,
    event_type varchar(40) not null,
    received_at timestamp with time zone not null,
    correlation_id varchar(120),
    payload_hash varchar(128) not null
);

create table idempotency_records (
    id uuid primary key,
    operation varchar(120) not null,
    resource_id varchar(120) not null,
    caller_id varchar(120) not null,
    idempotency_key varchar(200) not null,
    request_hash varchar(128) not null,
    response_status integer not null,
    response_body text,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    unique(operation, resource_id, caller_id, idempotency_key)
);
