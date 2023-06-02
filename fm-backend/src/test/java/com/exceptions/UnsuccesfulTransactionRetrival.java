package com.exceptions;

public class UnsuccesfulTransactionRetrival extends Exception {
    public UnsuccesfulTransactionRetrival(String errorMessage) {
        super(errorMessage);
    }
}
