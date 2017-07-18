package com.dotmarketing.db;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;


public class DbConnectionFactoryTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    final String SQL_COUNT = "select count(*) as test from inode";
    final String SQL_INSERT = "insert into inode (inode, idate, owner, type) values (?,?,?,'testing')";

    final DotConnect dc = new DotConnect();


    @Test
    public void testSelectTransaction() throws Exception {
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        int i = testSelectInTransaction();
        assertThat("we got the results of our query", i > 0);
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
    }

    @Test
    public void testRollback() throws Exception {
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        int count = getCount();
        boolean exceptionThrown = false;
        try {
            rollbackTransaction();
        } catch (DotDataException dde) {
            exceptionThrown = true;
        }
        assertThat("nothing was added", count == getCount());
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        assertThat("Exception was thrown and handled", exceptionThrown);
    }


    @Test
    public void testStartTransactionIfNeeded() throws Exception {
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        assertThat("Transaction started", DbConnectionFactory.startTransactionIfNeeded());
        assertThat("No need to start another transaction", !DbConnectionFactory.startTransactionIfNeeded());
        assertThat("In transaction", DbConnectionFactory.inTransaction());
        DbConnectionFactory.closeAndCommit();
        assertThat("No transaction", !DbConnectionFactory.inTransaction());

    }

    @Test
    public void testNestedTransactions() throws Exception {
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        int count = getCount();
        int count2 = testNestedTransactions(0);
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        assertThat("Counts are the same", count == count2);

        boolean exceptionThrown = false;
        try {
            count2 = testNestedTransactionsFail(0);
        } catch (Throwable dse) {
            dse.printStackTrace();
            assertThat("we have our DotDataException", dse instanceof DotDataException);
            assertThat("we have our DotStateException", dse.getCause() instanceof DotStateException);
            assertThat("we have the right DotStateException", dse.getMessage().equals("crapped out!"));
            assertThat("No transaction", !DbConnectionFactory.inTransaction());
            exceptionThrown = true;
        }
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
        assertThat("exception should be thrown", exceptionThrown);

        testNestedTransactionsWrap(0);
        assertThat("No transaction", !DbConnectionFactory.inTransaction());
    }


    private int testNestedTransactions(final int nest) throws Exception {
        return LocalTransaction.wrapReturn(() -> {
            boolean alreadyTransaction = DbConnectionFactory.startTransactionIfNeeded();

            assertThat("Already in transaction", !alreadyTransaction);
            if (nest > 10) {
                return getCount();
            } else {
                return testNestedTransactions(nest + 1);
            }
        });

    }


    private void testNestedTransactionsWrap(final int nest) throws Exception {
        LocalTransaction.wrap(() -> {
            boolean alreadyTransaction = DbConnectionFactory.startTransactionIfNeeded();

            assertThat("Already in transaction", !alreadyTransaction);
            if (nest < 10) {
                try {
                    testNestedTransactionsWrap(nest + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private int testNestedTransactionsFail(final int nest) throws Exception {

        return LocalTransaction.wrapReturn(() -> {
            boolean alreadyTransaction = DbConnectionFactory.startTransactionIfNeeded();

            assertThat("Already in transaction", !alreadyTransaction);
            if (nest > 5) {
                throw new DotStateException("crapped out!");
            }
            if (nest > 10) {
                return getCount();
            } else {
                return testNestedTransactionsFail(nest + 1);
            }

        });

    }


    private int getCount() {
        dc.setSQL(SQL_COUNT);
        return dc.getInt("test");
    }

    private int rollbackTransaction() throws Exception {
        int numInodes = getCount();
        try {
            return LocalTransaction.wrapReturn(() -> {

                String dupeInode = UUID.randomUUID().toString();
                assertThat("inTransaction", DbConnectionFactory.inTransaction());
                dc.setSQL(SQL_INSERT);
                dc.addParam(UUID.randomUUID().toString());
                dc.addParam(new java.util.Date());
                dc.addParam("test");
                dc.loadResult();

                assertThat("Inserts are visible in transaction", (getCount() == numInodes + 1));

                dc.setSQL(SQL_INSERT);
                dc.addParam(dupeInode);
                dc.addParam(new java.util.Date());
                dc.addParam("test");
                dc.loadResult();

                assertThat("Inserts visible in transaction", (getCount() == numInodes + 2));

                // this should fail as we are inserting a dupe inode
                dc.setSQL(SQL_INSERT);
                dc.addParam(dupeInode);
                dc.addParam(new java.util.Date());
                dc.addParam("test");
                dc.loadResult();

                return getCount();
            });
        } catch (Exception e) {
            //
            assertThat("we should be rolled back with no transaction here", !DbConnectionFactory.inTransaction());
            throw new DotDataException(e.getMessage(), e);
        } finally {
            assertThat("inTransaction", !DbConnectionFactory.inTransaction());
        }
    }


    private int testSelectInTransaction() throws Exception {

        assertThat("No transaction", !DbConnectionFactory.inTransaction());

        assertThat("No transaction", !DbConnectionFactory.inTransaction());

        try {
            return LocalTransaction.wrapReturn(() -> {
                dc.setSQL(SQL_COUNT);
                int ret = dc.getInt("test");
                assertThat("inTransaction", DbConnectionFactory.inTransaction());
                return ret;
            });
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        } finally {
            assertThat("inTransaction", !DbConnectionFactory.inTransaction());
        }
    }


}
