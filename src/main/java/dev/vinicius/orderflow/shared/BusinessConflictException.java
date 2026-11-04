package dev.vinicius.orderflow.shared;

public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String message) { super(message); }
}
