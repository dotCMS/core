package com.dotmarketing.startup.runonce;

import static com.dotcms.security.apps.AppsUtil.APPS_IMPORT_EXPORT_DEFAULT_PASSWORD;
import static com.dotcms.security.apps.AppsUtil.generateKey;
import static com.dotcms.security.apps.AppsUtil.importSecrets;
import static com.dotcms.security.apps.AppsUtil.internalKey;
import static com.dotcms.security.apps.AppsUtil.loadPass;
import static com.dotcms.security.apps.AppsUtil.mapForValidation;
import static com.dotcms.security.apps.AppsUtil.toJsonAsChars;
import static com.dotcms.security.apps.AppsUtil.validateForSave;
import static com.dotmarketing.util.UtilMethods.isSet;
import static com.liferay.util.StringPool.*;

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
import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Task201008LoadAppsSecrets implements StartupTask {

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
        return keyStoreHelper.size() == 0 && isSet(Config.getStringProperty(APPS_IMPORT_EXPORT_DEFAULT_PASSWORD, BLANK)) ;
    }

    @Override
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
                    try {
                        final Map<String, List<AppSecrets>> secretsBySiteId = importSecrets(
                                path, key);
                        for (final Entry<String, List<AppSecrets>> importEntry : secretsBySiteId
                                .entrySet()) {
                            final String siteId = importEntry.getKey();
                            if (Host.SYSTEM_HOST.equalsIgnoreCase(siteId) || findSite(siteId,
                                    dotConnect)) {
                                final List<AppSecrets> appSecrets = importEntry.getValue();
                                for (final AppSecrets secrets : appSecrets) {
                                    if (!secrets.getSecrets().isEmpty()) {
                                        final AppDescriptor descriptor = descriptorsMap
                                                .get(secrets.getKey().toLowerCase());
                                        if (null != descriptor) {
                                            validateForSave(mapForValidation(secrets), descriptor);
                                            final char[] chars = toJsonAsChars(secrets);
                                            final String internalKey = internalKey(secrets.getKey(),
                                                    siteId);
                                            keyStoreHelper.saveValue(internalKey, chars);
                                            importCount++;
                                        } else {
                                            throw new DotDataException(String.format(
                                                    "I cant't import a secret meant for a descriptor `%s` that doesn't exist locally.",
                                                    secrets.getKey()));
                                        }
                                    } else {
                                        throw new DotDataException(
                                                "I cant't import a secret that was generated empty.");
                                    }
                                }
                            } else {
                                throw new DotDataException(String.format(
                                        "I cant't import a secret that belongs into a unknown site `%s`.",
                                        siteId));
                            }
                        }
                    } finally {
                        path.toFile().delete();
                    }
                }
            }
        } catch (Exception e) {
            throw new DotDataException("Error Importing AppSecret from starer ", e);
        }
    }

    @VisibleForTesting
    public int getImportCount() {
        return importCount;
    }
}
