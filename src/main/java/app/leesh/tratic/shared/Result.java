package app.leesh.tratic.shared;

import java.util.function.Function;

public sealed interface Result<T, E>
        permits Result.Ok, Result.Err {

    <U> Result<U, E> map(Function<T, U> mapper);

    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.ok(mapper.apply(value));
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.err(error);
        }
    }

    static <T, E> Ok<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Err<T, E> err(E error) {
        return new Err<>(error);
    }
}
