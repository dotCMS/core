package com.dotmarketing.startup.runonce;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task260313SyncHostIdentifierAssetName} Upgrade Task runs as expected.
 *
 * <p>Tests cover:
 * <ul>
 *     <li>Normal execution: out-of-sync {@code identifier.asset_name} records are corrected.</li>
 *     <li>Idempotency: running the task twice does not throw and leaves data consistent.</li>
 *     <li>System Host exclusion: the sentinel record is never modified by the task.</li>
 *     <li>forceRun() detection: returns {@code true} only when at least one record is out-of-sync.</li>
 * </ul>
 *
 * @author dotCMS
 * @since Mar 13th, 2026
 */
public class Task260313SyncHostIdentifierAssetNameTest {

    private static Host testHost;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        // Create a test site so we have a real host record to work with.
        testHost = new SiteDataGen().nextPersisted();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testHost != null) {
            try {
                APILocator.getHostAPI().archive(testHost, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(testHost, APILocator.systemUser(), false);
            } catch (final Exception e) {
                // best-effort cleanup
            }
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *         {@link Task260313SyncHostIdentifierAssetName#buildHostnameExpression()}</li>
     *     <li><b>Given Scenario:</b> A fully initialised dotCMS environment with the Host content
     *     type and its {@code hostName} field present.</li>
     *     <li><b>Expected Result:</b> {@code buildHostnameExpression()} returns a non-null, non-empty
     *     SQL expression that contains {@code hostName} or the legacy column alias (e.g.
     *     {@code c.text1}).</li>
     * </ul>
     */
    @Test
    public void buildHostnameExpressionReturnsNonNull() {
        final Task260313SyncHostIdentifierAssetName task =
                new Task260313SyncHostIdentifierAssetName();

        final String expr = task.buildHostnameExpression();

        assertNotNull("buildHostnameExpression() must return a non-null value in a valid env",
                expr);
        assertTrue("expression must not be blank", expr.trim().length() > 0);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *         {@link Task260313SyncHostIdentifierAssetName#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b> The {@code identifier.asset_name} for the test host is
     *     deliberately set to a value that differs from its actual hostname.</li>
     *     <li><b>Expected Result:</b>
     *         <ol>
     *             <li>{@code forceRun()} returns {@code true} after the corruption.</li>
     *             <li>{@code executeUpgrade()} runs without throwing.</li>
     *             <li>{@code forceRun()} returns {@code false} after the upgrade.</li>
     *             <li>The {@code identifier.asset_name} in the database equals the hostname.</li>
     *         </ol>
     *     </li>
     * </ul>
     */
    @Test
    public void upgradeTaskFixesOutOfSyncAssetName() throws Exception {
        final String identifierId = testHost.getIdentifier();
        final String actualHostname = testHost.getHostname();

        // Deliberately corrupt asset_name so the task has something to fix.
        final String corruptedName = "corrupted-asset-name-" + System.currentTimeMillis();
        corruptAssetName(identifierId, corruptedName);

        final Task260313SyncHostIdentifierAssetName task =
                new Task260313SyncHostIdentifierAssetName();

        assertTrue("forceRun() must return true when asset_name is out-of-sync",
                task.forceRun());

        task.executeUpgrade();

        assertFalse("forceRun() must return false after asset_name has been synced",
                task.forceRun());

        // Confirm the column really was updated in the database.
        final String storedName = readAssetName(identifierId);
        assertEquals(
                "identifier.asset_name must equal the hostname after the upgrade task",
                actualHostname, storedName);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *         {@link Task260313SyncHostIdentifierAssetName#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b> The upgrade task is executed twice on an already-synced
     *     database.</li>
     *     <li><b>Expected Result:</b> No exception is thrown on the second invocation and
     *     {@code forceRun()} remains {@code false}.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskIsIdempotent() throws Exception {
        // Ensure data is in sync first.
        final Task260313SyncHostIdentifierAssetName task =
                new Task260313SyncHostIdentifierAssetName();
        task.executeUpgrade();

        // Run a second time — must not throw.
        task.executeUpgrade();

        assertFalse("forceRun() must return false after running twice", task.forceRun());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *         {@link Task260313SyncHostIdentifierAssetName#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b> The System Host sentinel record exists in the identifier
     *     table (it always does in a live environment).</li>
     *     <li><b>Expected Result:</b> The task never modifies the System Host row, regardless of
     *     what {@code asset_name} it currently holds.</li>
     * </ul>
     */
    @Test
    public void systemHostIsNotModifiedByTask() throws Exception {
        // Record the current asset_name of the System Host before the task runs.
        final String systemHostId = Host.SYSTEM_HOST;
        final String beforeName = readAssetName(systemHostId);

        // Corrupt another host to trigger the task.
        corruptAssetName(testHost.getIdentifier(),
                "temp-corrupted-" + System.currentTimeMillis());

        final Task260313SyncHostIdentifierAssetName task =
                new Task260313SyncHostIdentifierAssetName();
        task.executeUpgrade();

        final String afterName = readAssetName(systemHostId);

        assertEquals("The System Host asset_name must not be changed by the upgrade task",
                beforeName, afterName);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Directly updates {@code identifier.asset_name} to a fake value for the given identifier,
     * bypassing any API caches so the task sees a corrupt state.
     */
    private static void corruptAssetName(final String identifierId,
            final String corruptedName) throws Exception {
        new DotConnect()
                .setSQL("UPDATE identifier SET asset_name = ? WHERE id = ?")
                .addParam(corruptedName)
                .addParam(identifierId)
                .loadResult();
    }

    /**
     * Reads the current {@code asset_name} from the {@code identifier} table for the given ID.
     *
     * @return the stored value, or {@code null} if the row does not exist.
     */
    private static String readAssetName(final String identifierId) throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT asset_name FROM identifier WHERE id = ?")
                .addParam(identifierId)
                .loadObjectResults();
        if (rows.isEmpty()) {
            return null;
        }
        final Object val = rows.get(0).get("asset_name");
        return val == null ? null : val.toString();
    }
}
