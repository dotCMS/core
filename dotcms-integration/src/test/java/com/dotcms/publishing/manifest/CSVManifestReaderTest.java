package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
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
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class CSVManifestReaderTest {
    private static String headers = "INCLUDED/EXCLUDED,object type, Id, inode, title, site, folder, excluded by, reason to be evaluated";

    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @DataProvider()
    public static Object[] assets() throws Exception {
        prepare();
        return new CSVManifestReaderTest.TestCase[]{
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

    private static CSVManifestReaderTest.TestCase getRelationshipsTestCase() throws DotDataException, DotSecurityException {
        final ContentType parentContentType = new ContentTypeDataGen().nextPersisted();
        final ContentType childContentType = new ContentTypeDataGen().nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(childContentType)
                .parent(parentContentType)
                .nextPersisted();

        final String line = list(PusheableAsset.RELATIONSHIP.getType(), relationship.getInode(),
                "", relationship.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(relationship, line);
    }

    private static CSVManifestReaderTest.TestCase getContentletTestCase() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final Host host = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, false);
        final Folder folder = APILocator.getFolderAPI()
                .find(contentlet.getFolder(), systemUser, false);

        final String line = list(PusheableAsset.CONTENTLET.getType(), contentlet.getIdentifier(), contentlet.getInode(),
                contentlet.getName(), host.getName(), folder.getPath())
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(contentlet, line);
    }

    private static CSVManifestReaderTest.TestCase getUserTestCase() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().nextPersisted();

        final String line = list(PusheableAsset.USER.getType(),
                String.valueOf(user.getUserId()), "", user.getFullName(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(user, line);
    }

    private static CSVManifestReaderTest.TestCase getRuleTestCase() throws DotDataException, DotSecurityException {
        final Rule rule = new RuleDataGen().nextPersisted();
        final Folder folder = APILocator.getFolderAPI()
                .find(rule.getFolder(), APILocator.systemUser(), false);
        final String line = list(PusheableAsset.RULE.getType(),
                String.valueOf(rule.getId()), "", rule.getName(), "", folder.getPath())
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(rule, line);
    }

    private static CSVManifestReaderTest.TestCase getLanguegeTestCase() {
        final Language language = new LanguageDataGen().nextPersisted();

        final String line = list(PusheableAsset.LANGUAGE.getType(),
                String.valueOf(language.getId()), "", language.getLanguage(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(language, line);
    }

    private static CSVManifestReaderTest.TestCase getTemplateTestCase() {
        final Template template = new TemplateDataGen().nextPersisted();

        final String line = list(PusheableAsset.TEMPLATE.getType(), template.getIdentifier(),
                template.getInode(), template.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(template, line);
    }

    private static CSVManifestReaderTest.TestCase getContainerTestCase() {
        final Container container = new ContainerDataGen().nextPersisted();
        final String line = list(PusheableAsset.CONTAINER.getType(), container.getIdentifier(),
                container.getInode(), container.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(container, line);
    }

    private static CSVManifestReaderTest.TestCase getFileContainerTestCase()
            throws DotDataException, DotSecurityException {
        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen().nextPersisted();

        final String line = list(PusheableAsset.CONTAINER.getType(), fileAssetContainer.getIdentifier(),
                fileAssetContainer.getInode(), fileAssetContainer.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(fileAssetContainer, line);
    }

    private static CSVManifestReaderTest.TestCase getFolderTestCase() {
        final Folder parent = new FolderDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().parent(parent).nextPersisted();
        final String line = list(PusheableAsset.FOLDER.getType(), folder.getIdentifier(), folder.getInode(),
                folder.getTitle(), folder.getHost().getName(), parent.getPath())
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(folder, line);
    }

    private static CSVManifestReaderTest.TestCase getCategoryTestCase() {
        final Category category = new CategoryDataGen().nextPersisted();
        final String line = list(PusheableAsset.CATEGORY.getType(), category.getIdentifier(),
                category.getInode(), category.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(category, line);
    }

    private static CSVManifestReaderTest.TestCase getHostTestCase() {
        final Host host = new SiteDataGen().nextPersisted();
        final String line = list(PusheableAsset.SITE.getType(), host.getIdentifier(),
                host.getInode(), host.getTitle(), "System Host", "/")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(host, line);
    }

    private static CSVManifestReaderTest.TestCase getWorkflowTestCase() throws DotDataException, DotSecurityException {
        final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
        final String line = list(PusheableAsset.WORKFLOW.getType(), workflowScheme.getId(),
                "", workflowScheme.getName(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(workflowScheme, line);
    }

    private static CSVManifestReaderTest.TestCase getLinkTestCase() throws DotDataException, DotSecurityException {
        final Link link = new LinkDataGen().nextPersisted();
        final Host host = APILocator.getHostAPI()
                .find(link.getHostId(), APILocator.systemUser(), false);
        final String line = list(PusheableAsset.LINK.getType(), link.getIdentifier(),
                link.getInode(), link.getTitle(), "", "")
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(link, line);
    }

    private static CSVManifestReaderTest.TestCase getFileTemplateTestCase()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final FileAssetTemplate template = new TemplateAsFileDataGen().designTemplate(true)
                .host(host).nextPersisted();

        final String line = list(PusheableAsset.TEMPLATE.getType(), template.getIdentifier(),
                template.getInode(), template.getTitle(), "", template.getPath())
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(template, line);
    }

    private static CSVManifestReaderTest.TestCase getContentTypeTestCase() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Host host = APILocator.getHostAPI().find(contentType.host(), systemUser, false);
        final Folder folder = APILocator.getFolderAPI()
                .find(contentType.folder(), systemUser, false);
        final String line = list(PusheableAsset.CONTENT_TYPE.getType(), contentType.id(),
                "", contentType.name(), host.getName(), folder.getPath())
                .stream().collect(Collectors.joining(","));
        return new CSVManifestReaderTest.TestCase(contentType, line);
    }



    /**
     * Method to test: {@link CSVManifestReader#getIncludedAssets()}
     * when: Create a Manifest File and include a asset and excluded another on
     * should: Return just the Included one
     */
    @Test
    @UseDataProvider("assets")
    public void include(final CSVManifestReaderTest.TestCase testCase) throws IOException {
        final String includeReason = ManifestReason.INCLUDE_BY_USER.getMessage();

        File manifestFile = null;

        final Host systemHost = APILocator.systemHost();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.include(testCase.asset, includeReason);
            manifestBuilder.exclude(systemHost, includeReason,
                    ManifestReason.EXCLUDE_SYSTEM_OBJECT.getMessage());
            manifestFile = manifestBuilder.getManifestFile();
        }

        final ManifestReader manifestReader = new CSVManifestReader(manifestFile);
        final Collection<ManifestInfo> includedAssets = manifestReader.getIncludedAssets();
        assertEquals(1, includedAssets.size());

        final ManifestInfo assetIncluded = includedAssets.iterator().next();
        assertEquals(testCase.asset.getManifestInfo(), assetIncluded);

        final Collection<ManifestInfo> assets = manifestReader.getAssets();
        assertEquals(2, assets.size());

        for (final ManifestInfo asset : assets) {
            assertTrue(asset.equals(assetIncluded) || asset.equals(systemHost.getManifestInfo()));
        }
    }

    /**
     * Method to test: {@link CSVManifestReader#getAssets(ManifestReason)}
     * when: Create a Manifest File and include two asset with different reason
     * should: Return just the asset with the reason requested
     */
    @Test
    public void getAssets() throws IOException {
        final String includeReason1 = ManifestReason.INCLUDE_BY_USER.getMessage();
        final String includeReason2 = ManifestReason.INCLUDE_AUTOMATIC_BY_DOTCMS.getMessage();

        File manifestFile = null;

        final ContentType contentType1 = new ContentTypeDataGen().nextPersisted();
        final ContentType contentType2 = new ContentTypeDataGen().nextPersisted();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.include(contentType1, includeReason1);
            manifestBuilder.include(contentType2, includeReason2);
            manifestFile = manifestBuilder.getManifestFile();
        }

        final ManifestReader manifestReader = new CSVManifestReader(manifestFile);
        final Collection<ManifestInfo> includedAssets = manifestReader.getAssets(ManifestReason.INCLUDE_BY_USER);
        assertEquals(1, includedAssets.size());

        final ManifestInfo assetIncluded = includedAssets.iterator().next();
        assertEquals(contentType1.getManifestInfo(), assetIncluded);
    }

    /**
     * Method to test: {@link CSVManifestReader#getAssets(ManifestReason)}
     * when: Create a Manifest File and include two asset with different reason
     * should: Return just the asset with the reason requested
     */
    @Test
    public void createCSVManifestReaderWithInputStream() throws FileNotFoundException {
        final String includeReason1 = ManifestReason.INCLUDE_BY_USER.getMessage();
        final String includeReason2 = ManifestReason.INCLUDE_AUTOMATIC_BY_DOTCMS.getMessage();

        File manifestFile = null;

        final ContentType contentType1 = new ContentTypeDataGen().nextPersisted();
        final ContentType contentType2 = new ContentTypeDataGen().nextPersisted();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.include(contentType1, includeReason1);
            manifestBuilder.include(contentType2, includeReason2);
            manifestFile = manifestBuilder.getManifestFile();
        }

        final ManifestReader manifestReader = new CSVManifestReader(new FileReader(manifestFile));
        final Collection<ManifestInfo> includedAssets = manifestReader.getAssets(ManifestReason.INCLUDE_BY_USER);
        assertEquals(1, includedAssets.size());

        final ManifestInfo assetIncluded = includedAssets.iterator().next();
        assertEquals(contentType1.getManifestInfo(), assetIncluded);
    }

    /**
     * Method to test: {@link CSVManifestReader#getExcludedAssets()} ()}
     * when: Create a Manifest File and include a asset and excluded another on
     * should: Return just the Excluded one
     */
    @Test
    @UseDataProvider("assets")
    public void exclude(final CSVManifestReaderTest.TestCase testCase) throws IOException {
        final String includeReason = ManifestReason.INCLUDE_BY_USER.getMessage();

        File manifestFile = null;

        final Language language = new LanguageDataGen().nextPersisted();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.exclude(testCase.asset, includeReason,
                    ManifestReason.EXCLUDE_BY_FILTER.getMessage());
            manifestBuilder.include(language, includeReason);
            manifestFile = manifestBuilder.getManifestFile();
        }

        final ManifestReader manifestReader = new CSVManifestReader(manifestFile);
        final Collection<ManifestInfo> excludedAssets = manifestReader.getExcludedAssets();
        assertEquals(1, excludedAssets.size());

        final ManifestInfo assetExcluded = excludedAssets.iterator().next();
        assertEquals(testCase.asset.getManifestInfo(), assetExcluded);

        final Collection<ManifestInfo> assets = manifestReader.getAssets();
        assertEquals(2, assets.size());

        for (final ManifestInfo asset : assets) {
            assertTrue(asset.equals(assetExcluded) || asset.equals(language.getManifestInfo()));
        }
    }

    private String getManifestFileLine(final ManifestItem manifestItem) {
        final ManifestInfo manifestInfo = manifestItem.getManifestInfo();

        return list(
                manifestInfo.objectType(),
                manifestInfo.id(),
                manifestInfo.inode(),
                manifestInfo.title(),
                manifestInfo.site(),
                manifestInfo.folder()).stream().collect(Collectors.joining(","));
    }

    /**
     * Method to test: {@link CSVManifestReader#getAssets(ManifestReason)}
     * when: Create a ManifestBuilder but don't include or exclude anything
     * should: Create the manifest file with just the headers
     */
    @Test()
    public void emptyManifestFile() throws IOException {
        final File emptyManifestFile = FileUtil.createTemporaryFile("emptyManifestFile");
        FileUtils.write(emptyManifestFile, headers, StandardCharsets.UTF_8);

        final ManifestReader manifestReader = new CSVManifestReader(emptyManifestFile);

        assertTrue(manifestReader.getIncludedAssets().isEmpty());
        assertTrue(manifestReader.getExcludedAssets().isEmpty());
        assertTrue(manifestReader.getAssets().isEmpty());
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
     * Method to test: {@link CSVManifestReader#getMetadata(String)}
     * When: Create a manifest with to Metadata header
     * Should: return the right value
     */
    @Test()
    public void getMetadata(){
        File manifestFile = null;

        final Language language = new LanguageDataGen().nextPersisted();

        try(final CSVManifestBuilder manifestBuilder = new CSVManifestBuilder()) {
            manifestBuilder.addMetadata("header_1", "First header");
            manifestBuilder.addMetadata("header_2", "Second header");
            manifestBuilder.include(language, ManifestReason.INCLUDE_AUTOMATIC_BY_DOTCMS.getMessage());
            manifestFile = manifestBuilder.getManifestFile();
        }

        final ManifestReader manifestReader = new CSVManifestReader(manifestFile);
        final Collection<ManifestInfo> includedAssets = manifestReader.getIncludedAssets();
        assertEquals(1, includedAssets.size());

        final ManifestInfo assetExcluded = includedAssets.iterator().next();
        assertEquals(language.getManifestInfo(), assetExcluded);

        assertEquals("First header", manifestReader.getMetadata("header_1"));
        assertEquals("Second header", manifestReader.getMetadata("header_2"));
    }
}
