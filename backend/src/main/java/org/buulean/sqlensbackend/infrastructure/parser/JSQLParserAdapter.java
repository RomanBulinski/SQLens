package org.buulean.sqlensbackend.infrastructure.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.domain.port.SqlParser;
import org.buulean.sqlensbackend.domain.result.ParseError;
import org.buulean.sqlensbackend.domain.result.ParseResult;
import org.springframework.stereotype.Component;

/**
 * Adapter (infrastructure) — isolates JSQLParser from the domain layer.
 * Translates JSQLParserException into a domain ParseError.
 */
@Component
public class JSQLParserAdapter implements SqlParser {

    @Override
    public ParseResult<Statement, ParseError> parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return ParseResult.success(statement);
        } catch (JSQLParserException ex) {
            return ParseResult.failure(mapException(ex));
        }
    }

    private ParseError mapException(JSQLParserException ex) {
        String raw = ex.getMessage() != null ? ex.getMessage() : "SQL parse error";
        // JSQLParser messages contain position info like "at line X, column Y"
        Integer line   = extractInt(raw, "line ");
        Integer column = extractInt(raw, "column ");
        // Extract the meaningful part of the message (remove boilerplate)
        String message = raw.contains("\n") ? raw.substring(0, raw.indexOf('\n')).trim() : raw;
        return ParseError.parseError(message, line, column);
    }

    private Integer extractInt(String text, String prefix) {
        int idx = text.toLowerCase().indexOf(prefix);
        if (idx < 0) return null;
        int start = idx + prefix.length();
        int end   = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        if (end == start) return null;
        try { return Integer.parseInt(text.substring(start, end)); }
        catch (NumberFormatException e) { return null; }
    }
}
