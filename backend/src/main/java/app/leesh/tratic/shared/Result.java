package app.leesh.tratic.shared;

import java.util.function.Function;

public sealed interface Result<T, E>
        permits Result.Ok, Result.Err {

    <U> Result<U, E> map(Function<T, U> mapper);
    <F> Result<T, F> mapError(Function<E, F> mapper);
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    <R> R fold(Function<T, R> onOk, Function<E, R> onErr);

    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.ok(mapper.apply(value));
        }

        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return Result.ok(value);
        }

        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public <R> R fold(Function<T, R> onOk, Function<E, R> onErr) {
            return onOk.apply(value);
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.err(error);
        }

        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return Result.err(mapper.apply(error));
        }

        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return Result.err(error);
        }

        @Override
        public <R> R fold(Function<T, R> onOk, Function<E, R> onErr) {
            return onErr.apply(error);
        }
    }

    static <T, E> Ok<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Err<T, E> err(E error) {
        return new Err<>(error);
    }
}
