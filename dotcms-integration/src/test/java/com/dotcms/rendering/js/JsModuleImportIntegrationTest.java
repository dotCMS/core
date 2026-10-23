package com.dotcms.rendering.js;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for loading JavaScript modules stored as file assets with {@code import()}, using the
 * content-backed module source of {@link JsFileSystem} (its default constructor) against real sites, folders and file
 * assets.
 * <p>
 * Scripts run in a GraalJS context built by the engine's own context builder, {@code JsEngine#buildContext}, with a
 * content-backed {@code new JsFileSystem()}. The tests still do not call {@link JsEngine#eval}: {@code eval} first
 * preloads the server's {@code WEB-INF/javascript} sources through the servlet context, and the servlet context used
 * by the integration environment does not provide them, so {@code eval} stops before the script runs. The module
 * loading path under test (module path handling, site and file asset lookup, binary read and its cache) is the same
 * in both cases, because {@code JsEngine} builds its contexts with that same builder and file system.
 * <p>
 * No request is bound to the test thread while a script runs, so an {@code /application/} path without a site name
 * resolves on the default site.
 */
public class JsModuleImportIntegrationTest extends IntegrationTestBase {

    private static final String MODULES_FOLDER_PATH = "/application/js/modules/";
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    private static final List<Contentlet> createdModules = new ArrayList<>();
    private static final List<Folder> createdFolders = new ArrayList<>();
    private static final List<Host> createdSites = new ArrayList<>();
    private static final List<Path> createdTempFolders = new ArrayList<>();

    private static Host defaultSite;

    private HttpServletRequest previousRequest;

    /**
     * Starts the dotCMS services and loads the default site, where modules without a site name are looked up.
     *
     * @throws Exception when the services cannot be started or the default site cannot be loaded
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        defaultSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
    }

    /**
     * Removes every module, folder and site created by these tests, plus the temporary files the modules were
     * uploaded from. Each item is removed on its own, so one failure does not leave the rest behind.
     */
    @AfterClass
    public static void cleanUp() {
        for (final Contentlet module : createdModules) {
            try {
                ContentletDataGen.destroy(module);
            } catch (final Exception e) {
                Logger.warn(JsModuleImportIntegrationTest.class, "Could not remove a test module", e);
            }
        }

        for (final Folder folder : createdFolders) {
            try {
                FolderDataGen.remove(folder);
            } catch (final RuntimeException e) {
                Logger.warn(JsModuleImportIntegrationTest.class, "Could not remove a test folder: " + e.getMessage());
            }
        }

        for (final Host site : createdSites) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(JsModuleImportIntegrationTest.class, "Could not remove a test site: " + e.getMessage());
            }
        }

        for (final Path tempFolder : createdTempFolders) {
            FileUtils.deleteQuietly(tempFolder.toFile());
        }
    }

    /**
     * Unbinds any request left on the test thread, so a module path without a site name resolves on the default
     * site. The previous request is kept and restored by {@link #restoreRequest()}.
     */
    @Before
    public void clearRequest() {
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * Puts back the request that was bound to the test thread before {@link #clearRequest()} ran.
     */
    @After
    public void restoreRequest() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
    }

    /**
     * Checks that a module stored as an unpublished file asset under {@code /application/} on the current site is
     * loaded by its module path and returns its export. Loading the stored file by its file system path is not a
     * module path, so that import is refused and returns none of the module's content. Before that second import,
     * the test checks that the file system path is absolute and points to a file that exists, so the refusal comes
     * from the path not being a module path and not from a file that is missing.
     *
     * @throws Exception when the test data cannot be created
     */
    @Test
    public void currentSiteApplicationModuleIsImported() throws Exception {
        // Given an unpublished module file asset under /application/js/modules/ on the default site, whose binary
        // is stored as a file with an absolute file system path
        final String fileName = "current-site-module-" + RUN_ID + ".mjs";
        final Contentlet module = createModule(defaultSite, fileName,
                "export const value = 'CURRENT-SITE-EXPORT';");
        final Path binaryLocation = APILocator.getFileAssetAPI().fromContentlet(module).getFileAsset().toPath();
        assertTrue("The stored module file should have an absolute file system path: " + binaryLocation,
                binaryLocation.isAbsolute());
        assertTrue("The stored module file should exist at " + binaryLocation, Files.isRegularFile(binaryLocation));

        // When a script imports /application/js/modules/<name>, and then the stored file by its file system path
        final String fromApplicationPath = runAsync(
                "(await import('" + MODULES_FOLDER_PATH + fileName + "')).value");
        final String fromFileSystemPath = runAsync("(await import('" + binaryLocation + "')).value");

        // Then the module path returns the module's export, and the file system path, which is not a module path,
        // is refused without returning the module's content
        assertEquals("RESOLVED CURRENT-SITE-EXPORT", fromApplicationPath);
        assertTrue(fromFileSystemPath, fromFileSystemPath.startsWith("REJECTED"));
        assertFalse(fromFileSystemPath, fromFileSystemPath.contains("CURRENT-SITE-EXPORT"));
    }

    /**
     * Checks that a script naming a site with {@code //<site>/application/...} gets the module of that site, whether
     * the site is named by its name, its identifier or one of its aliases, even when the default site holds a module
     * with the same name and a different export.
     *
     * @throws Exception when the test data cannot be created
     */
    @Test
    public void siteQualifiedApplicationModuleIsImported() throws Exception {
        // Given a second site with an alias, and a module with the same file name on the default site and on the
        // second site
        final String siteName = "js-modules-" + RUN_ID + ".test";
        final String siteAlias = "js-modules-alias-" + RUN_ID + ".test";
        final Host secondSite = new SiteDataGen().name(siteName).aliases(siteAlias).nextPersisted(true);
        createdSites.add(secondSite);
        final String fileName = "shared-module-" + RUN_ID + ".mjs";
        createModule(defaultSite, fileName, "export const value = 'DEFAULT-SITE-EXPORT';");
        createModule(secondSite, fileName, "export const value = 'SECOND-SITE-EXPORT';");

        // When a script imports /application/js/modules/<name> of the second site by its name, its identifier and its
        // alias, and then the same path with no site
        final String fromSecondSite = runAsync(
                "(await import('//" + siteName + MODULES_FOLDER_PATH + fileName + "')).value");
        final String fromSecondSiteIdentifier = runAsync(
                "(await import('//" + secondSite.getIdentifier() + MODULES_FOLDER_PATH + fileName + "')).value");
        final String fromSecondSiteAlias = runAsync(
                "(await import('//" + siteAlias + MODULES_FOLDER_PATH + fileName + "')).value");
        final String fromCurrentSite = runAsync(
                "(await import('" + MODULES_FOLDER_PATH + fileName + "')).value");

        // Then every site-qualified path returns the second site's export, and the plain path the default site's one
        assertEquals("RESOLVED SECOND-SITE-EXPORT", fromSecondSite);
        assertEquals("RESOLVED SECOND-SITE-EXPORT", fromSecondSiteIdentifier);
        assertEquals("RESOLVED SECOND-SITE-EXPORT", fromSecondSiteAlias);
        assertEquals("RESOLVED DEFAULT-SITE-EXPORT", fromCurrentSite);
    }

    /**
     * Checks that once a new version of a module is checked in, the next import returns the new export, because the
     * new version's binary is stored at a new location and the cached source of the old one is not used.
     *
     * @throws Exception when the test data cannot be created or updated
     */
    @Test
    public void newModuleVersionIsServedOnNextImport() throws Exception {
        // Given a module on the default site that has already been imported once
        final String fileName = "versioned-module-" + RUN_ID + ".mjs";
        final String importScript = "(await import('" + MODULES_FOLDER_PATH + fileName + "')).value";
        final Contentlet firstVersion = createModule(defaultSite, fileName, "export const value = 'FIRST-VERSION';");
        assertEquals("RESOLVED FIRST-VERSION", runAsync(importScript));

        // When the module is checked out, its binary is replaced with a new export, and it is checked in again
        final Contentlet workingCopy = ContentletDataGen.checkout(firstVersion);
        workingCopy.setBinary(FileAssetAPI.BINARY_FIELD,
                writeTempFile(fileName, "export const value = 'SECOND-VERSION';"));
        final Contentlet secondVersion = ContentletDataGen.checkin(workingCopy);
        final String secondImport = runAsync(importScript);

        // Then the new version's binary has a new location, and importing the module returns the new export
        assertNotEquals(binaryPath(firstVersion), binaryPath(secondVersion));
        assertEquals("RESOLVED SECOND-VERSION", secondImport);
    }

    /**
     * Checks that a module that exists on the default site is not found when the script names a site that does not
     * exist, and that a module that does not exist on the default site is not found either: both imports are
     * rejected, the first one without the default site module's export, and the file system reports both paths as
     * not found.
     *
     * @throws Exception when the test data cannot be created
     */
    @Test
    public void missingSiteOrModuleIsNotFound() throws Exception {
        // Given a module that the default site serves, a site name no site uses, and a module name no file asset of
        // the default site uses
        final String fileName = "default-site-only-module-" + RUN_ID + ".mjs";
        createModule(defaultSite, fileName, "export const value = 'DEFAULT-SITE-ONLY-EXPORT';");
        final String existingModulePath = MODULES_FOLDER_PATH + fileName;
        assertEquals("RESOLVED DEFAULT-SITE-ONLY-EXPORT",
                runAsync("(await import('" + existingModulePath + "')).value"));
        final String missingSitePath = "//no-such-site-" + RUN_ID + ".test" + existingModulePath;
        final String missingModulePath = MODULES_FOLDER_PATH + "no-such-module-" + RUN_ID + ".mjs";

        // When a script imports the existing module through the unknown site, and the missing module, and the file
        // system opens each path
        final String fromMissingSite = runAsync("(await import('" + missingSitePath + "')).value");
        final String fromMissingModule = runAsync("(await import('" + missingModulePath + "')).value");

        // Then both imports are rejected, the unknown site does not fall back to the default site's module, and both
        // paths are reported as not found
        assertTrue(fromMissingSite, fromMissingSite.startsWith("REJECTED"));
        assertFalse(fromMissingSite, fromMissingSite.contains("DEFAULT-SITE-ONLY-EXPORT"));
        assertTrue(fromMissingModule, fromMissingModule.startsWith("REJECTED"));
        assertEquals(NoSuchFileException.class, openFailure(missingSitePath));
        assertEquals(NoSuchFileException.class, openFailure(missingModulePath));
    }

    /**
     * Checks that a module stops being served once it is archived: after the archive, importing it by the same module
     * path is rejected without returning its export, and the file system reports the path as not found. The archived
     * module is still removed after the tests, because it stays in the list of created modules.
     *
     * @throws Exception when the test data cannot be created or archived
     */
    @Test
    public void archivedModuleIsNotFound() throws Exception {
        // Given a module on the default site that has already been imported once
        final String fileName = "archived-module-" + RUN_ID + ".mjs";
        final String modulePath = MODULES_FOLDER_PATH + fileName;
        final String importScript = "(await import('" + modulePath + "')).value";
        final Contentlet module = createModule(defaultSite, fileName,
                "export const value = 'ARCHIVED-MODULE-EXPORT';");
        assertEquals("RESOLVED ARCHIVED-MODULE-EXPORT", runAsync(importScript));

        // When the module is archived by the system user, and a script imports the same module path again
        ContentletDataGen.archive(module);
        final String afterArchive = runAsync(importScript);

        // Then the import is rejected without returning the module's export, and the path is reported as not found
        assertTrue(afterArchive, afterArchive.startsWith("REJECTED"));
        assertFalse(afterArchive, afterArchive.contains("ARCHIVED-MODULE-EXPORT"));
        assertEquals(NoSuchFileException.class, openFailure(modulePath));
    }

    /**
     * Creates a JavaScript module as a working (unpublished) file asset in the {@code /application/js/modules/}
     * folder of a site, and keeps it for removal after the tests.
     *
     * @param site     the {@link Host} the module belongs to
     * @param fileName the file name of the module, which is also the last part of its import path
     * @param source   the module's source text
     * @return the module's {@link Contentlet}
     * @throws Exception when the folder or the file asset cannot be created
     */
    private static Contentlet createModule(final Host site, final String fileName, final String source)
            throws Exception {
        final Folder folder = moduleFolder(site);
        final Contentlet module = new FileAssetDataGen(writeTempFile(fileName, source))
                .folder(folder).host(site).nextPersisted();
        createdModules.add(module);
        return module;
    }

    /**
     * Returns the {@code /application/js/modules/} folder of a site, creating the missing parts. The first folder of
     * that path that did not exist is kept for removal after the tests, so folders that were already there stay.
     *
     * @param site the {@link Host} that holds the folder
     * @return the modules {@link Folder}
     * @throws DotDataException     when the folders cannot be read or saved
     * @throws DotSecurityException when the system user cannot read or save the folders
     */
    private static Folder moduleFolder(final Host site) throws DotDataException, DotSecurityException {
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final User systemUser = APILocator.systemUser();

        String firstMissingPath = null;
        final StringBuilder path = new StringBuilder("/");
        for (final String name : MODULES_FOLDER_PATH.substring(1).split("/")) {
            path.append(name).append('/');
            final Folder existing = folderAPI.findFolderByPath(path.toString(), site, systemUser, false);
            if (null == existing || !UtilMethods.isSet(existing.getInode())) {
                firstMissingPath = path.toString();
                break;
            }
        }

        final Folder modulesFolder = folderAPI.createFolders(MODULES_FOLDER_PATH, site, systemUser, false);
        if (null != firstMissingPath) {
            createdFolders.add(folderAPI.findFolderByPath(firstMissingPath, site, systemUser, false));
        }
        return modulesFolder;
    }

    /**
     * Writes a file with the given name and text in a new temporary folder, so two files can share a name.
     *
     * @param fileName the name of the file
     * @param content  the text to write
     * @return the written file
     * @throws IOException when the file cannot be written
     */
    private static File writeTempFile(final String fileName, final String content) throws IOException {
        final Path tempFolder = Files.createTempDirectory("js-module-import");
        createdTempFolders.add(tempFolder);
        final Path file = tempFolder.resolve(fileName);
        Files.writeString(file, content);
        return file.toFile();
    }

    /**
     * Returns the absolute file system path of a file asset version's binary.
     *
     * @param fileAsset the file asset version
     * @return the absolute path of its binary
     */
    private static String binaryPath(final Contentlet fileAsset) {
        return APILocator.getFileAssetAPI().fromContentlet(fileAsset).getFileAsset().getAbsolutePath();
    }

    /**
     * Opens a module path through a content-backed {@link JsFileSystem}, the way the engine reads a module, and
     * reports how it failed.
     *
     * @param path the module path, as a script would write it
     * @return the class of the exception thrown, or {@code null} when the module could be opened
     */
    private static Class<? extends Exception> openFailure(final String path) {
        final JsFileSystem fileSystem = new JsFileSystem();
        try (SeekableByteChannel ignored = fileSystem.newByteChannel(fileSystem.parsePath(path),
                Collections.emptySet())) {
            return null;
        } catch (final Exception e) {
            return e.getClass();
        }
    }

    /**
     * Runs an asynchronous expression in a GraalJS context built by {@code JsEngine#buildContext}, with a
     * content-backed {@link JsFileSystem} and without full host access, and returns {@code RESOLVED <value>} or
     * {@code REJECTED <error>}.
     *
     * @param expression a JavaScript expression that may use {@code await}
     * @return the outcome of the expression as text
     */
    private static String runAsync(final String expression) {
        try (Context context = JsEngine.buildContext(new JsFileSystem(), false)) {
            context.eval(Source.create("js", "(async () => { try { globalThis.result = 'RESOLVED ' + ("
                    + expression + "); } catch (e) { globalThis.result = 'REJECTED ' + e; } })();"));
            return context.eval("js", "String(globalThis.result)").asString();
        }
    }
}
