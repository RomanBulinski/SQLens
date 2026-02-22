package org.buulean.sqlensbackend.infrastructure.config;

import org.buulean.sqlensbackend.application.service.QueryValidator;
import org.buulean.sqlensbackend.application.validation.EmptyQueryValidator;
import org.buulean.sqlensbackend.application.validation.QueryLengthValidator;
import org.buulean.sqlensbackend.application.validation.StatementTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    /**
     * Wires the validation Chain of Responsibility:
     * EmptyQuery → QueryLength → StatementType
     */
    @Bean
    public QueryValidator validationChain() {
        QueryValidator empty  = new EmptyQueryValidator();
        QueryValidator length = new QueryLengthValidator();
        QueryValidator type   = new StatementTypeValidator();
        empty.setNext(length).setNext(type);
        return empty;
    }
}
