package org.buulean.sqlensbackend.application.validation;

import org.buulean.sqlensbackend.application.service.QueryValidator;
import org.buulean.sqlensbackend.domain.result.ParseError;

import java.util.Optional;

public class StatementTypeValidator extends QueryValidator {

    @Override
    protected Optional<ParseError> doValidate(String sql) {
        String upper = sql.stripLeading().toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            return Optional.of(ParseError.unsupportedStatement());
        }
        return Optional.empty();
    }
}
