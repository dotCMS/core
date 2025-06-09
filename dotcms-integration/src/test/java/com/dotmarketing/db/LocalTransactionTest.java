package com.dotmarketing.db;

import com.dotmarketing.db.LocalTransaction.Tx;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

/**
 * Here we're testing a little transaction context
 * that allows us to run code in a Monad-like fashion,
 */
public class LocalTransactionTest {

    public static final String TRANSACTION_COMPLETED_SUCCESSFULLY = "Transaction completed successfully";

    /**
     * This test verifies that the transaction context can execute a block of code
     * and return a result when the transaction is committed successfully.
     * It also checks that the onCommit block is executed.
     */
    @Test
    public void TxOnSuccessfulCommitTest() throws Exception {
        final AtomicBoolean commit = new AtomicBoolean(false);
        final AtomicBoolean rollback = new AtomicBoolean(false);
        final Tx<String> tx = LocalTransaction.<String>tx();
        final String result = tx.of(() -> {
            Logger.info(this, "Executing transaction logic");
            return TRANSACTION_COMPLETED_SUCCESSFULLY;
        }).onRollback(() -> {
            // This block will execute if the transaction is rolled back
            Logger.info(this, "Transaction rolled back");
            rollback.set(true);
        }).onCommit(() -> {
            // This block will execute if the transaction is committed
            Logger.info(this, "Transaction committed successfully");
            commit.set(true);
        }).run();
        Assert.assertEquals(TRANSACTION_COMPLETED_SUCCESSFULLY, result);
        Assert.assertTrue(commit.get());
        Assert.assertFalse(rollback.get());
    }

    /**
     * This test verifies that the transaction context can handle a rollback scenario.
     * It simulates an exception to trigger the rollback,
     * @throws Exception
     */
    @Test
    public void TxOnRollBackTest() throws Exception {
        final AtomicBoolean commit = new AtomicBoolean(false);
        final AtomicBoolean rollback = new AtomicBoolean(false);
        final Tx<String> tx = LocalTransaction.<String>tx();
        final String result = tx.of(() -> {
            throw new RuntimeException("Simulated exception to trigger rollback");
        }).onRollback(() -> {
            // This block will execute if the transaction is rolled back
            Logger.info(this, "Transaction rolled back");
            rollback.set(true);
        }).onCommit(() -> {
            // This block will execute if the transaction is committed
            Logger.info(this, "Transaction committed successfully");
            commit.set(true);
        }).run();
        // Since the transaction is rolled back, result should be null
        Assert.assertNull(result);
        // Verify that the rollback block was executed
        Assert.assertTrue(rollback.get());
        // Verify that the commit block was not executed
        Assert.assertFalse(commit.get());
    }

    /**
     * This test verifies that an exception thrown within the transaction context bubbles up correctly
     * The Rollback block should execute, and the Commit block should not. and the exception should be caught.
     * @throws Exception
     */
    @Test
    public void TxOnExceptionBubbleUpTest() throws Exception {
        final AtomicBoolean commit = new AtomicBoolean(false);
        final AtomicBoolean rollback = new AtomicBoolean(false);
        final AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        final Tx<String> tx = LocalTransaction.<String>tx();
        String result = null;
        try {
            result = tx.of(() -> {
                throw new DotDataException("Simulated exception to trigger rollback");
            }).onRollback(() -> {
                // This block will execute if the transaction is rolled back
                Logger.info(this, "Transaction rolled back");
                rollback.set(true);
            }).onCommit(() -> {
                // This block will execute if the transaction is committed
                Logger.info(this, "Transaction committed successfully");
                commit.set(true);
            }).run(true);
        } catch (Exception e) {
            // Expected exception, we can assert on it if needed
            Logger.error(this, "Caught expected exception: " + e.getMessage());
            if(e instanceof DotDataException && e.getMessage().equals("Simulated exception to trigger rollback")) {
                exceptionThrown.set(true);
            }
        }
        // Since the transaction is rolled back, result should be null
        Assert.assertNull(result);
        // Verify that the rollback block was executed
        Assert.assertTrue(rollback.get());
        // Verify that the commit block was not executed
        Assert.assertFalse(commit.get());
        // Verify that the exception was thrown
        Assert.assertTrue(exceptionThrown.get());
    }

}