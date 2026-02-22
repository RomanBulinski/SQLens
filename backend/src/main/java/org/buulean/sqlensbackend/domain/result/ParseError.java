package org.buulean.sqlensbackend.domain.result;

public class ParseError {

    private final String  code;
    private final String  message;
    private final Integer line;
    private final Integer column;
    private final String  suggestion;

    public ParseError(String code, String message, Integer line, Integer column, String suggestion) {
        this.code       = code;
        this.message    = message;
        this.line       = line;
        this.column     = column;
        this.suggestion = suggestion;
    }

    public String  getCode()       { return code; }
    public String  getMessage()    { return message; }
    public Integer getLine()       { return line; }
    public Integer getColumn()     { return column; }
    public String  getSuggestion() { return suggestion; }

    // ── Static factories ────────────────────────────────────────────────

    public static ParseError emptyInput() {
        return new ParseError(
            "EMPTY_INPUT",
            "SQL query must not be empty.",
            null, null, null
        );
    }

    public static ParseError tooLong(int actual, int max) {
        return new ParseError(
            "QUERY_TOO_LONG",
            "Query length " + actual + " exceeds the maximum of " + max + " characters.",
            null, null, null
        );
    }

    public static ParseError unsupportedStatement() {
        return new ParseError(
            "UNSUPPORTED_STATEMENT",
            "Only SELECT and WITH (CTE) statements are supported.",
            null, null,
            "Start your query with SELECT or WITH."
        );
    }

    public static ParseError parseError(String message, Integer line, Integer column) {
        return new ParseError("PARSE_ERROR", message, line, column, null);
    }

    public static ParseError internalError(String message) {
        return new ParseError("INTERNAL_ERROR", message, null, null, null);
    }
}
