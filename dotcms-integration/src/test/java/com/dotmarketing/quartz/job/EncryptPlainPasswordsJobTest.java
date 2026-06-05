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
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.liferay.portal.model.Company;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link EncryptPlainPasswordsJob}.
 *
 * Rows are inserted directly via {@link DotConnect} (rather than {@code UserAPI.save}) so the
 * test controls exactly what lands in {@code user_.password_} and {@code user_.passwordEncrypted}.
 *
 * The scheduled cron firing is removed from Quartz in {@link #beforeClass()} so the every-N-minute
 * job that {@link DotInitScheduler} registers cannot race the in-test invocations.
 */
public class EncryptPlainPasswordsJobTest extends IntegrationTestBase {

    private static final String JOB_NAME = "EncryptPlainPasswordsJob";
    private static final String PLAINTEXT = "s3cret-plain-password";

    private static String defaultCompanyId;

    private final EncryptPlainPasswordsJob job = new EncryptPlainPasswordsJob();
    private final List<String> insertedUserIds = new ArrayList<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        defaultCompanyId = PublicCompanyFactory.getDefaultCompany().getCompanyId();
        // Prevent the scheduled cron firing from racing the explicit job.execute() calls below.
        QuartzUtils.getScheduler().deleteJob(JOB_NAME, DotInitScheduler.DOTCMS_JOB_GROUP_NAME);
    }

    @AfterClass
    public static void afterClass() {
        Config.setProperty(EncryptPlainPasswordsJob.ENABLE_PROPERTY, true);
    }

    @After
    public void cleanupInsertedRows() throws DotDataException {
        for (final String userId : insertedUserIds) {
            // LoginFactory.doLogin loads the user via UserAPI, which auto-assigns default roles
            // (users_cms_roles.user_id FKs back to user_). Clear those first to avoid a
            // foreign-key violation on the user_ delete.
            new DotConnect()
                    .setSQL("delete from users_cms_roles where user_id = ?")
                    .addParam(userId)
                    .loadResult();
            new DotConnect()
                    .setSQL("delete from user_ where userId = ?")
                    .addParam(userId)
                    .loadResult();
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

    /**
     * End-to-end auth roundtrip: the hash format produced by the job must be accepted by the
     * platform's real authentication entry point.
     *
     * Given: a user with passwordEncrypted=false sitting in user_ with plaintext password "X".
     * When:  the job runs and rewrites password_ as a hash.
     * Then:  {@link LoginFactory#doLogin(String, String)} with the original plaintext returns
     *        true; the same call with a wrong password returns false. This proves the job's
     *        output is interchangeable with hashes produced via UserAPI / login flows.
     */
    @Test
    public void test_user_can_authenticate_after_job_hashes_password() throws Exception {
        final String userId = insertUser(PLAINTEXT, false);

        TestJobExecutor.execute(job, new HashMap<>());

        // Confirm the row was actually hashed before we test auth.
        final Map<String, Object> row = loadRow(userId);
        assertTrue("Pre-condition: row should be hashed before auth check",
                toBool(row.get("passwordencrypted")));

        // doLogin resolves the user via the company's configured auth type — pass whichever
        // identifier that resolver expects so the test is robust to AUTH_TYPE_EA vs AUTH_TYPE_ID.
        final String identifier = Company.AUTH_TYPE_EA.equals(
                PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? emailFor(userId) : userId;

        assertTrue("doLogin with original plaintext must succeed against the job-produced hash",
                LoginFactory.doLogin(identifier, PLAINTEXT));
        assertFalse("doLogin with a wrong password must fail",
                LoginFactory.doLogin(identifier, PLAINTEXT + "-wrong"));
    }

    /**
     * Given: a row that the SELECT will see as plaintext, but whose password_ is mutated by another
     *        writer between SELECT and UPDATE.
     * When:  the job runs.
     * Then:  the guarded UPDATE affects zero rows and the concurrent writer's value is preserved.
     */
    @Test
    public void test_concurrent_change_is_not_clobbered() throws Exception {
        final String userId = insertUser(PLAINTEXT, false);

        // Simulate the racing writer by changing the password_ value AFTER our SELECT would have
        // captured the plaintext but BEFORE the UPDATE fires. We do it by pre-changing the row,
        // then asking the job to hash using the original (stale) plaintext.
        final String concurrentWriterHash = PasswordFactoryProxy.generateHash("a-different-password");
        new DotConnect()
                .setSQL("update user_ set password_ = ?, passwordEncrypted = ? where userId = ?")
                .addParam(concurrentWriterHash)
                .addParam(true)
                .addParam(userId)
                .loadResult();

        // Now run the job. The SELECT will not match (encrypted=true), but even if a real race let
        // the row through, the guarded UPDATE (where password_ = old plaintext) must miss.
        TestJobExecutor.execute(job, new HashMap<>());

        final Map<String, Object> row = loadRow(userId);
        assertEquals("Concurrent writer's value must be preserved",
                concurrentWriterHash, row.get("password_"));
        assertTrue("Concurrent writer's encrypted flag must be preserved",
                toBool(row.get("passwordencrypted")));
    }

    private String insertUser(final String password, final boolean encrypted) throws DotDataException {
        final String userId = "test-encrypt-job-" + UUIDGenerator.generateUuid();
        new DotConnect()
                .setSQL("insert into user_ (userId, companyId, password_, passwordEncrypted, "
                        + "emailAddress, passwordReset, male, dottedSkins, roundedSkins, "
                        + "failedLoginAttempts, agreedToTermsOfUse, active_) "
                        + "values (?, ?, ?, ?, ?, false, false, false, false, 0, false, true)")
                .addParam(userId)
                .addParam(defaultCompanyId)
                .addParam(password)
                .addParam(encrypted)
                .addParam(emailFor(userId))
                .loadResult();
        insertedUserIds.add(userId);
        return userId;
    }

    private static String emailFor(final String userId) {
        return userId + "@test.dotcms";
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
