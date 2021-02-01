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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.dotcms.util.CollectionsUtils.*;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;

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

    public static String getFormattedContent(final File file, final Map<String, Object> arguments) throws IOException {
        final byte[] bytes = FileUtil.getBytes(file);
        final String fileContent = new String(bytes);
        return StringFormatterTestUtil.format(fileContent, arguments);
    }

    public static String getFormattedContentWithoutSpace(final File file, final Map<String, Object> arguments) throws IOException {
        return removeSpace(getFormattedContent(file, arguments));
    }

    public static String removeSpace(final String message){
        return message.replaceAll("\n[ \\t\\r\\n\\v\\f]*", "");
    }

    public static String getFileContent(final File file) throws IOException {
        final byte[] bytes = FileUtil.getBytes(file);
        return new String(bytes);
    }

    public static File getFileInResources(final String path){
        return new File(FileTestUtil.class.getResource(path).getFile());
    }

    public static Collection<File> assertBundleFile(
            final File bundleRoot,
            final Object asset) throws IOException {
        final AssertionChecker assertionChecker = assertions.get(asset.getClass());

        return assertBundleFile(bundleRoot, asset, assertionChecker.getFilesPathExpected());
    }


    public static Collection<File> assertBundleFile(
            final File bundleRoot,
            final Object asset,
            final String expectedFilePath) throws IOException {
        return assertBundleFile(bundleRoot, asset, list(expectedFilePath));
    }

    public static Collection<File> assertBundleFile(
            final File bundleRoot,
            final Object asset,
            final Collection<String> expectedFilesPath) throws IOException {

        final AssertionChecker assertionChecker = assertions.get(asset.getClass());
        final Collection<File> files = assertionChecker.getFile(asset, bundleRoot);

        for (final File file : files) {
            assertTrue(String.format("File %s, not exists", file.getAbsolutePath()), file.exists());

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

            assertEquals(String.format("Fail for %s: %s", asset.getClass().getName(),  asset.toString()),
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

    public static String getFileExt(final File file) {
        final String fileName = file.getName();
        final int index = fileName.indexOf(".");
        return index != -1 ? fileName.substring(index) : fileName;
    }


    @NotNull
    private static String removeContent(String content, Collection<String> toRemove) {
        for (String regex : toRemove) {
            content = content.replaceAll(regex, "");
        }
        return content;
    }

}
