package com.dotcms.cli.command;

import com.dotcms.DotCMSITProfile;
import com.dotcms.cli.command.PushContext.LockExecException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.util.Optional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class PushContextImplIT {

    @Inject
    PushContextImpl pushContext;

    /**
     * Scenario: ExecWithinLock is called with a key that is not locked The inner function returns a value
     * Expect: The value is returned
     * @throws LockExecException
     */
    @Test
    void Test_ExecWithinLock_Expect_Success() throws LockExecException {
        final Optional<String> optional = pushContext.execWithinLock("anyKeyExpectSuccess", () -> Optional.of("success"));
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals("success", optional.get());
        Assertions.assertTrue(pushContext.contains("anyKeyExpectSuccess"));
    }

    /**
     * Scenario: ExecWithinLock is called with any key, and exception is thrown
     * Expect: The exception is thrown and the key is does not remain locked
     * @throws LockExecException
     */
    @Test
    void Test_ExecWithinLock_Expect_Exception() throws LockExecException {
        Optional<String> optional = Optional.empty();
        try {
            optional = pushContext.execWithinLock("anyKeyExpectException",
                    () -> {
                        throw new IllegalStateException("exception");
                    });
        }catch (final IllegalStateException e) {
            Assertions.assertEquals("exception", e.getMessage());
        }
        Assertions.assertFalse(optional.isPresent());
        Assertions.assertFalse(pushContext.contains("anyKeyExpectException"));
    }

    /**
     * Scenario: ExecWithinLock is called with any key
     * Expect: The inner function returns an empty optional, the key is not locked
     * @throws LockExecException
     */
    @Test
    void Test_ExecWithinLock_Expect_Failure() throws LockExecException {
        final Optional<String> optional = pushContext.execWithinLock("anyKeyExpectFailure", Optional::empty);
        Assertions.assertFalse(optional.isPresent());
        Assertions.assertFalse(pushContext.contains("anyKeyExpectFailure"));
    }

    /**
     * Scenario: ExecWithinLock is called with a key that is already locked. Then a second call is made with the same key
     * Expect: The second call returns an empty optional cause the keu is already locked
     * This basically demonstrate that a transactions bound to a key is not reentrant
     * @throws LockExecException
     */
    @Test
    void Test_Consecutive_Calls_On_The_Same_Lock_Key() throws LockExecException {
        //First make a successful call, simulate a successful a transaction
        final Optional<String> optional1 = pushContext.execWithinLock("expectSuccess", () -> Optional.of("success"));
        Assertions.assertTrue(optional1.isPresent());
        Assertions.assertTrue(pushContext.contains("expectSuccess"));

        //This should return an empty optional since the key is already locked
        final Optional<String> optional2 = pushContext.execWithinLock("expectSuccess", () -> Optional.of("success"));
        Assertions.assertTrue(optional2.isEmpty());


    }

}