package app.leesh.tratic.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

public class ResultTest {

    @Test
    public void ok_mapError_is_noop_and_fold_uses_ok_branch() {
        Result<Integer, String> result = Result.<Integer, String>ok(10).mapError(err -> "x-" + err);

        assertInstanceOf(Result.Ok.class, result);
        String folded = result.fold(v -> "ok:" + v, e -> "err:" + e);
        assertEquals("ok:10", folded);
    }

    @Test
    public void err_mapError_transforms_and_fold_uses_err_branch() {
        Result<Integer, String> result = Result.<Integer, Integer>err(7).mapError(v -> "code-" + v);

        assertInstanceOf(Result.Err.class, result);
        String folded = result.fold(v -> "ok:" + v, e -> "err:" + e);
        assertEquals("err:code-7", folded);
    }
}
