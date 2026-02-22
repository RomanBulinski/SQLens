package org.buulean.sqlensbackend.application.service;

import org.buulean.sqlensbackend.domain.result.ParseError;

import java.util.Optional;

/** Abstract base for Chain of Responsibility — pre-parse SQL validation. */
public abstract class QueryValidator {

    private QueryValidator next;

    public QueryValidator setNext(QueryValidator next) {
        this.next = next;
        return next;
    }

    public final Optional<ParseError> validate(String sql) {
        Optional<ParseError> error = doValidate(sql);
        if (error.isPresent()) return error;
        return next != null ? next.validate(sql) : Optional.empty();
    }

    protected abstract Optional<ParseError> doValidate(String sql);
}
