package com.dotmarketing.startup.runalways;

import static com.dotcms.security.apps.AppsUtil.APPS_IMPORT_EXPORT_DEFAULT_PASSWORD;
import static com.dotcms.security.apps.AppsUtil.generateKey;
import static com.dotcms.security.apps.AppsUtil.importSecrets;
import static com.dotcms.security.apps.AppsUtil.internalKey;
import static com.dotcms.security.apps.AppsUtil.loadPass;
import static com.dotcms.security.apps.AppsUtil.mapForValidation;
import static com.dotcms.security.apps.AppsUtil.toJsonAsChars;
import static com.dotcms.security.apps.AppsUtil.validateForSave;
import static com.dotmarketing.util.UtilMethods.isSet;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppDescriptorHelper;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPIImpl;
import com.dotcms.security.apps.SecretsKeyStoreHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.startup.runonce.Task04355SystemEventAddServerIdColumn;
import com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumn;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Task00050LoadAppsSecrets implements StartupTask {

    private static final Pattern importFilePattern = Pattern
            .compile("^dotSecrets-import\\.([a-zA-Z0-9-_]+)", Pattern.CASE_INSENSITIVE);

    private static final String SITE_LOOKUP_SQL =
            "select count(*) as x from contentlet_version_info clvi, contentlet c, structure s  \n"
                    + " where c.structure_inode = s.inode and  s.name = 'Host' and clvi.working_inode = c.inode and c.identifier = ? ";

    static boolean matchesFileName(final String input) {
        final Matcher matcher = importFilePattern.matcher(input);
        return matcher.find();
    }

    final SecretsKeyStoreHelper keyStoreHelper = new SecretsKeyStoreHelper();

    final AppDescriptorHelper appDescriptorHelper = new AppDescriptorHelper();

    private Map<String, AppDescriptor> getAppDescriptors() throws DotDataException {
        try {
            return appDescriptorHelper.loadAppDescriptors().stream().collect(Collectors
                    .toMap(appDescriptor -> appDescriptor.getKey().toLowerCase(),
                            Function.identity()));
        } catch (URISyntaxException | IOException e) {
            throw new DotDataException(e);
        }
    }

    private boolean findSite(final String siteId, final DotConnect dotConnect) {
        dotConnect.setSQL(SITE_LOOKUP_SQL);
        dotConnect.addParam(siteId);
        return dotConnect.getInt("x") == 1;
    }


    private int importCount;

    @Override
    public boolean forceRun() {
        addDotSaltClusterColumnIfNeeded();
        return keyStoreHelper.size() == 0 && isSet(Config.getStringProperty(APPS_IMPORT_EXPORT_DEFAULT_PASSWORD, BLANK)) ;
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dotConnect = new DotConnect();
        final Map<String, AppDescriptor> descriptorsMap = getAppDescriptors();

        final Path serverDir = Paths.get(APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + AppsAPIImpl.SERVER_DIR_NAME).normalize();
        final Key key = generateKey(loadPass(null));
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverDir,
                    entry -> matchesFileName(entry.getFileName().toString()))) {
                for (final Path path : stream) {
                    Logger.info(Task00050LoadAppsSecrets.class,String.format("Importing contents from file: `%s`",path));
                    boolean importFileHasErrors = false;
                    try {
                        final Map<String, List<AppSecrets>> secretsBySiteId = importSecrets(path, key);
                        for (final Entry<String, List<AppSecrets>> importEntry : secretsBySiteId.entrySet()) {
                            final String siteId = importEntry.getKey();
                            if (Host.SYSTEM_HOST.equalsIgnoreCase(siteId) || findSite(siteId, dotConnect)) {
                                final List<AppSecrets> appSecrets = importEntry.getValue();
                                for (final AppSecrets secrets : appSecrets) {
                                    if (!secrets.getSecrets().isEmpty()) {
                                        final AppDescriptor descriptor = descriptorsMap
                                                .get(secrets.getKey().toLowerCase());
                                        if (null != descriptor) {
                                            try {
                                                validateForSave(mapForValidation(secrets), descriptor, Optional.empty());
                                                final char[] chars = toJsonAsChars(secrets);
                                                final String internalKey = internalKey(secrets.getKey(), siteId);
                                                keyStoreHelper.saveValue(internalKey, chars);
                                                importCount++;
                                                Logger.error(Task00050LoadAppsSecrets.class, String.format(
                                                    "Secret for key `%s` and site `%s` imported successfully. ",
                                                    secrets.getKey(), siteId));
                                            } catch (IllegalArgumentException e) {
                                                Logger.error(Task00050LoadAppsSecrets.class, e);
                                                importFileHasErrors = true;
                                            }
                                        } else {
                                            Logger.error(Task00050LoadAppsSecrets.class, String.format(
                                                    "Cant't import a secret meant for a descriptor `%s` that doesn't exist locally.",
                                                    secrets.getKey()));
                                            importFileHasErrors = true;
                                        }
                                    } else {
                                        Logger.error(Task00050LoadAppsSecrets.class,
                                                "Cant't import a secret that was generated empty.");
                                        importFileHasErrors = true;
                                    }
                                }
                            } else {
                                Logger.error(Task00050LoadAppsSecrets.class, String.format(
                                         "Cant't import a secret that belongs into a unknown site `%s`. ", siteId));
                                importFileHasErrors = true;
                            }
                        }
                        //if we had an error or nothing came on the file.
                        importFileHasErrors = importFileHasErrors || secretsBySiteId.isEmpty();
                    } finally {
                        if (!importFileHasErrors) {
                            path.toFile().delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(Task00050LoadAppsSecrets.class, "Error Importing AppSecret from starter.",
                    e);
        }
    }

    /**
     * This Task attempts to create a security repo that depends on a call to {@link ClusterData#getClusterSalt()}
     * This creates a conflict When upgrading from 4.x since back then we didn't have the column `cluster_salt`
     * For which we need to call the upgrade task from here
     */
    private void addDotSaltClusterColumnIfNeeded() {
        try {
            final Task05350AddDotSaltClusterColumn task05350AddDotSaltClusterColumn = new Task05350AddDotSaltClusterColumn();
            if (task05350AddDotSaltClusterColumn.forceRun()) {
                task05350AddDotSaltClusterColumn.executeUpgrade();
            }

            final Task04355SystemEventAddServerIdColumn systemEventAddServerIdColumn = new Task04355SystemEventAddServerIdColumn();
            if (systemEventAddServerIdColumn.forceRun()) {
                systemEventAddServerIdColumn.executeUpgrade();
            }

        } catch (Exception e) {
            Logger.error(Task00050LoadAppsSecrets.class,
                    "Error Applying upgrade task from Task00050LoadAppsSecrets.", e);
        }
    }

    @VisibleForTesting
    public int getImportCount() {
        return importCount;
    }
}
