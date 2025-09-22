package com.warehouse.warehouse_management.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ItemNotDeletableException extends RuntimeException {
    public ItemNotDeletableException(String message) { super(message); }
}