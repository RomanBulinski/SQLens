package org.buulean.sqlensbackend.application.validation;

import org.buulean.sqlensbackend.application.service.QueryValidator;
import org.buulean.sqlensbackend.domain.result.ParseError;

import java.util.Optional;

public class EmptyQueryValidator extends QueryValidator {

    @Override
    protected Optional<ParseError> doValidate(String sql) {
        if (sql == null || sql.isBlank()) {
            return Optional.of(ParseError.emptyInput());
        }
        return Optional.empty();
    }
}
