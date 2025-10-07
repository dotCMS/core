package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.bytebuddy.ExternalTransactionAdvice;
import com.dotcms.test.ExternalTransactionalTester;
import com.dotcms.test.ReadOnlyTester;
import com.dotcms.test.TransactionalTester;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.ReturnableDelegate;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Logger;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class LocalTransactionAndCloseDBIfOpenedFactoryTest extends IntegrationTestBase {

    final String SQL_COUNT = "select count(*) as test from inode";
    final String SQL_COUNT_COUNTER = "select count(*) as test from counter";
    final String SQL_INSERT = "insert into counter(name, currentid) values (?, ?)";

    final DotConnect dc = new DotConnect();

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    @Ignore("This test runs locally but fails in cloud")
    public void testUpdateSelectTransaction() throws Exception {

        final TransactionalTester tx1        = new TransactionalTester();
        final ReadOnlyTester readOnlyTester1 = new ReadOnlyTester();
        final TransactionalTester tx2        = new TransactionalTester();
        final ReadOnlyTester readOnlyTester2 = new ReadOnlyTester();
        final StringBuilder builder          = new StringBuilder();

        DbConnectionFactory.closeSilently(); // make sure any previous conn is already closed before start

        tx1.test(() -> {

            builder.append(DbConnectionFactory.getConnection().toString());
            update();
            Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

            readOnlyTester1.test( () -> {
                // the con will be already created by the aspect.
                Assert.assertTrue(DbConnectionFactory.connectionExists());
                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                getCount();
                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                final HibernateUtil hibernateUtil = new HibernateUtil(Link.class);
                Link l = new Link();
                String tableName = l.getType();

                final String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
                        + tableName + "_1_ where tree.child = " + tableName + ".inode and " + tableName
                        + "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type ='"+tableName+"'";
                hibernateUtil.setSQLQuery(sql);

                tx2.test(() -> {

                    Assert.assertTrue(DbConnectionFactory.inTransaction());
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                    update();
                    Assert.assertTrue(DbConnectionFactory.inTransaction());
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

                    readOnlyTester2.test(() -> {

                        Assert.assertTrue(DbConnectionFactory.inTransaction());
                        Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                        getCount();
                        Assert.assertTrue(DbConnectionFactory.inTransaction());
                        Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

                    });
                });

                try {
                    hibernateUtil.list();
                } catch (Exception e) {
                    Assert.fail("Hibernate wired connection still works");
                }

            } );

            Assert.assertTrue(DbConnectionFactory.inTransaction());
            Assert.assertTrue(DbConnectionFactory.connectionExists());

        });

        Assert.assertFalse(DbConnectionFactory.inTransaction());
        Assert.assertFalse(DbConnectionFactory.connectionExists());
    }

    @Test
    @Ignore("This test runs locally but fails in cloud")
    public void testSelectUpdateTransaction() throws Exception {

        final TransactionalTester tx1        = new TransactionalTester();
        final ReadOnlyTester readOnlyTester1 = new ReadOnlyTester();
        final TransactionalTester tx2        = new TransactionalTester();
        final ReadOnlyTester readOnlyTester2 = new ReadOnlyTester();
        final StringBuilder builder          = new StringBuilder();

        DbConnectionFactory.closeSilently(); // make sure any previous conn is already closed before start

        readOnlyTester1.test(() -> {

            builder.append(DbConnectionFactory.getConnection().toString());
            getCount();
            Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

            tx1.test( () -> {
                // the con will be already created by the aspect.
                Assert.assertTrue(DbConnectionFactory.connectionExists());
                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                update();
                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                final HibernateUtil hibernateUtil = new HibernateUtil(Link.class);
                Link l = new Link();
                String tableName = l.getType();

                final String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
                        + tableName + "_1_ where tree.child = " + tableName + ".inode and " + tableName
                        + "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type ='"+tableName+"'";
                hibernateUtil.setSQLQuery(sql);

                readOnlyTester2.test(() -> {

                    Assert.assertTrue(DbConnectionFactory.inTransaction());
                    Assert.assertTrue(DbConnectionFactory.connectionExists());
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                    getCount();
                    Assert.assertTrue(DbConnectionFactory.inTransaction());
                    Assert.assertTrue(DbConnectionFactory.connectionExists());
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

                    tx2.test(() -> {

                        Assert.assertTrue(DbConnectionFactory.inTransaction());
                        Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                        update();
                        Assert.assertTrue(DbConnectionFactory.inTransaction());
                        Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

                    });
                });

                try {
                    hibernateUtil.list();
                } catch (Exception e) {
                    Assert.fail("Hibernate wired connection still works");
                }

            } );

            Assert.assertFalse(DbConnectionFactory.inTransaction());
            Assert.assertTrue(DbConnectionFactory.connectionExists());
        });

        Assert.assertFalse(DbConnectionFactory.inTransaction());
        Assert.assertFalse(DbConnectionFactory.connectionExists());

    }

    @Test
    @Ignore("This test runs locally but fails in cloud")
    public void testSingleSelectTransaction() throws Exception {

        final ReadOnlyTester readOnlyTester1 = new ReadOnlyTester();
        final StringBuilder builder          = new StringBuilder();

        DbConnectionFactory.closeSilently(); // make sure any previous conn is already closed before start

        readOnlyTester1.test(() -> {

            builder.append(DbConnectionFactory.getConnection().toString());
            getCount();
            Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());



            Assert.assertFalse(DbConnectionFactory.inTransaction());
            Assert.assertTrue(DbConnectionFactory.connectionExists());
        });

        Assert.assertFalse(DbConnectionFactory.inTransaction());
        Assert.assertFalse(DbConnectionFactory.connectionExists());
    }

    @Test
    @Ignore("This test runs locally but fails in cloud")
    public void testSingleSelectUpdateTransaction() throws Exception {

        final ReadOnlyTester readOnlyTester1 = new ReadOnlyTester();
        final TransactionalTester tx1        = new TransactionalTester();
        final StringBuilder builder          = new StringBuilder();

        DbConnectionFactory.closeSilently(); // make sure any previous conn is already closed before start

        readOnlyTester1.test(() -> {

            Assert.assertFalse(DbConnectionFactory.connectionExists());
            builder.append(DbConnectionFactory.getConnection().toString());
            getCount();
            Assert.assertFalse(DbConnectionFactory.inTransaction());
            Assert.assertTrue(DbConnectionFactory.connectionExists());
            Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());

            tx1.test( () -> {
                // the con will be already created by the aspect.
                Assert.assertTrue(DbConnectionFactory.connectionExists());
                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                update();
                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                final HibernateUtil hibernateUtil = new HibernateUtil(Link.class);
                Link l = new Link();
                String tableName = l.getType();

                final String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
                        + tableName + "_1_ where tree.child = " + tableName + ".inode and " + tableName
                        + "_1_.inode = " + tableName + ".inode and " + tableName + "_1_.type ='" + tableName + "'";
                hibernateUtil.setSQLQuery(sql);
                try {
                    hibernateUtil.list();
                } catch (Exception e) {
                    Assert.fail("Hibernate wired connection still works");
                }

                Assert.assertTrue(DbConnectionFactory.inTransaction());
                Assert.assertTrue(DbConnectionFactory.connectionExists());
            });

            Assert.assertFalse(DbConnectionFactory.inTransaction());
            Assert.assertTrue(DbConnectionFactory.connectionExists());
        });

        Assert.assertFalse(DbConnectionFactory.inTransaction());
        Assert.assertFalse(DbConnectionFactory.connectionExists());
    }

    private int getCount() {
        dc.setSQL(SQL_COUNT);
        return dc.getInt("test");
    }

    private int getCountCounter() {
        dc.setSQL(SQL_COUNT_COUNTER);
        return dc.getInt("test");
    }

    private void update() throws DotDataException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Logger.error(this, "Not able to sleep", e);
        }
        dc.executeUpdate(SQL_INSERT,
                "test" + System.currentTimeMillis(), 1);
    }


    /**
     * Method to test: {@link com.dotmarketing.db.LocalTransaction#externalizeTransaction(ReturnableDelegate)}
     * Given Scenario: 1) Run an normal read operation, 2) inside #1 run a wrap in trans [this trans will fail at the end, so rollback] 3) inside #2 run an external trans
     * ExpectedResult: the first transaction makes 2 updates to count (the wrap) both will be rollback since the transaction will fail at the end.
     * the second transaction is mark as an external, so his 2 update to count should be ok, even if the parent caller fails and become to rollback
     *
     * So the count should be at the end + 2 than initial count.
     *
     */
    @Test
    @Ignore("need to add back in external transaction option")
    public void testUpdateExternalTransactionSuccess_Even_If_Current_Transaction_Fails() throws Exception {

        final ReadOnlyTester readOnlyTester1 = new ReadOnlyTester();
        final ExternalTransactionalTester externalTransactionalTester = new ExternalTransactionalTester();
        final TransactionalTester tx1        = new TransactionalTester();
        final StringBuilder builder          = new StringBuilder();
        final MutableInt   countInitial      = new MutableInt(0);
        final MutableInt   countExternal     = new MutableInt(0);

        DbConnectionFactory.closeSilently(); // make sure any previous conn is already closed before start

        readOnlyTester1.test(() -> {

            Assert.assertFalse(DbConnectionFactory.connectionExists());
            builder.append(DbConnectionFactory.getConnection().toString());
            countInitial.setValue(getCountCounter());
            Assert.assertFalse(DbConnectionFactory.inTransaction());
            Assert.assertTrue(DbConnectionFactory.connectionExists());
            Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
            try {
                tx1.test(() -> {
                    // the con will be already created by the aspect.
                    // this transaction will fail
                    Assert.assertTrue(DbConnectionFactory.connectionExists());
                    Assert.assertTrue(DbConnectionFactory.inTransaction());
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                    Assert.assertTrue(DbConnectionFactory.inTransaction());
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                    externalTransactionalTester.test(() -> {

                        update(); // even that the wrap trans will fail, this one is an isolated transaction so should work.
                        update();  // we call twice
                    });

                    countExternal.setValue(getCountCounter());
                    update();  // this will be rollback
                    update();  // this will be rollback
                    update();  // this will be rollback
                    update();  // this will be rollback
                    throw new DotRuntimeException("Fail");
                });
            } catch (Exception e) {

                // Ok
            }
        });

        int currentCount = getCountCounter();
        DbConnectionFactory.closeSilently();

        Assert.assertTrue(countExternal.intValue() > countInitial.intValue());
        // just 2 additional counts instead of 1, b/c 2 of the four counts made will be rollback
        Assert.assertEquals(countInitial.intValue() + 2,  countExternal.intValue());
        Assert.assertEquals(currentCount,  countExternal.intValue());
    }

    /**
     * Method to test: ExternalTransaction annotation
     * Given Scenario: Just annotated an method called inside a transaction
     * ExpectedResult: The method annotated by external should has a different connection than the one used by the transaction
     *
     */
    @Test
    public void test_external_transaction_advice() throws Throwable {

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();
        try {

            DbConnectionFactory.startTransactionIfNeeded();

            final String originalConn = DbConnectionFactory.getConnection().toString();

            testExternalTransactionAnnotation(originalConn);
        } finally {
            if (isNewConnection) {
                DbConnectionFactory.closeSilently();
            }
        }
    } // test

    // have to test in this way b/c the ExternalTransaction annotation does not work on
    private void testExternalTransactionAnnotation (final String originalConn) throws Throwable {

        final ExternalTransactionAdvice externalTransactionAdvice = new ExternalTransactionAdvice();
        ExternalTransactionAdvice.TransactionInfo transactionInfo = null;

        try {

            transactionInfo = externalTransactionAdvice.enter("testExternalTransactionAnnotation");
            String newConn = DbConnectionFactory.getConnection().toString();

            Assert.assertNotEquals("The outside conn should be diff to the inside conn on external transaction", originalConn, newConn);
        } finally {

            externalTransactionAdvice.exit(transactionInfo,  null);
        }

    }

}
