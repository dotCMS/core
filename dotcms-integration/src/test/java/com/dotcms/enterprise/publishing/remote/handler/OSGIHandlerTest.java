package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.receiver.BundleOrigin;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.OSGIUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link OSGIHandler}: when it is allowed to deploy or remove plugin jars carried by a
 * received bundle, and which jar names from the bundle descriptor it accepts.
 *
 * <p>The handler writes into the real plugin folders of the test instance, so every file a test
 * creates has a unique name and is removed in {@link #cleanup()}.
 */
public class OSGIHandlerTest extends IntegrationTestBase {

    private static final String DESCRIPTOR_SUFFIX = ".jar.descriptor.osgi.json";

    private final List<File> toDelete = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<Layout> layouts = new ArrayList<>();
    private File uploadFolder;
    private File deployFolder;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void cleanup() {
        toDelete.forEach(FileUtils::deleteQuietly);
        toDelete.clear();
        users.forEach(user -> Try.run(() -> UserDataGen.remove(user, Boolean.TRUE)));
        users.clear();
        layouts.forEach(layout -> Try.run(() -> APILocator.getLayoutAPI().removeLayout(layout)));
        layouts.clear();
    }

    private User backendUserWithPortlets(final String... portletIds) throws Exception {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        users.add(user);
        if (portletIds.length > 0) {
            final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
            layouts.add(layout);
            APILocator.getRoleAPI().addLayoutToRole(layout, APILocator.getRoleAPI().getUserRole(user));
        }
        return user;
    }

    private void initFolders() {
        uploadFolder = new File(OSGIUtil.getInstance().getFelixUploadPath());
        deployFolder = new File(OSGIUtil.getInstance().getFelixDeployPath());
        uploadFolder.mkdirs();
        deployFolder.mkdirs();
    }

    private static String uniqueName() {
        return "zz-test-" + UUIDGenerator.shorty();
    }

    private File newBundleFolder() throws Exception {
        final File bundleFolder = Files.createTempDirectory("bundle-folder").toFile();
        new File(bundleFolder, "osgiBundles").mkdirs();
        toDelete.add(bundleFolder);
        return bundleFolder;
    }

    private static void writeDescriptor(final File bundleFolder, final String operation,
            final String jarName) throws Exception {
        final String json = "{\"operation\":\"" + operation + "\",\"jarName\":\""
                + jarName.replace("\\", "\\\\") + "\"}";
        Files.write(new File(bundleFolder, "osgiBundles/" + uniqueName() + DESCRIPTOR_SUFFIX).toPath(),
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static PublisherConfig config(final BundleOrigin origin, final String uploaderId) {
        final PublisherConfig config = new PushPublisherConfig();
        config.setId(uniqueName() + ".tar.gz");
        if (origin != null) {
            config.put(PublisherConfig.Config.BUNDLE_ORIGIN.name(), origin.name());
        }
        if (uploaderId != null) {
            config.put(PublisherConfig.Config.UPLOADED_BY.name(), uploaderId);
        }
        return config;
    }

    private static PublisherConfig senderConfig() {
        return config(BundleOrigin.REGISTERED_SENDER, null);
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle publishes a jar and its origin is not recorded (unknown origin)
     * Should: copy nothing into the plugin upload folder.
     */
    @Test
    public void test_publish_withUnknownOrigin_copiesNothing() throws Exception {
        initFolders();
        final File bundleFolder = newBundleFolder();
        final String jarName = uniqueName() + ".jar";
        Files.write(new File(bundleFolder, "osgiBundles/" + jarName).toPath(), "not a real jar".getBytes());
        writeDescriptor(bundleFolder, "PUBLISH", jarName);
        toDelete.add(new File(uploadFolder, jarName));

        new OSGIHandler(config(null, null)).handle(bundleFolder);

        assertFalse("no jar may be deployed when the origin is unknown",
                new File(uploadFolder, jarName).exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle publishes a jar and it was uploaded by a user without access to the plugins
     * portlet
     * Should: copy nothing into the plugin upload folder.
     */
    @Test
    public void test_publish_uploadedByUserWithoutPluginsPortlet_copiesNothing() throws Exception {
        initFolders();
        final File bundleFolder = newBundleFolder();
        final String jarName = uniqueName() + ".jar";
        Files.write(new File(bundleFolder, "osgiBundles/" + jarName).toPath(), "not a real jar".getBytes());
        writeDescriptor(bundleFolder, "PUBLISH", jarName);
        toDelete.add(new File(uploadFolder, jarName));

        new OSGIHandler(config(BundleOrigin.USER_UPLOAD, backendUserWithPortlets("publishing-queue").getUserId()))
                .handle(bundleFolder);

        assertFalse(new File(uploadFolder, jarName).exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle unpublishes a jar and it was uploaded by a user without access to the
     * plugins portlet
     * Should: leave the deployed jar in place.
     */
    @Test
    public void test_unpublish_withoutPermission_deletesNothing() throws Exception {
        initFolders();
        final File bundleFolder = newBundleFolder();
        final String jarName = uniqueName() + ".jar";
        final File deployed = new File(deployFolder, jarName);
        Files.write(deployed.toPath(), "not a real jar".getBytes());
        toDelete.add(deployed);
        writeDescriptor(bundleFolder, "UNPUBLISH", jarName);

        new OSGIHandler(config(BundleOrigin.USER_UPLOAD, backendUserWithPortlets().getUserId()))
                .handle(bundleFolder);

        assertTrue("the deployed jar must survive", deployed.exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle comes from a registered sender and the jar name is a plain file name
     * Should: still copy the jar into the plugin upload folder, so legitimate pushes keep working.
     */
    @Test
    public void test_publish_fromRegisteredSender_copiesJar() throws Exception {
        initFolders();
        final File bundleFolder = newBundleFolder();
        final String jarName = uniqueName() + ".jar";
        Files.write(new File(bundleFolder, "osgiBundles/" + jarName).toPath(), "not a real jar".getBytes());
        writeDescriptor(bundleFolder, "PUBLISH", jarName);
        toDelete.add(new File(uploadFolder, jarName));
        toDelete.add(new File(deployFolder, jarName));

        new OSGIHandler(senderConfig()).handle(bundleFolder);

        assertTrue(new File(uploadFolder, jarName).exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle was uploaded by a user who has access to the plugins portlet
     * Should: copy the jar into the plugin upload folder.
     */
    @Test
    public void test_publish_uploadedByUserWithPluginsPortlet_copiesJar() throws Exception {
        initFolders();
        final File bundleFolder = newBundleFolder();
        final String jarName = uniqueName() + ".jar";
        Files.write(new File(bundleFolder, "osgiBundles/" + jarName).toPath(), "not a real jar".getBytes());
        writeDescriptor(bundleFolder, "PUBLISH", jarName);
        toDelete.add(new File(uploadFolder, jarName));
        toDelete.add(new File(deployFolder, jarName));

        new OSGIHandler(config(BundleOrigin.USER_UPLOAD,
                backendUserWithPortlets("dynamic-plugins").getUserId())).handle(bundleFolder);

        assertTrue(new File(uploadFolder, jarName).exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle is recorded as a user upload but the user cannot be found
     * Should: copy nothing, since the permission cannot be confirmed.
     */
    @Test
    public void test_publish_uploadedByUnknownUser_copiesNothing() throws Exception {
        initFolders();
        final File bundleFolder = newBundleFolder();
        final String jarName = uniqueName() + ".jar";
        Files.write(new File(bundleFolder, "osgiBundles/" + jarName).toPath(), "not a real jar".getBytes());
        writeDescriptor(bundleFolder, "PUBLISH", jarName);
        toDelete.add(new File(uploadFolder, jarName));

        new OSGIHandler(config(BundleOrigin.USER_UPLOAD, "no-such-user-" + uniqueName()))
                .handle(bundleFolder);

        assertFalse(new File(uploadFolder, jarName).exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: a PUBLISH descriptor names a jar that is not a plain file name inside the bundle's
     * plugin folder: a parent path, a nested path, an absolute path or an empty name
     * Should: fail the whole bundle and copy nothing, not even the file the path points to.
     */
    @Test
    public void test_publish_rejectsJarNamesThatAreNotPlainFileNames() throws Exception {
        initFolders();
        final File outside = Files.createTempFile("outside", ".jar").toFile();
        toDelete.add(outside);
        toDelete.add(new File(uploadFolder, outside.getName()));

        final String[] badNames = {
                "../../" + outside.getName(),
                "../" + outside.getName(),
                "sub/" + outside.getName(),
                outside.getAbsolutePath(),
                ""};

        for (final String badName : badNames) {
            final File bundleFolder = newBundleFolder();
            writeDescriptor(bundleFolder, "PUBLISH", badName);

            assertThrows("PUBLISH with jar name '" + badName + "' must fail the bundle",
                    Exception.class, () -> new OSGIHandler(senderConfig()).handle(bundleFolder));
            assertFalse("nothing may be copied for jar name '" + badName + "'",
                    new File(uploadFolder, outside.getName()).exists());
        }
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the PUBLISH jar name is a plain name but the entry is a symbolic link to a file outside
     * the bundle folder, or a directory
     * Should: fail the whole bundle and copy nothing.
     */
    @Test
    public void test_publish_rejectsSymlinkAndNonRegularEntries() throws Exception {
        initFolders();
        final File outside = Files.createTempFile("outside", ".jar").toFile();
        toDelete.add(outside);

        final File linkBundle = newBundleFolder();
        final String linkName = uniqueName() + ".jar";
        Files.createSymbolicLink(new File(linkBundle, "osgiBundles/" + linkName).toPath(), outside.toPath());
        writeDescriptor(linkBundle, "PUBLISH", linkName);
        toDelete.add(new File(uploadFolder, linkName));

        final File dirBundle = newBundleFolder();
        final String dirName = uniqueName() + ".jar";
        new File(dirBundle, "osgiBundles/" + dirName).mkdirs();
        writeDescriptor(dirBundle, "PUBLISH", dirName);
        toDelete.add(new File(uploadFolder, dirName));

        assertThrows(Exception.class, () -> new OSGIHandler(senderConfig()).handle(linkBundle));
        assertThrows(Exception.class, () -> new OSGIHandler(senderConfig()).handle(dirBundle));
        assertFalse(new File(uploadFolder, linkName).exists());
        assertFalse(new File(uploadFolder, dirName).exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: an UNPUBLISH descriptor names a jar that is not a plain file name inside the plugin
     * load folder (parent path, nested path, absolute path, empty name)
     * Should: fail the whole bundle and leave the file the path points to untouched.
     *
     * <p>The target is a file this test creates next to the load folder, never a real asset.
     */
    @Test
    public void test_unpublish_rejectsJarNamesThatAreNotPlainFileNames() throws Exception {
        initFolders();
        final File sentinel = new File(deployFolder.getParentFile(), uniqueName() + ".tmp");
        Files.write(sentinel.toPath(), "keep".getBytes());
        toDelete.add(sentinel);
        // keeps the load folder non-empty so an empty name can never remove the folder itself
        final File placeholder = new File(deployFolder, uniqueName() + ".keep");
        Files.write(placeholder.toPath(), "keep".getBytes());
        toDelete.add(placeholder);

        final String[] badNames = {
                "../" + sentinel.getName(),
                "sub/../../" + sentinel.getName(),
                sentinel.getAbsolutePath(),
                ""};

        for (final String badName : badNames) {
            final File bundleFolder = newBundleFolder();
            writeDescriptor(bundleFolder, "UNPUBLISH", badName);

            assertThrows("UNPUBLISH with jar name '" + badName + "' must fail the bundle",
                    Exception.class, () -> new OSGIHandler(senderConfig()).handle(bundleFolder));
            assertTrue("the file outside the load folder must survive '" + badName + "'",
                    sentinel.exists());
        }
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: an UNPUBLISH descriptor names a directory inside the plugin load folder
     * Should: fail the whole bundle and keep the directory (only regular files are removed).
     */
    @Test
    public void test_unpublish_rejectsNonRegularEntries() throws Exception {
        initFolders();
        final String dirName = uniqueName();
        final Path dir = Files.createDirectory(new File(deployFolder, dirName).toPath());
        toDelete.add(dir.toFile());

        final File bundleFolder = newBundleFolder();
        writeDescriptor(bundleFolder, "UNPUBLISH", dirName);

        assertThrows(Exception.class, () -> new OSGIHandler(senderConfig()).handle(bundleFolder));
        assertTrue(dir.toFile().exists());
    }

    /**
     * Method to Test: {@link OSGIHandler#handle(File)}
     * When: the bundle comes from a registered sender and an UNPUBLISH descriptor names a plain jar
     * file in the load folder
     * Should: still remove it, so legitimate unpublish keeps working.
     */
    @Test
    public void test_unpublish_fromRegisteredSender_removesJar() throws Exception {
        initFolders();
        final String jarName = uniqueName() + ".jar";
        final File deployed = new File(deployFolder, jarName);
        Files.write(deployed.toPath(), "not a real jar".getBytes());
        toDelete.add(deployed);
        final File bundleFolder = newBundleFolder();
        writeDescriptor(bundleFolder, "UNPUBLISH", jarName);

        new OSGIHandler(senderConfig()).handle(bundleFolder);

        assertFalse(deployed.exists());
    }
}
