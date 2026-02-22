package org.buulean.sqlensbackend.application.usecase;

import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.application.service.DiagramModelBuilder;
import org.buulean.sqlensbackend.application.service.QueryValidator;
import org.buulean.sqlensbackend.domain.model.QueryGraph;
import org.buulean.sqlensbackend.domain.port.SqlParser;
import org.buulean.sqlensbackend.domain.result.ParseError;
import org.buulean.sqlensbackend.domain.result.ParseResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates the full analysis pipeline:
 * Validate → Parse → Build graph.
 */
@Service
public class AnalyzeQueryUseCase {

    private final QueryValidator      validationChain;
    private final SqlParser           parser;
    private final DiagramModelBuilder builder;

    public AnalyzeQueryUseCase(QueryValidator validationChain,
                               SqlParser parser,
                               DiagramModelBuilder builder) {
        this.validationChain = validationChain;
        this.parser          = parser;
        this.builder         = builder;
    }

    public ParseResult<QueryGraph, ParseError> execute(String rawSql) {

        // 1. Validate (Chain of Responsibility)
        Optional<ParseError> validationError = validationChain.validate(rawSql);
        if (validationError.isPresent()) {
            return ParseResult.failure(validationError.get());
        }

        // 2. Parse (Adapter hides JSQLParser)
        ParseResult<Statement, ParseError> parsed = parser.parse(rawSql);
        if (!parsed.isSuccess()) {
            return ParseResult.failure(((ParseResult.Failure<Statement, ParseError>) parsed).error());
        }

        Statement statement = ((ParseResult.Success<Statement, ParseError>) parsed).value();

        // 3. Build graph (Open/Closed extensible extractors)
        QueryGraph graph = builder.build(statement);

        return ParseResult.success(graph);
    }
}
