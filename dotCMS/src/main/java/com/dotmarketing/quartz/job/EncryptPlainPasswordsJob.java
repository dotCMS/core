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
 * By default this job is scheduled to run every minute. It is stateful so concurrent firings
 * cannot overlap.
 */
public class EncryptPlainPasswordsJob implements StatefulJob {

    public static final String ENABLE_PROPERTY = "ENABLE_ENCRYPT_PLAIN_PASSWORDS_JOB";
    public static final String CRON_PROPERTY = "ENCRYPT_PLAIN_PASSWORDS_CRON_EXPRESSION";
    public static final String DEFAULT_CRON_EXPRESSION = "0 0/1 * * * ?";

    private static final String SELECT_PLAINTEXT_USERS_SQL =
            "select userId, password_ from user_ where passwordEncrypted = ? and password_ is not null";

    private static final String UPDATE_HASHED_PASSWORD_SQL =
            "update user_ set password_ = ?, passwordEncrypted = ? where userId = ?";

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
            Logger.info(this.getClass(),
                    "Shutdown in progress - skipping EncryptPlainPasswordsJob execution");
            return;
        }

        if (!isEnabled()) {
            Logger.debug(this.getClass(),
                    () -> ENABLE_PROPERTY + "=false - skipping EncryptPlainPasswordsJob execution");
            return;
        }

        try {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL(SELECT_PLAINTEXT_USERS_SQL)
                    .addParam(false)
                    .loadObjectResults();

            if (rows.isEmpty()) {
                return;
            }

            Logger.info(EncryptPlainPasswordsJob.class,
                    "Found " + rows.size() + " user(s) with unencrypted passwords. Hashing now.");

            int hashed = 0;
            for (final Map<String, Object> row : rows) {
                final String userId = (String) row.get("userid");
                final String plaintext = (String) row.get("password_");

                if (!UtilMethods.isSet(userId) || !UtilMethods.isSet(plaintext)) {
                    continue;
                }

                try {
                    final String hash = PasswordFactoryProxy.generateHash(plaintext);
                    new DotConnect()
                            .setSQL(UPDATE_HASHED_PASSWORD_SQL)
                            .addParam(hash)
                            .addParam(true)
                            .addParam(userId)
                            .loadResult();
                    hashed++;
                } catch (PasswordException | DotDataException e) {
                    Logger.error(EncryptPlainPasswordsJob.class,
                            "Unable to hash password for userId=" + userId + ": " + e.getMessage(),
                            e);
                }
            }

            Logger.info(EncryptPlainPasswordsJob.class,
                    "Encrypted " + hashed + " of " + rows.size() + " plaintext password row(s).");
        } catch (DotDataException e) {
            Logger.error(EncryptPlainPasswordsJob.class,
                    "Error scanning user_ table for unencrypted passwords", e);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }
}
