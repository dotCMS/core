package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateAsFileDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class CSVManifestBuilderTest {

    private static String headers = "INCLUDED/EXCLUDED,object type, Id, inode, title, site, folder, excluded by, reason to be evaluated";

    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @DataProvider()
    public static Object[] assets() throws Exception {
        prepare();
        return new TestCase[]{
            getContentTypeTestCase(),
            getTemplateTestCase(),
            getFileTemplateTestCase(),
            getContainerTestCase(),
            getFileContainerTestCase(),
            getFolderTestCase(),
            getHostTestCase(),
            getCategoryTestCase(),
            getLinkTestCase(),
            getWorkflowTestCase(),
            getLanguegeTestCase(),
            getRuleTestCase(),
            getUserTestCase(),
            getContentletTestCase(),
            getRelationshipsTestCase()
        };
    }

    private static TestCase getRelationshipsTestCase() throws DotDataException, DotSecurityException {
        final ContentType parentContentType = new ContentTypeDataGen().nextPersisted();
        final ContentType childContentType = new ContentTypeDataGen().nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(childContentType)
                .parent(parentContentType)
                .nextPersisted();

        final String line = list(PusheableAsset.RELATIONSHIP.getType(), relationship.getInode(),
                "", relationship.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(relationship, line);
    }

    private static TestCase getContentletTestCase() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Host host = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, false);
        final Folder folder = APILocator.getFolderAPI()
                .find(contentlet.getFolder(), systemUser, false);

        final String line = list(PusheableAsset.CONTENTLET.getType(), contentlet.getIdentifier(), contentlet.getInode(),
                contentlet.getName(), host.getName(), folder.getPath())
                .stream().collect(Collectors.joining(","));
        return new TestCase(contentlet, line);
    }

    private static TestCase getUserTestCase() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().nextPersisted();

        final String line = list(PusheableAsset.USER.getType(),
                String.valueOf(user.getUserId()), "", user.getFullName(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(user, line);
    }

    private static TestCase getRuleTestCase() throws DotDataException, DotSecurityException {
        final Rule rule = new RuleDataGen().nextPersisted();
        final Folder folder = APILocator.getFolderAPI()
                .find(rule.getFolder(), APILocator.systemUser(), false);
        final String line = list(PusheableAsset.RULE.getType(),
                String.valueOf(rule.getId()), "", rule.getName(), "", folder.getPath())
                .stream().collect(Collectors.joining(","));
        return new TestCase(rule, line);
    }

    private static TestCase getLanguegeTestCase() {
        final Language language = new LanguageDataGen().nextPersisted();

        final String line = list(PusheableAsset.LANGUAGE.getType(),
                String.valueOf(language.getId()), "", language.getLanguage(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(language, line);
    }

    private static TestCase getTemplateTestCase() {
        final Template template = new TemplateDataGen().nextPersisted();

        final String line = list(PusheableAsset.TEMPLATE.getType(), template.getIdentifier(),
                template.getInode(), template.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(template, line);
    }

    private static TestCase getContainerTestCase() {
        final Container container = new ContainerDataGen().nextPersisted();
        final String line = list(PusheableAsset.CONTAINER.getType(), container.getIdentifier(),
                container.getInode(), container.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(container, line);
    }

    private static TestCase getFileContainerTestCase()
            throws DotDataException, DotSecurityException {
        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen().nextPersisted();

        final String line = list(PusheableAsset.CONTAINER.getType(), fileAssetContainer.getIdentifier(),
                fileAssetContainer.getInode(), fileAssetContainer.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(fileAssetContainer, line);
    }

    private static TestCase getFolderTestCase() {
        final Folder parent = new FolderDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().parent(parent).nextPersisted();
        final String line = list(PusheableAsset.FOLDER.getType(), folder.getIdentifier(), folder.getInode(),
                folder.getTitle(), folder.getHost().getName(), parent.getPath())
                .stream().collect(Collectors.joining(","));
        return new TestCase(folder, line);
    }

    private static TestCase getCategoryTestCase() {
        final Category category = new CategoryDataGen().nextPersisted();
        final String line = list(PusheableAsset.CATEGORY.getType(), category.getInode(), StringPool.BLANK,
                 category.getCategoryName(), StringPool.BLANK, StringPool.BLANK)
                .stream().collect(Collectors.joining(","));
        return new TestCase(category, line);
    }

    private static TestCase getHostTestCase() {
        final Host host = new SiteDataGen().nextPersisted();
        final String line = list(PusheableAsset.SITE.getType(), host.getIdentifier(),
                host.getInode(), host.getTitle(), "System Host", "/")
                .stream().collect(Collectors.joining(","));
        return new TestCase(host, line);
    }

    private static TestCase getWorkflowTestCase() throws DotDataException, DotSecurityException {
        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final String line = list(PusheableAsset.WORKFLOW.getType(), workflowScheme.getId(),
                "", workflowScheme.getName(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(workflowScheme, line);
    }

    private static TestCase getLinkTestCase() throws DotDataException, DotSecurityException {
        final Link link = new LinkDataGen().nextPersisted();
        final Host host = APILocator.getHostAPI()
                .find(link.getHostId(), APILocator.systemUser(), false);
        final String line = list(PusheableAsset.LINK.getType(), link.getIdentifier(),
                link.getInode(), link.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new TestCase(link, line);
    }

    private static TestCase getFileTemplateTestCase()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final FileAssetTemplate template = new TemplateAsFileDataGen().designTemplate(true)
                .host(host).nextPersisted();

        final String line = list(PusheableAsset.TEMPLATE.getType(), template.getIdentifier(),
                template.getInode(), template.getTitle(), "", template.getPath())
                .stream().collect(Collectors.joining(","));
        return new TestCase(template, line);
    }

    private static TestCase getContentTypeTestCase() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Host host = APILocator.getHostAPI().find(contentType.host(), systemUser, false);
        final Folder folder = APILocator.getFolderAPI()
                .find(contentType.folder(), systemUser, false);
        final String line = list(PusheableAsset.CONTENT_TYPE.getType(), contentType.id(),
                "", contentType.name(), host.getName(), folder.getPath())
                .stream().collect(Collectors.joining(","));
        return new TestCase(contentType, line);
    }

    /**
     * Method to test: {@link CSVManifestBuilder#getManifestFile()}
     * when: Create a ManifestBuilder but don't include or exclude anything
     * should: Create the manifest file with just the headers
     */
    @Test(expected = IllegalStateException.class)
    public void emptyManifestFile() throws IOException {
        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
           manifestBuilder.getManifestFile();
        }
    }

    /**
     * Method to test: {@link CSVManifestBuilder#getManifestFile()}
     * when: Create a ManifestBuilder and include a asset
     * should: Create the manifest file with just the headers
     */
    @Test
    @UseDataProvider("assets")
    public void include(final TestCase testCase) throws IOException {
        final String includeReason = "Include testing";

        File manifestFile = null;

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.include(testCase.asset, includeReason);
            manifestFile = manifestBuilder.getManifestFile();
        }

        final List<String> expected = list(headers);
        expected.add("INCLUDED," + testCase.lineExpected + ",," + includeReason);

        assertManifestLines(manifestFile, expected);
    }

    /**
     * Method to test: {@link CSVManifestBuilder#getManifestFile()}
     * when: Create a ManifestBuilder and exclude a asset
     * should: Create the manifest file with just the headers
     */
    @Test
    @UseDataProvider("assets")
    public void exclude(final TestCase testCase) throws IOException {
        final String excludeReason = "Exclude testing";
        final String evaluateReason = "Evaluate testing";

        File manifestFile = null;

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.exclude(testCase.asset, evaluateReason, excludeReason);
            manifestFile = manifestBuilder.getManifestFile();
        }

        final List<String> expected = list(headers);
        expected.add("EXCLUDED," + testCase.lineExpected + "," + excludeReason + "," + evaluateReason);

        assertManifestLines(manifestFile, expected);
    }

    private void assertManifestLines(final File manifestFile, final List<String> expected)
            throws IOException {
        final List<String> lines = getFileLines(manifestFile);

        assertEquals("Manifest\n" + lines.stream()
                .collect(Collectors.joining("\n")), expected.size(), lines.size());

        for (int index = 0; index < lines.size(); index++) {
            final String line = lines.get(index);
            final String lineExpected = expected.get(index);

            assertEquals(lineExpected, line);
        }
    }

    private List<String> getFileLines(final File manifestFile) throws IOException {
        String line;
        final List<String> lines = new ArrayList();
        try (BufferedReader csvReader = new BufferedReader(new FileReader(manifestFile))) {
            while ((line = csvReader.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }

    private static class TestCase {
        ManifestItem asset;
        String lineExpected;

        public TestCase(ManifestItem asset, String linesExpected) {
            this.asset = asset;
            this.lineExpected = linesExpected;
        }
    }

    /**
     * Method to test: {@link CSVManifestBuilder#addMetadata(String, String)}
     * When: Add two headers to a Manifest
     * Should: create two comment line before the headers
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void addHeader() throws DotDataException, DotSecurityException, IOException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String includeReason = "Include testing";
        final TestCase testCase = getContentTypeTestCase();

        File manifestFile = null;

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.addMetadata("header_1", "first test header");
            manifestBuilder.addMetadata("header_2", "second test header");
            manifestBuilder.include(testCase.asset, includeReason);
            manifestFile = manifestBuilder.getManifestFile();
        }

        final String contentTypeLineExpected = "INCLUDED," + testCase.lineExpected + ",," + includeReason;

        final List<String> lines = getFileLines(manifestFile);

        assertEquals(4, lines.size());

        assertEquals("#header_1:first test header", lines.get(0));
        assertEquals("#header_2:second test header", lines.get(1));
        assertEquals(headers, lines.get(2));
        assertEquals(contentTypeLineExpected, lines.get(3));

    }

    /**
     * Method to test: {@link CSVManifestBuilder#addMetadata(String, String)}
     * When: Add a header after include a asset in the manifest
     * Should: throw a {@link IllegalStateException}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test(expected = IllegalStateException.class)
    public void callAddHeaderAfterInclude(){
        final String includeReason = "Include testing";
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.include(contentType, includeReason);
            manifestBuilder.addMetadata("header_1", "first test header");
        }
    }

    /**
     * Method to test: {@link CSVManifestBuilder#addMetadata(String, String)}
     * When: Add a header after exclude a asset in the manifest
     * Should: throw a {@link IllegalStateException}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test(expected = IllegalStateException.class)
    public void callAddHeaderAfterExclude(){
        final String excludedReason = "exclude testing";
        final String evaluateReason = "evaluate testing";
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.exclude(contentType, evaluateReason, excludedReason);
            manifestBuilder.addMetadata("header_1", "first test header");
        }
    }
}
