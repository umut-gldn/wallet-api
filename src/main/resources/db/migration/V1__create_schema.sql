CREATE SEQUENCE account_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE app_user (
                          id            uuid                        NOT NULL,
                          email         varchar(255)                NOT NULL,
                          password_hash varchar(100)                NOT NULL,
                          created_at    timestamp(6) with time zone NOT NULL,
                          CONSTRAINT pk_app_user PRIMARY KEY (id),
                          CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE account (
                         id             uuid                        NOT NULL,
                         account_number varchar(16)                 NOT NULL,
                         owner_id       uuid                        NOT NULL,
                         currency       varchar(3)                  NOT NULL,
                         status         varchar(16)                 NOT NULL,
                         balance        numeric(19, 2)              NOT NULL,
                         created_at     timestamp(6) with time zone NOT NULL,
                         CONSTRAINT pk_account PRIMARY KEY (id),
                         CONSTRAINT uk_account_account_number UNIQUE (account_number),
                         CONSTRAINT uk_account_owner_currency UNIQUE (owner_id, currency),
                         CONSTRAINT ck_account_currency CHECK (currency IN ('TRY', 'USD', 'EUR')),
                         CONSTRAINT ck_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE TABLE transfer (
                          id              uuid                        NOT NULL,
                          owner_id        uuid                        NOT NULL,
                          from_account_id uuid                        NOT NULL,
                          to_account_id   uuid                        NOT NULL,
                          amount          numeric(19, 2)              NOT NULL,
                          currency        varchar(3)                  NOT NULL,
                          idempotency_key varchar(64)                 NOT NULL,
                          created_at      timestamp(6) with time zone NOT NULL,
                          CONSTRAINT pk_transfer PRIMARY KEY (id),
                          CONSTRAINT uk_transfer_owner_idempotency_key UNIQUE (owner_id, idempotency_key),
                          CONSTRAINT ck_transfer_currency CHECK (currency IN ('TRY', 'USD', 'EUR'))
);

CREATE TABLE account_entry (
                               id            uuid                        NOT NULL,
                               account_id    uuid                        NOT NULL,
                               type          varchar(20)                 NOT NULL,
                               amount        numeric(19, 2)              NOT NULL,
                               balance_after numeric(19, 2)              NOT NULL,
                               transfer_id   uuid,
                               created_at    timestamp(6) with time zone NOT NULL,
                               CONSTRAINT pk_account_entry PRIMARY KEY (id),
                               CONSTRAINT ck_entry_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT'))
);

CREATE INDEX idx_entry_account_created ON account_entry (account_id, created_at);