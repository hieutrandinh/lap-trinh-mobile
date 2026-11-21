package org.example.be.rest.usecase;

public record CreationResult<T>(T record, boolean alreadyExisted) {}
