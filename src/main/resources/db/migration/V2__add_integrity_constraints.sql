-- Entity'lerde ilişki (@ManyToOne) olmadığı için Hibernate foreign key üretmemişti.
-- Burada veritabanı seviyesinde bütünlük ekliyoruz.

ALTER TABLE account_entry
    ADD CONSTRAINT fk_entry_account FOREIGN KEY (account_id) REFERENCES account (id),
    ADD CONSTRAINT fk_entry_transfer FOREIGN KEY (transfer_id) REFERENCES transfer (id),
    ADD CONSTRAINT ck_entry_amount_positive CHECK (amount > 0);

ALTER TABLE transfer
    ADD CONSTRAINT fk_transfer_from_account FOREIGN KEY (from_account_id) REFERENCES account (id),
    ADD CONSTRAINT fk_transfer_to_account FOREIGN KEY (to_account_id) REFERENCES account (id),
    ADD CONSTRAINT ck_transfer_amount_positive CHECK (amount > 0),
    ADD CONSTRAINT ck_transfer_different_accounts CHECK (from_account_id <> to_account_id);

-- Uygulama kodu zaten engelliyor; bu, kodda bir hata olursa son güvenlik ağı.
ALTER TABLE account
    ADD CONSTRAINT ck_account_balance_non_negative CHECK (balance >= 0);