package com.shivankkapoor.standbase.service;

public class InvalidCreditDayException extends RuntimeException {
    public InvalidCreditDayException(String message) {
        super(message);
    }
}
