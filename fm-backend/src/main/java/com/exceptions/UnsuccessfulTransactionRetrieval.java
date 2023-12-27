package com.exceptions;

public class UnsuccessfulTransactionRetrieval extends Exception {
    public UnsuccessfulTransactionRetrieval(String errorMessage) {
        super(errorMessage);
    }
}
