package com.dotcms.publisher.receiver;

import com.dotcms.enterprise.publishing.remote.bundler.OSGIBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
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
 * Plugin jars in an uploaded bundle need the same access as installing them from the plugins
 * portlet. This class decides, from how a bundle arrived, whether its plugin jars may be
 * deployed, and lists a bundle file to see whether it carries any.
 */
public final class BundlePluginAccess {

    /** Portlet that gives access to installing plugins. */
    public static final String PLUGINS_PORTLET = PortletID.DYNAMIC_PLUGINS.toString();

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
     * Tells whether the plugins of a received bundle may be deployed, from how the bundle arrived:
     * a bundle from a registered sender may; a user upload may only if the uploader still has
     * access to the plugins portlet; anything else (no origin recorded, an unknown origin, an
     * uploader that cannot be found) may not.
     *
     * @param config The configuration of the bundle being processed.
     * @return {@code true} if the plugins of the bundle may be deployed.
     */
    public static boolean mayDeployPlugins(final PublisherConfig config) {
        final BundleOrigin origin = parseOrigin(config.get(PublisherConfig.Config.BUNDLE_ORIGIN.name()));
        if (origin == BundleOrigin.REGISTERED_SENDER) {
            return true;
        }
        if (origin == BundleOrigin.USER_UPLOAD) {
            final Object uploaderId = config.get(PublisherConfig.Config.UPLOADED_BY.name());
            return uploaderId instanceof String && canDeployPlugins(Try.of(() -> APILocator.getUserAPI()
                    .loadUserById((String) uploaderId)).getOrNull());
        }
        return false;
    }

    /**
     * Reads a bundle origin from a stored value.
     *
     * @param value The stored value, normally the name of a {@link BundleOrigin}.
     * @return The origin, or {@code null} if the value is missing or not a known origin.
     */
    public static BundleOrigin parseOrigin(final Object value) {
        return value == null ? null
                : Try.of(() -> BundleOrigin.valueOf(value.toString())).getOrNull();
    }

    /**
     * Tells whether a bundle archive has entries in its plugins folder. The archive is only listed,
     * nothing is extracted. Entry names are normalized first, so a name with {@code .} or {@code ..}
     * segments that resolves into the plugins folder counts.
     *
     * @param archive The {@code .tar.gz} bundle file.
     * @return {@code true} if at least one entry sits in the plugins folder or if the archive
     * cannot be read, so that an archive that cannot be listed is treated as carrying plugins;
     * {@code false} if it was read and none does.
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
                    "The bundle archive could not be listed, treating it as carrying plugins: "
                            + e.getMessage());
            return true;
        }
        return false;
    }
}
