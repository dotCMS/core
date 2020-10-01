package com.dotmarketing.startup.runonce;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsAPIImpl;
import com.dotcms.security.apps.AppsUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Task05400LoadAppsSecrets implements StartupTask {

    private static final Pattern importFilePattern = Pattern.compile("^dotSecrets-import\\.([a-zA-Z0-9-_]+)", Pattern.CASE_INSENSITIVE);

    static boolean matchesFileName(final String input) {
        final Matcher matcher = importFilePattern.matcher(input);
        return matcher.find();
    }

    final AppsAPI appsAPI = APILocator.getAppsAPI();

    private int importCount;

    @Override
    public boolean forceRun() {
        return Try.of(() -> appsAPI.appKeysByHost().isEmpty()).getOrElse(() -> false);
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final User user = APILocator.systemUser();
        final Path appsDefaultDir = Paths.get(AppsAPIImpl.getAppsDefaultDirectory());
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(null));
        try {

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(appsDefaultDir,
                    entry -> matchesFileName(entry.getFileName().toString()))) {
                for (final Path path : stream) {
                     importCount += appsAPI.importSecretsAndSave(path, key, user);
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
