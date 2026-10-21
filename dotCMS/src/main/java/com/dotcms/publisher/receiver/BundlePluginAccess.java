package com.dotcms.publisher.receiver;

import com.dotcms.enterprise.publishing.remote.bundler.OSGIBundler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Decides who may bring plugin jars in through a bundle file uploaded by a logged-in user.
 * Plugins are installed from the plugins portlet; a bundle upload must not be a way around it.
 */
public final class BundlePluginAccess {

    /** Portlet that gives access to installing plugins. */
    public static final String PLUGINS_PORTLET = "dynamic-plugins";

    private BundlePluginAccess() {
    }

    /**
     * Tells whether a user may deploy plugins, that is, whether they have access to the plugins
     * portlet.
     *
     * @param user The user that uploaded the bundle.
     * @return {@code true} if the user has access to the plugins portlet; {@code false} if not or
     * if the check cannot be made.
     */
    public static boolean canDeployPlugins(final User user) {
        return user != null && Try.of(() -> APILocator.getLayoutAPI()
                .doesUserHaveAccessToPortlet(PLUGINS_PORTLET, user)).getOrElse(false);
    }

    /**
     * Tells whether a bundle archive has entries in its plugins folder. The archive is only listed,
     * nothing is extracted. Entry names are normalized first, so a name with {@code .} or {@code ..}
     * segments that resolves into the plugins folder counts.
     *
     * @param archive The {@code .tar.gz} bundle file.
     * @return {@code true} if at least one entry sits in the plugins folder; {@code false} if none
     * does or if the archive cannot be read (it then fails later, when it is processed).
     */
    public static boolean carriesPlugins(final File archive) {
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(
                new BufferedInputStream(Files.newInputStream(archive.toPath()))))) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {
                // collapse "." and ".." the way extraction resolves them
                String name = Paths.get(entry.getName().replace('\\', '/')).normalize()
                        .toString().replace('\\', '/');
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.equals(OSGIBundler.FOLDER_BUNDLES)
                        || name.startsWith(OSGIBundler.FOLDER_BUNDLES + "/")) {
                    return true;
                }
            }
        } catch (final IOException e) {
            Logger.warn(BundlePluginAccess.class,
                    "The bundle archive could not be listed: " + e.getMessage());
        }
        return false;
    }
}
