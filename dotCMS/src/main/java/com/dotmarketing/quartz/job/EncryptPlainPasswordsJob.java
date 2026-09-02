package com.dotmarketing.quartz.job;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * Sweeps the {@code user_} table for rows whose {@code passwordEncrypted} flag is {@code false}
 * (plaintext passwords) and rewrites them with a securely hashed value via
 * {@link PasswordFactoryProxy#generateHash(String)}. Once hashed, the row is flipped to
 * {@code passwordEncrypted = true}.
 *
 * Implementation notes:
 * <ul>
 *   <li>Stateful job — concurrent firings cannot overlap.</li>
 *   <li>The UPDATE is guarded by {@code passwordEncrypted = false AND password_ = ?} so that a
 *       concurrent password change (via {@code UserAPI}) cannot be silently regressed: if the row
 *       was already rewritten between the SELECT and the UPDATE, the UPDATE affects zero rows
 *       and is logged at debug.</li>
 *   <li>Each firing processes at most {@code ENCRYPT_PLAIN_PASSWORDS_BATCH_SIZE} rows so a bulk
 *       import depositing tens of thousands of plaintext rows cannot pin a Quartz worker thread
 *       for hours. Remaining rows are caught on subsequent ticks.</li>
 * </ul>
 */
public class EncryptPlainPasswordsJob implements StatefulJob {

    public static final String ENABLE_PROPERTY = "ENABLE_ENCRYPT_PLAIN_PASSWORDS_JOB";
    public static final String CRON_PROPERTY = "ENCRYPT_PLAIN_PASSWORDS_CRON_EXPRESSION";
    public static final String BATCH_SIZE_PROPERTY = "ENCRYPT_PLAIN_PASSWORDS_BATCH_SIZE";
    public static final String DEFAULT_CRON_EXPRESSION = "0 0/5 * * * ?";
    public static final int DEFAULT_BATCH_SIZE = 500;

    private static final String SELECT_PLAINTEXT_USERS_SQL =
            "select userId, password_ from user_ where passwordEncrypted = ? and password_ is not null limit ?";

    private static final String UPDATE_HASHED_PASSWORD_SQL =
            "update user_ set password_ = ?, passwordEncrypted = ? "
                    + "where userId = ? and passwordEncrypted = ? and password_ = ?";

    /**
     * Runtime kill switch. The scheduler also checks this property at startup; checking it here
     * lets an operator disable the job between firings without restarting the JVM.
     */
    public static boolean isEnabled() {
        return Config.getBooleanProperty(ENABLE_PROPERTY, true);
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
            Logger.info(EncryptPlainPasswordsJob.class,
                    "Shutdown in progress - skipping EncryptPlainPasswordsJob execution");
            return;
        }

        if (!isEnabled()) {
            Logger.debug(EncryptPlainPasswordsJob.class,
                    () -> ENABLE_PROPERTY + "=false - skipping EncryptPlainPasswordsJob execution");
            return;
        }

        final int batchSize = Config.getIntProperty(BATCH_SIZE_PROPERTY, DEFAULT_BATCH_SIZE);

        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL(SELECT_PLAINTEXT_USERS_SQL)
                    .addParam(false)
                    .addParam(batchSize)
                    .loadObjectResults();

            if (rows.isEmpty()) {
                return;
            }

            Logger.info(EncryptPlainPasswordsJob.class,
                    "Found " + rows.size() + " user(s) with unencrypted passwords. Hashing now.");

            int hashed = 0;
            int skipped = 0;
            for (final Map<String, Object> row : rows) {
                final String userId = (String) row.get("userid");
                final String plaintext = (String) row.get("password_");

                if (!UtilMethods.isSet(userId) || !UtilMethods.isSet(plaintext)) {
                    continue;
                }

                try {
                    final String hash = PasswordFactoryProxy.generateHash(plaintext);
                    final int updated = new DotConnect()
                            .executeUpdate(UPDATE_HASHED_PASSWORD_SQL,
                                    hash, true, userId, false, plaintext);
                    if (updated == 0) {
                        // Row changed between SELECT and UPDATE — another writer got there first.
                        skipped++;
                        Logger.debug(EncryptPlainPasswordsJob.class,
                                () -> "Skipped userId=" + userId
                                        + " — row was modified between select and update");
                    } else {
                        hashed++;
                    }
                } catch (PasswordException | DotDataException e) {
                    Logger.error(EncryptPlainPasswordsJob.class,
                            "Unable to hash password for userId=" + userId + ": " + e.getMessage(),
                            e);
                }
            }

            Logger.info(EncryptPlainPasswordsJob.class,
                    "Encrypted " + hashed + " of " + rows.size()
                            + " plaintext password row(s) (skipped " + skipped + ").");
        } catch (DotDataException e) {
            Logger.error(EncryptPlainPasswordsJob.class,
                    "Error scanning user_ table for unencrypted passwords", e);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }
}
