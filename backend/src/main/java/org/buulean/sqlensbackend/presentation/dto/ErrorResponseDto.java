package org.buulean.sqlensbackend.presentation.dto;

public class ErrorResponseDto {

    private String  code;
    private String  message;
    private Integer line;
    private Integer column;
    private String  suggestion;

    public ErrorResponseDto() {}

    public ErrorResponseDto(String code, String message,
                            Integer line, Integer column, String suggestion) {
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
}
