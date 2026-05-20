package com.dotmarketing.quartz.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link EncryptPlainPasswordsJob}.
 *
 * Rows are inserted directly via {@link DotConnect} (rather than {@code UserAPI.save}) so the
 * test controls exactly what lands in {@code user_.password_} and {@code user_.passwordEncrypted}.
 */
public class EncryptPlainPasswordsJobTest extends IntegrationTestBase {

    private static final String COMPANY_ID = "dotcms.org";
    private static final String PLAINTEXT = "s3cret-plain-password";

    private final EncryptPlainPasswordsJob job = new EncryptPlainPasswordsJob();
    private final java.util.List<String> insertedUserIds = new java.util.ArrayList<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void cleanupInsertedRows() throws DotDataException {
        for (final String userId : insertedUserIds) {
            new DotConnect().setSQL("delete from user_ where userId = ?").addParam(userId).loadResult();
        }
        insertedUserIds.clear();
        Config.setProperty(EncryptPlainPasswordsJob.ENABLE_PROPERTY, true);
    }

    /**
     * Given: a row with passwordEncrypted=false and a plaintext password.
     * When:  the job runs.
     * Then:  passwordEncrypted flips to true, password_ no longer equals the plaintext, and
     *        {@link PasswordFactoryProxy#authPassword} authenticates the original plaintext
     *        against the stored hash.
     */
    @Test
    public void test_hashes_plaintext_row() throws Exception {
        final String userId = insertUser(PLAINTEXT, false);

        TestJobExecutor.execute(job, new HashMap<>());

        final Map<String, Object> row = loadRow(userId);
        assertTrue("passwordEncrypted should be true after job runs",
                toBool(row.get("passwordencrypted")));
        final String storedPassword = (String) row.get("password_");
        assertNotEquals("password_ should no longer be the plaintext",
                PLAINTEXT, storedPassword);
        assertEquals("Stored hash must authenticate the original plaintext",
                AuthenticationStatus.AUTHENTICATED,
                PasswordFactoryProxy.authPassword(PLAINTEXT, storedPassword));
    }

    /**
     * Given: a row that is already passwordEncrypted=true.
     * When:  the job runs.
     * Then:  the row is untouched (password_ and flag are unchanged).
     */
    @Test
    public void test_leaves_already_encrypted_row_alone() throws Exception {
        final String alreadyHashed = PasswordFactoryProxy.generateHash(PLAINTEXT);
        final String userId = insertUser(alreadyHashed, true);

        TestJobExecutor.execute(job, new HashMap<>());

        final Map<String, Object> row = loadRow(userId);
        assertTrue("passwordEncrypted should still be true", toBool(row.get("passwordencrypted")));
        assertEquals("password_ should be unchanged",
                alreadyHashed, row.get("password_"));
    }

    /**
     * Given: a row with passwordEncrypted=false and a null password_.
     * When:  the job runs.
     * Then:  the row is skipped — passwordEncrypted stays false (nothing to hash).
     */
    @Test
    public void test_skips_row_with_null_password() throws Exception {
        final String userId = insertUser(null, false);

        TestJobExecutor.execute(job, new HashMap<>());

        final Map<String, Object> row = loadRow(userId);
        assertFalse("Row with null password_ must not be marked encrypted",
                toBool(row.get("passwordencrypted")));
        assertNull("password_ must remain null", row.get("password_"));
    }

    /**
     * Given: multiple plaintext rows.
     * When:  the job runs once.
     * Then:  every row is hashed in the single pass.
     */
    @Test
    public void test_hashes_multiple_rows_in_one_pass() throws Exception {
        final String userIdA = insertUser(PLAINTEXT + "-a", false);
        final String userIdB = insertUser(PLAINTEXT + "-b", false);
        final String userIdC = insertUser(PLAINTEXT + "-c", false);

        TestJobExecutor.execute(job, new HashMap<>());

        for (final String userId : List.of(userIdA, userIdB, userIdC)) {
            final Map<String, Object> row = loadRow(userId);
            assertTrue("All rows should be flipped to encrypted",
                    toBool(row.get("passwordencrypted")));
        }
    }

    /**
     * Given: the kill-switch property is set to false.
     * When:  the job runs.
     * Then:  the plaintext row is left alone (no-op at runtime).
     */
    @Test
    public void test_disabled_flag_makes_job_noop() throws Exception {
        final String userId = insertUser(PLAINTEXT, false);
        Config.setProperty(EncryptPlainPasswordsJob.ENABLE_PROPERTY, false);

        TestJobExecutor.execute(job, new HashMap<>());

        final Map<String, Object> row = loadRow(userId);
        assertFalse("Disabled job must not encrypt the row",
                toBool(row.get("passwordencrypted")));
        assertEquals("Disabled job must not rewrite password_",
                PLAINTEXT, row.get("password_"));
    }

    private String insertUser(final String password, final boolean encrypted) throws DotDataException {
        final String userId = "test-encrypt-job-" + UUIDGenerator.generateUuid();
        new DotConnect()
                .setSQL("insert into user_ (userId, companyId, password_, passwordEncrypted, " +
                        "passwordReset, male, dottedSkins, roundedSkins, failedLoginAttempts, " +
                        "agreedToTermsOfUse, active_) " +
                        "values (?, ?, ?, ?, false, false, false, false, 0, false, true)")
                .addParam(userId)
                .addParam(COMPANY_ID)
                .addParam(password)
                .addParam(encrypted)
                .loadResult();
        insertedUserIds.add(userId);
        return userId;
    }

    private Map<String, Object> loadRow(final String userId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select password_, passwordEncrypted from user_ where userId = ?")
                .addParam(userId)
                .loadObjectResults();
        assertEquals("Expected exactly one row for userId=" + userId, 1, rows.size());
        return rows.get(0);
    }

    private static boolean toBool(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
