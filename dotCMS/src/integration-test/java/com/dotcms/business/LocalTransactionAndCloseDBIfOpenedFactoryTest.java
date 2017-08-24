package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.test.ReadOnlyTester;
import com.dotcms.test.TransactionalTester;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.links.model.Link;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class LocalTransactionAndCloseDBIfOpenedFactoryTest extends IntegrationTestBase {

    final String SQL_COUNT = "select count(*) as test from inode";
    final String SQL_INSERT = "insert into counter(name, currentid) values (?, ?)";

    final DotConnect dc = new DotConnect();

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
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
                    Assert.assertEquals(DbConnectionFactory.getConnection().toString(), builder.toString());
                    getCount();
                    Assert.assertTrue(DbConnectionFactory.inTransaction());
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

    private int getCount() {
        dc.setSQL(SQL_COUNT);
        return dc.getInt("test");
    }

    private void update() throws DotDataException {
        dc.executeUpdate(SQL_INSERT,
                "test" + System.currentTimeMillis(), 1);
    }


}
