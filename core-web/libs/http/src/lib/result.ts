export type Result<T, E> = SuccessResult<T> | ErrorResult<E>;

type SuccessResult<T> = {
    ok: true;
    val: T;
};

type ErrorResult<E> = {
    ok: false;
    val: E;
};

export function Ok<T>(val: T): Result<T, never> {
    return { ok: true, val };
}

export function Err<E>(val: E): Result<never, E> {
    return { ok: false, val };
}
