package com.dotmarketing.startup.runonce;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsAPIImpl;
import com.dotcms.security.apps.AppsUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Task05390LoadAppsSecrets implements StartupTask {

    final AppsAPI appsAPI = APILocator.getAppsAPI();

    @Override
    public boolean forceRun() {
        return Try.of(() -> appsAPI.appKeysByHost().isEmpty()).getOrElse(() -> false);
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final User user = APILocator.systemUser();
        final Path path = Paths.get(AppsAPIImpl.getAppsDefaultDirectory());
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(null));
        try {
            final Map<String, List<AppSecrets>> importedSecretsBySiteId = appsAPI.importSecrets(path, key, user);

            for (final Entry<String, List<AppSecrets>> entry : importedSecretsBySiteId.entrySet()) {
                final String siteId = entry.getKey();
                final List<AppSecrets> secrets = entry.getValue();
               // final Host site = hostAPI.find(siteId, user, false);

                    for (final AppSecrets appSecrets : secrets) {
                        appsAPI.saveSecrets(appSecrets, siteId, user);
                    }

            }

        } catch (Exception e) {
            throw new DotDataException("Error Importing AppSecret from starer ", e);
        }
    }

}
