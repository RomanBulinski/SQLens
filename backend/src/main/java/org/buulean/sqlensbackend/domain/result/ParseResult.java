package org.buulean.sqlensbackend.domain.result;

public sealed interface ParseResult<T, E> permits ParseResult.Success, ParseResult.Failure {

    record Success<T, E>(T value) implements ParseResult<T, E> {}
    record Failure<T, E>(E error) implements ParseResult<T, E> {}

    static <T, E> ParseResult<T, E> success(T value) { return new Success<>(value); }
    static <T, E> ParseResult<T, E> failure(E error)  { return new Failure<>(error); }

    default boolean isSuccess() { return this instanceof Success<T, E>; }
}
