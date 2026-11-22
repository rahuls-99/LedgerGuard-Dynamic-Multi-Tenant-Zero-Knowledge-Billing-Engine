package com.ledgerguard.ledger.posting;
public interface FailureInjector { enum Point { AFTER_DEBIT, BEFORE_COMMIT } void checkpoint(Point point); }
