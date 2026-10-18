package dev.umut.wallet_api.account;

public enum EntryType {
    DEPOSIT(true),WITHDRAWAL(false),TRANSFER_IN(true),TRANSFER_OUT(false);

    private final boolean increasesBalance;

    EntryType(boolean increasesBalance) {
        this.increasesBalance = increasesBalance;
    }

    public boolean increasesBalance() {
        return increasesBalance;
    }
}
