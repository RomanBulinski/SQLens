package org.buulean.sqlensbackend.application.validation;

import org.buulean.sqlensbackend.application.service.QueryValidator;
import org.buulean.sqlensbackend.domain.result.ParseError;

import java.util.Optional;

public class QueryLengthValidator extends QueryValidator {

    private static final int MAX_LENGTH = 100_000;

    @Override
    protected Optional<ParseError> doValidate(String sql) {
        if (sql.length() > MAX_LENGTH) {
            return Optional.of(ParseError.tooLong(sql.length(), MAX_LENGTH));
        }
        return Optional.empty();
    }
}
