package com.dotcms.test.util;



import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.publishing.remote.bundler.AssignableFromMap;
import com.dotcms.test.util.assertion.*;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.google.common.collect.ImmutableList;
import com.liferay.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Provide util test for testing
 */
public class FileTestUtil {

    static AssignableFromMap<AssertionChecker> assertions;

    static{
        assertions = new AssignableFromMap<>();
        assertions.put(ContentType.class, new ContentTypeAssertionChecker());
        assertions.put(Host.class, new HostAssertionChecker());
        assertions.put(WorkflowScheme.class, new WorkflowSchemeAssertionChecker());
        assertions.put(Relationship.class, new RelationshipAssertionChecker());
        assertions.put(Category.class, new CategoryAssertionChecker());
        assertions.put(Template.class, new TemplateAssertionChecker());
        assertions.put(Container.class, new ContainerAssertionChecker());
        assertions.put(Folder.class, new FolderAssertionChecker());
        assertions.put(Link.class, new LinkAssertionChecker());
        assertions.put(Contentlet.class, new ContentletAssertionChecker());
        assertions.put(Language.class, new LanguageAssertionChecker());
        assertions.put(Rule.class, new RuleAssertionChecker());
        assertions.put(Link.class, new LinkAssertionChecker());
    }

    private FileTestUtil(){}

    /**
     * Get the content file and formatted it with the arguments
     * For example if the file has the content:
     * <pre>
     *     Hello __name__
     * </pre>
     *
     * and as argument we pass:
     *
     * name -> Wordl
     *
     * then the method will be return
     * <pre>
     *     Hello Wordl
     * </pre>
     *
     * @param file
     * @param arguments
     * @return
     * @throws IOException
     */
    public static String getFormattedContent(final File file, final Map<String, Object> arguments) throws IOException {
        final byte[] bytes = FileUtil.getBytes(file);
        final String fileContent = new String(bytes);
        return StringFormatterTestUtil.format(fileContent, arguments);
    }

    /**
     * Get the content file and formatted it with the arguments, and removing al the content in the end of each line
     *
     * @see FileTestUtil#getFormattedContent(File, Map)
     *
     * @param file
     * @param arguments
     * @return
     * @throws IOException
     */
    public static String getFormattedContentWithoutSpace(final File file, final Map<String, Object> arguments) throws IOException {
        return removeSpace(getFormattedContent(file, arguments));
    }

    private static String removeSpace(final String message){
        return message.replaceAll("\n[ \\t\\r\\n\\v\\f]*", "");
    }

    /**
     * Return the content of the file
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String getFileContent(final File file) throws IOException {
        final byte[] bytes = FileUtil.getBytes(file);
        return new String(bytes);
    }

    /**
     * Look a file into the resource directory
     *
     * @param path file relative path in the resource directory
     * @return
     * @throws IOException
     */
    public static File getFileInResources(final String path){
        return new File(FileTestUtil.class.getResource(path).getFile());
    }

    /**
     * Check in bundleRoot exists the file for the asset and it have the right content.
     * it use the {@link AssertionChecker} class to know how to make the asserts, step by step this method
     * do the follow:
     *
     * - Check if the Files get by {@link AssertionChecker#getFile(Object, File)} method exists}, If any of the files not
     * exists throw a {@link AssertionError}.
     * - If {@link AssertionChecker#checkFileContent(Object)} return true then check the content of the file.
     * - Use the file path return by {@link AssertionChecker#getFilePathExpected(File)} as template, then populate it using
     * {@link AssertionChecker#getFileArguments(Object, File)} method, later compare the result with the content of
     * {@link AssertionChecker#getFile(Object, File)}, if the content are not equal then throw a {@link AssertionError}
     *
     * @param bundleRoot bundle root directory
     * @param asset asset to be Assert
     * @return
     * @throws IOException if the file not exists or not has the right content
     *
     * @see FileTestUtil#getFormattedContent(File, Map)
     * @see AssertionChecker
     */
    public static Collection<File> assertBundleFile(
            final File bundleRoot,
            final Object asset) throws IOException {
        final AssertionChecker assertionChecker = assertions.get(asset.getClass());

        return assertBundleFile(bundleRoot, asset, assertionChecker.getFilesPathExpected());
    }

    /**
     * Check in bundleRoot exists the file for the asset and it have the same content that the file in expectedFilePath
     *
     * @param bundleRoot
     * @param asset
     * @return
     * @throws IOException if the file not exists or not has the right content
     */

    /**
     * Check in bundleRoot exists the file for the asset and it have the right content.
     * it use the {@link AssertionChecker} class to know how to make the asserts, step by step this method
     * do the follow:
     *
     * - Check if the Files get by {@link AssertionChecker#getFile(Object, File)} method exists}, If any of the files not
     * exists throw a {@link AssertionError}.
     * - If {@link AssertionChecker#checkFileContent(Object)} return true then check the content of the file.
     * - If the file to compare have the same ext that <code>expectedFilePath</code> then use the file in <code>expectedFilePath</code>
     * as template otherwise use Use the file path return by {@link AssertionChecker#getFilePathExpected(File)}.
     * - Then populate the template using {@link AssertionChecker#getFileArguments(Object, File)} method,
     * later compare the result with the content of {@link AssertionChecker#getFile(Object, File)},
     * if the content are not equal then throw a {@link AssertionError}
     *
     * @param bundleRoot bundle root directory
     * @param asset asset to be Assert
     * @param expectedFilePath  Template file to populate and compare
     * @return
     * @throws IOException if the file not exists or not has the right content
     *
     * @see FileTestUtil#getFormattedContent(File, Map)
     * @see AssertionChecker
     *
     */
    public static Collection<File> assertBundleFile(
            final File bundleRoot,
            final Object asset,
            final String expectedFilePath) throws IOException {
        return assertBundleFile(bundleRoot, asset, list(expectedFilePath));
    }

    /**
     * Check in bundleRoot exists the file for the asset and it have the right content.
     * it use the {@link AssertionChecker} class to know how to make the asserts, step by step this method
     * do the follow:
     *
     * - Check if the Files get by {@link AssertionChecker#getFile(Object, File)} method exists}, If any of the files not
     * exists throw a {@link AssertionError}.
     * - If {@link AssertionChecker#checkFileContent(Object)} return true then check the content of the file.
     * - If the file to compare have the same ext that one in <code>expectedFilesPath</code> then use it
     * as template otherwise use Use the file path return by {@link AssertionChecker#getFilePathExpected(File)}.
     * - Then populate the template using {@link AssertionChecker#getFileArguments(Object, File)} method,
     * later compare the result with the content of {@link AssertionChecker#getFile(Object, File)},
     * if the content are not equal then throw a {@link AssertionError}
     *
     * @param bundleRoot bundle root directory
     * @param asset asset to be Assert
     * @param expectedFilesPath  Template file to populate and compare
     * @return
     * @throws IOException if the file not exists or not has the right content
     *
     * @see FileTestUtil#getFormattedContent(File, Map)
     * @see AssertionChecker
     *
     */
    public static Collection<File> assertBundleFile(
            final File bundleRoot,
            final Object asset,
            final Collection<String> expectedFilesPath) throws IOException {

        AssertionChecker assertionChecker = null;

        if (Contentlet.class == asset.getClass()) {
            assertionChecker = ((Contentlet) asset).isHost() ? assertions.get(Host.class) : assertions.get(asset.getClass());
        } else {
            assertionChecker = assertions.get(asset.getClass());
        }

        final Collection<File> files = assertionChecker.getFile(asset, bundleRoot);

        for (final File file : files) {

            if (!file.exists()) {
                final String paths = FileUtil.listFilesRecursively(bundleRoot).stream()
                        .map(fileInStream -> fileInStream.getAbsolutePath())
                        .collect(Collectors.joining(", "));
                assertTrue(String.format("File %s, not exists, files: %s", file.getAbsolutePath(), paths), file.exists());
            }

            if (!assertionChecker.checkFileContent(asset)) {
                continue;
            }

            Map<String, Object> arguments = assertionChecker.getFileArguments(asset, file);
            final Collection<String> toRemove = assertionChecker.getRegExToRemove(file);

            final File expectedFile =FileTestUtil.getFileInResources(
                    getFileWithSameExt(expectedFilesPath, file).orElse(assertionChecker.getFilePathExpected(file))
            );

            String fileContentExpected = FileTestUtil.getFormattedContentWithoutSpace(expectedFile, arguments);
            fileContentExpected = removeContent(fileContentExpected, toRemove);

            String fileContent = FileTestUtil.removeSpace(FileTestUtil.getFileContent(file));
            fileContent = removeContent(fileContent, toRemove);

            final String difference = StringUtils.difference(fileContentExpected, fileContent);
            assertEquals(String.format("Fail for %s:\nAsset -> %s \ndifference -> %s\nexpected -> %s\nreal -> %s\n",
                    asset.getClass().getSimpleName(), asset.toString(), difference, fileContentExpected, fileContent),
                    fileContentExpected, fileContent);
        }

        return files;
    }

    private static Optional<String> getFileWithSameExt(Collection<String> expectedFilesPath, final File fileToFind) {
        final String fileExt = getFileExt(fileToFind);
        return expectedFilesPath.stream()
                .map((String filePath) -> new File(filePath))
                .filter((File file) -> getFileExt(file).equals(fileExt))
                .map((File file) -> file.getPath())
                .findFirst();
    }

    private static String getFileExt(final File file) {
        final String fileName = file.getName();
        final int index = fileName.indexOf(".");
        return index != -1 ? fileName.substring(index) : fileName;
    }

    private static String removeContent(
            final String content,
            final Collection<String> toRemoveParams) {

        final Collection<String> toRemove = new ArrayList<>();
        toRemove.addAll(toRemoveParams);
        toRemove.add("<.*></.*>");

        String result = content;

        for (String regex : toRemove) {
            result = result.replaceAll(regex, "");
        }
        return result;
    }

}
