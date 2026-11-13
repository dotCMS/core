package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
import com.dotmarketing.util.UUIDUtil;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that {@link Task261112WidenSiteSearchAuditTextColumns} turns the {@code path},
 * {@code host_list} and {@code lang_list} columns of {@code sitesearch_audit} into {@code text},
 * that it does nothing once they are, and that an audit row with values longer than the old
 * 500-character limit can then be saved and read back unchanged (issue #36706).
 */
public class Task261112WidenSiteSearchAuditTextColumnsTest {

    private static final String[] COLUMNS = {"path", "host_list", "lang_list"};

    private String jobId;

    /**
     * Starts the dotCMS services the startup task and the audit API need.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Removes the audit rows a test wrote.
     */
    @After
    public void removeAuditRows() throws DotDataException {
        if (jobId != null) {
            APILocator.getSiteSearchAuditAPI().removeAudits(jobId);
        }
    }

    /**
     * Leaves the columns as {@code text}, the state a current schema has, even if a test failed
     * halfway through.
     */
    @AfterClass
    public static void restoreColumns() throws Exception {
        if (new Task261112WidenSiteSearchAuditTextColumns().forceRun()) {
            new Task261112WidenSiteSearchAuditTextColumns().executeUpgrade();
        }
    }

    /**
     * Given a database whose three list columns are still {@code varchar(500)}, when the task
     * runs, then all three become {@code text}, stay {@code NOT NULL}, and the task no longer
     * asks to run.
     */
    @Test
    public void test_executeUpgrade_turns_list_columns_into_text() throws Exception {
        narrowColumnsToOldSchema();
        final Task261112WidenSiteSearchAuditTextColumns task =
                new Task261112WidenSiteSearchAuditTextColumns();
        assertTrue("The task must run while the columns are varchar(500)", task.forceRun());

        task.executeUpgrade();

        for (final String column : COLUMNS) {
            final Map<String, Object> info = columnInfo(column);
            assertEquals(column + " must be text after the upgrade", "text", info.get("data_type"));
            assertEquals(column + " must stay NOT NULL", "NO", info.get("is_nullable"));
        }
        assertFalse("The task must not run again once the columns are text", task.forceRun());
    }

    /**
     * Given the upgraded columns, when an audit row is saved with a path, site list and language
     * list longer than 500 characters, then the save succeeds and the row reads back unchanged —
     * the insert that used to fail and cost the next crawl a full rebuild.
     */
    @Test
    public void test_audit_with_values_over_500_chars_is_saved_after_upgrade() throws Exception {
        narrowColumnsToOldSchema();
        new Task261112WidenSiteSearchAuditTextColumns().executeUpgrade();

        jobId = UUIDUtil.uuid();
        final String longPath = "/" + RandomStringUtils.randomAlphabetic(600);
        final String longHostList = RandomStringUtils.randomAlphabetic(700);
        final String longLangList = RandomStringUtils.randomAlphabetic(800);

        final SiteSearchAudit audit = new SiteSearchAudit();
        audit.setJobId(jobId);
        audit.setJobName("long-path-audit-test");
        audit.setFireDate(new Date());
        audit.setIncremental(false);
        audit.setStartDate(new Date());
        audit.setEndDate(new Date());
        audit.setHostList(longHostList);
        audit.setAllHosts(false);
        audit.setLangList(longLangList);
        audit.setPath(longPath);
        audit.setPathInclude(true);
        audit.setIndexName("long-path-audit-test");

        final SiteSearchAuditAPI auditAPI = APILocator.getSiteSearchAuditAPI();
        auditAPI.save(audit);

        final List<SiteSearchAudit> saved = auditAPI.findRecentAudits(jobId, 0, 1);
        assertEquals("The audit row must have been written", 1, saved.size());
        assertEquals(longPath, saved.get(0).getPath());
        assertEquals(longHostList, saved.get(0).getHostList());
        assertEquals(longLangList, saved.get(0).getLangList());
    }

    /**
     * Puts the three list columns back to the pre-fix {@code varchar(500)} definition.
     *
     * <p>The table is shared with every other test in the run, so existing rows are never cut to
     * fit: when any row already holds a value longer than 500 characters the test is skipped
     * instead, and the type change itself carries no {@code USING} cast, so Postgres would refuse
     * it rather than silently truncate.</p>
     */
    private static void narrowColumnsToOldSchema() throws SQLException {
        final StringBuilder longRows = new StringBuilder(
                "SELECT count(*) AS n FROM sitesearch_audit WHERE ");
        final StringBuilder alter = new StringBuilder("ALTER TABLE sitesearch_audit ");
        for (int i = 0; i < COLUMNS.length; i++) {
            longRows.append(i == 0 ? "" : " OR ")
                    .append("length(").append(COLUMNS[i]).append(") > 500");
            alter.append(i == 0 ? "" : ", ")
                    .append("ALTER COLUMN ").append(COLUMNS[i]).append(" TYPE varchar(500)");
        }
        assumeTrue("sitesearch_audit already holds values longer than 500 characters; narrowing "
                        + "the columns would fail, and truncating them would destroy that data",
                new DotConnect().setSQL(longRows.toString()).getInt("n") == 0);
        new DotConnect().executeStatement(alter.toString());
    }

    /**
     * Reads a {@code sitesearch_audit} column's type and nullability from the catalog.
     *
     * @param column the column name
     * @return a map with {@code data_type} and {@code is_nullable}
     */
    private static Map<String, Object> columnInfo(final String column) throws DotDataException {
        return new DotConnect()
                .setSQL("SELECT data_type, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'sitesearch_audit' AND column_name = ?")
                .addParam(column)
                .loadObjectResults()
                .get(0);
    }
}
