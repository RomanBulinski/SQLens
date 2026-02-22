package org.buulean.sqlensbackend.presentation.controller;

import org.buulean.sqlensbackend.application.usecase.AnalyzeQueryUseCase;
import org.buulean.sqlensbackend.domain.model.QueryGraph;
import org.buulean.sqlensbackend.domain.result.ParseError;
import org.buulean.sqlensbackend.domain.result.ParseResult;
import org.buulean.sqlensbackend.presentation.dto.DiagramResponseDto;
import org.buulean.sqlensbackend.presentation.dto.SqlAnalyzeRequest;
import org.buulean.sqlensbackend.presentation.mapper.DiagramDtoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql")
public class SqlAnalyzeController {

    private final AnalyzeQueryUseCase analyzeQueryUseCase;
    private final DiagramDtoMapper    mapper;

    public SqlAnalyzeController(AnalyzeQueryUseCase analyzeQueryUseCase,
                                DiagramDtoMapper mapper) {
        this.analyzeQueryUseCase = analyzeQueryUseCase;
        this.mapper              = mapper;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody SqlAnalyzeRequest request) {

        ParseResult<QueryGraph, ParseError> result =
                analyzeQueryUseCase.execute(request.getSql());

        if (result instanceof ParseResult.Success<QueryGraph, ParseError> s) {
            DiagramResponseDto dto = mapper.toDto(s.value());

            // NFR: warn on high complexity
            if (s.value().complexity() > 20) {
                dto.setWarning("Very complex query detected. Performance may be impacted.");
            }

            return ResponseEntity.ok(dto);
        }

        ParseError error = ((ParseResult.Failure<QueryGraph, ParseError>) result).error();
        return ResponseEntity.badRequest().body(mapper.toErrorDto(error));
    }
}
