package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.startup.runalways.Task00001LoadSchema;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Guards the contract of {@link DotCMSInitDb#INODE_EXISTS_SQL}, the existence probe that replaced
 * {@code select count(*) as test from inode} in the three run-always startup checks.
 * <p>
 * The probe is only a safe substitute for {@code count(*)} if it holds three properties at once,
 * and the two that guard first-boot cannot be exercised against the real {@code inode} table
 * because the integration database is always populated. They are covered here with a scratch table
 * and a deliberately absent one:
 * <ul>
 *     <li>populated table -&gt; exactly one row, value {@code 1};</li>
 *     <li>empty table -&gt; exactly one row, value {@code 0}, resolvable through
 *     {@link DotConnect#getInt(String)} <b>without throwing</b> (a bare
 *     {@code select 1 ... limit 1} would return zero rows here and blow up
 *     {@code Task00004LoadStarter.forceRun()} / {@code DotCMSInitDb.isConfigured()} on a fresh
 *     database);</li>
 *     <li>missing table -&gt; {@link SQLException}, which is the signal
 *     {@link Task00001LoadSchema#forceRun()} uses to decide the schema must be loaded.</li>
 * </ul>
 *
 * @author dotCMS
 */
public class InodeExistenceCheckIntegrationTest {


    private static final String INODE_TABLE = "inode";
    private static final String EMPTY_TABLE = "dotcms_36865_empty_probe";
    private static final String MISSING_TABLE = "dotcms_36865_absent_probe";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Builds the production probe for an arbitrary table. The table names fed to this method are
     * all {@code private static final} constants declared above -- no external input reaches it.
     *
     * @param table The table to probe.
     *
     * @return The existence-probe SQL statement for the given table.
     */
    private static String existsSqlFor(final String table) {
        return "select case when exists (select 1 from " + table + ") then 1 else 0 end as test";
    }

    /**
     * Runs a statement on a connection borrowed straight from the pool, so DDL never rides on the
     * ThreadLocal connection the surrounding test may already have a transaction open on.
     *
     * @param sql The statement to execute.
     */
    private static void executeOnPooledConnection(final String sql) throws SQLException {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        }
    }

    /**
     * Method to test: {@link DotCMSInitDb#INODE_EXISTS_SQL}
     * Given Scenario: The constant is compared against the shape this test parameterises for its
     * scratch tables.
     * Expected Result: They are identical, so every other assertion here is genuinely exercising
     * the statement that ships.
     */
    @Test
    public void test_probeSqlShape_matchesProductionConstant() {
        assertEquals("The test builds a different query shape than the one used in production; "
                        + "update existsSqlFor() to match DotCMSInitDb.INODE_EXISTS_SQL.",
                DotCMSInitDb.INODE_EXISTS_SQL, existsSqlFor(INODE_TABLE));
    }

    /**
     * Method to test: {@link DotCMSInitDb#INODE_EXISTS_SQL}
     * Given Scenario: The probe runs against the populated {@code inode} table of a started-up
     * dotCMS.
     * Expected Result: {@link DotConnect#getInt(String)} returns 1, keeping the {@code > 0} and
     * {@code < 1} comparisons at the call sites behaving exactly as they did with {@code count(*)}.
     */
    @Test
    public void test_probe_returnsOne_whenTableHasRows() {
        final DotConnect db = new DotConnect();
        db.setSQL(DotCMSInitDb.INODE_EXISTS_SQL);

        assertEquals(1, db.getInt("test"));
    }

    /**
     * Method to test: {@link DotCMSInitDb#INODE_EXISTS_SQL}
     * Given Scenario: The probe runs against an existing but completely empty table -- the shape of
     * a database whose schema has been loaded but whose starter has not been imported yet.
     * Expected Result: Exactly one row is returned and {@link DotConnect#getInt(String)} yields 0
     * without throwing. This is the regression guard for first-boot: a zero-row query would make
     * {@code getInt} throw {@code DotRuntimeException} instead.
     */
    @Test
    public void test_probe_returnsZeroAndDoesNotThrow_whenTableIsEmpty() throws Exception {
        executeOnPooledConnection("create table if not exists " + EMPTY_TABLE + " (id int)");
        try {
            final DotConnect db = new DotConnect();
            db.setSQL(existsSqlFor(EMPTY_TABLE));

            assertEquals("The probe must always return exactly one row, even against an empty "
                    + "table, or DotConnect.getInt() throws on a fresh database.",
                    1, db.loadObjectResults().size());
            assertEquals(0, db.getInt("test"));
        } finally {
            executeOnPooledConnection("drop table if exists " + EMPTY_TABLE);
        }
    }

    /**
     * Method to test: {@link DotCMSInitDb#INODE_EXISTS_SQL}
     * Given Scenario: The probe runs against a table that does not exist at all -- the shape of a
     * brand new, unschema'd database.
     * Expected Result: A {@link SQLException} is raised. {@link Task00001LoadSchema#forceRun()}
     * treats that exception, not a row value, as the signal to load the schema, so the probe must
     * keep failing this way.
     */
    @Test
    public void test_probe_throwsSqlException_whenTableIsMissing() throws Exception {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(existsSqlFor(MISSING_TABLE))) {
            fail("Expected a SQLException probing the missing table '" + MISSING_TABLE
                    + "' -- Task00001LoadSchema.forceRun() depends on that exception.");
        } catch (final SQLException expected) {
            // Expected: this is the "empty dotCMS database" signal.
        }
    }

    /**
     * Method to test: {@link Task00001LoadSchema#forceRun()}
     * Given Scenario: A populated database whose schema is already loaded.
     * Expected Result: false -- the schema must not be re-created.
     */
    @Test
    public void test_task00001ForceRun_isFalse_onPopulatedDatabase() {
        assertFalse("Task00001LoadSchema would re-run the schema load against a populated database.",
                new Task00001LoadSchema().forceRun());
    }

    /**
     * Method to test: {@link Task00004LoadStarter#forceRun()}
     * Given Scenario: A populated database that already has inodes.
     * Expected Result: false -- the starter import must not run again.
     */
    @Test
    public void test_task00004ForceRun_isFalse_onPopulatedDatabase() {
        assertFalse("Task00004LoadStarter would re-import the starter against a populated database.",
                new Task00004LoadStarter().forceRun());
    }

}
