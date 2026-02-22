package org.buulean.sqlensbackend.domain.port;

import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.domain.result.ParseError;
import org.buulean.sqlensbackend.domain.result.ParseResult;

/** Application → Infrastructure port: parses raw SQL text into an AST. */
public interface SqlParser {
    ParseResult<Statement, ParseError> parse(String sql);
}
