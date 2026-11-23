package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotcms.publisher.pusher.wrapper.PushContentWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UUIDGenerator;
import org.apache.commons.lang3.tuple.Pair;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentHandlerTest {

    private static final String INPUT_FILE_PATH = "/bundlers-test/file-asset/contentlet.fileAsset.xml";


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    /**
     * Test-Method: {@Link ContentHandler#newXStreamInstance}
     * Given scenario:  We feed XStream with an xml file filled with properties that no longer exist in the current version of FileAsset
     * Expected Result XStream should be able to recover and ignore the unknown un-mapped properties
     * @throws IOException
     */
    @Test
    public void Test_XStream_Deserializer() throws IOException {
        final XStream xStream = XStreamHandler.newXStreamInstance();

        final File file = FileTestUtil.getFileInResources(INPUT_FILE_PATH);
        ContentWrapper wrapper;
        try(final InputStream input = Files.newInputStream(file.toPath())){
            wrapper = (ContentWrapper) xStream.fromXML(input);
        }
        assertNotNull(wrapper);
        final Contentlet contentlet = wrapper.getContent();
        assertTrue(contentlet.isFileAsset());
    }

    /**
     * Method to test: {@link ContentHandler#relateTagsToContent(Contentlet content, Map<String, List<Tag>> tags)}
     * When: Content is pushed which has tags that live on a host not in the target system.
     * Should: The tags should save to the system host
     */
    @Test
    public void TEST_SAVING_TAGS_ON_NON_EXISTING_HOST() throws Exception{
        // Given
        final String nonExistantHost = "non-existing-host" + UUIDGenerator.shorty();
        final String[] tags = {"tag1_" + UUIDGenerator.shorty(),"tag2_" + UUIDGenerator.shorty()};
        final String nonExistantUser = UUIDGenerator.shorty();

        Contentlet contentlet = TestDataUtils.getDotAssetLikeContentlet();

        List<Tag> tagList = new ArrayList<>();

        Arrays.stream(tags).forEach(tag -> {
            Tag t = new Tag();
            t.setTagName(tag);
            t.setHostId(nonExistantHost);
            t.setModDate(new Date());
            t.setUserId(nonExistantUser);
            tagList.add(t);
        });

        Field field = contentlet.getContentType().fields(TagField.class).get(0);

        Map<String,List<Tag>> fieldTags = Map.of(Objects.requireNonNull(field.variable()), tagList);

        // Should not throw an error
        new ContentHandler(new PublisherConfig()).relateTagsToContent(contentlet,fieldTags);


        assertEquals(APILocator.getTagAPI().getTagsByName(tags[0]).size(), 1);

        Tag savedTag = APILocator.getTagAPI().getTagsByName(tags[0]).get(0);
        assertNotNull(savedTag);
        assertEquals(savedTag.getTagName(), tags[0]);
        assertEquals(Host.SYSTEM_HOST, savedTag.getHostId());

        savedTag = APILocator.getTagAPI().getTagsByName(tags[1]).get(0);
        assertNotNull(savedTag);
        assertEquals(savedTag.getTagName(), tags[1]);
        assertEquals(Host.SYSTEM_HOST, savedTag.getHostId());
    }

    /**
     * Method to test: {@link ContentHandler#findUniqueContentMatch(Contentlet, List, Pair)}
     * When: The receiver's index still holds a document for a unique value whose contentlet is gone from the
     * database -- the "ghost" left behind by an index/database desynchronization -- and a bundle arrives carrying
     * that same unique value.
     * Should: Skip the stale match and hand back the incoming content untouched, so the bundle is imported as new
     * content instead of failing on this -- and on every single retry.
     */
    @Test
    public void TEST_STALE_INDEX_DOCUMENT_DOES_NOT_BLOCK_UNIQUE_FIELD_MATCH() throws Exception {
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final String uniqueVarName = "uniqueField" + UUIDGenerator.shorty().replace("-", "");
        final ContentType contentType = new ContentTypeDataGen()
                .field(new FieldDataGen().type(TextField.class).velocityVarName(uniqueVarName).unique(true).next())
                .nextPersisted();
        final String uniqueValue = "ghost-" + UUIDGenerator.shorty();

        // A contentlet that gets indexed the regular way...
        final Contentlet ghost = new ContentletDataGen(contentType)
                .languageId(languageId)
                .setProperty(uniqueVarName, uniqueValue)
                .nextPersisted();

        // ...and is then wiped from the database behind the API's back, leaving its index document orphaned
        new DotConnect().setSQL("delete from contentlet_version_info where identifier = ?")
                .addParam(ghost.getIdentifier()).loadResult();
        new DotConnect().setSQL("delete from contentlet where identifier = ?")
                .addParam(ghost.getIdentifier()).loadResult();
        CacheLocator.getContentletCache().remove(ghost.getInode());
        CacheLocator.getIdentifierCache().removeFromCacheByVersionable(ghost);

        // The ghost must really be there, otherwise this test would pass for the wrong reason
        final List<ContentletSearch> staleMatches = APILocator.getContentletAPI().searchIndex(
                "+structureInode:" + contentType.id() + " +working:true", 0, -1, "score",
                APILocator.systemUser(), false);
        assertFalse("The stale index document was not created; the scenario under test is not in place",
                staleMatches.isEmpty());
        assertNull("The ghost must no longer be resolvable from the database",
                APILocator.getContentletAPI().find(ghost.getInode(), APILocator.systemUser(), false));

        final Contentlet fromBundle = new Contentlet();
        fromBundle.setContentTypeId(contentType.id());
        fromBundle.setIdentifier(UUIDGenerator.generateUuid());
        fromBundle.setInode(UUIDGenerator.generateUuid());
        fromBundle.setLanguageId(languageId);
        fromBundle.setStringProperty(uniqueVarName, uniqueValue);

        final Contentlet result = new ContentHandler(new PublisherConfig()).findUniqueContentMatch(fromBundle,
                FieldsCache.getFieldsByStructureInode(contentType.id()), Pair.of(languageId, languageId));

        assertSame("The stale index document must be ignored, not matched", fromBundle, result);
    }

    /**
     * Method to test: {@link ContentHandler#handle(File)}
     * Given scenario: A bundle whose content item has an inode that is a relative path
     * ({@code ../<name>}) rather than a UUID, and a file at the folder that inode points to.
     * Expected Result: The bundle is refused, and nothing is written next to the asset store, where
     * that inode would place the item's binary folder.
     */
    @Test
    public void TEST_BUNDLE_CONTENT_WITH_PATH_AS_INODE_IS_REFUSED() throws Exception {
        final ContentType contentType = binaryContentType();
        final File bundleFolder = newBundleFolder();
        final String probeName = "ctprobe" + UUIDGenerator.shorty();
        final File probeFolder = new File(
                new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()).getParentFile(), probeName);
        try {
            final Contentlet source = persistedSource(contentType, "bundle binary");
            final String inode = "../" + probeName;
            writeContentXml(bundleFolder, source, inode, UUIDGenerator.generateUuid(), null);
            // The file the receiver would pick up as the item's binary, at <bundle>/<name>/<field>
            FileUtils.writeStringToFile(new File(bundleFolder, probeName + "/" + BINARY_VAR + "/marker.txt"),
                    "marker", StandardCharsets.UTF_8);

            assertThrows(DotPublishingException.class, () -> handlerFor(bundleFolder).handle(bundleFolder));

            assertFalse("No folder may be created outside the asset store", probeFolder.exists());
            assertTrue("The item must not be saved",
                    new DotConnect().setSQL("select inode from contentlet where inode = ?")
                            .addParam(inode).loadResults().isEmpty());
        } finally {
            FileUtils.deleteQuietly(probeFolder);
            FileUtils.deleteQuietly(bundleFolder);
            ContentTypeDataGen.remove(contentType);
        }
    }

    /**
     * Method to test: {@link ContentHandler#handle(File)}
     * Given scenario: A bundle whose content item has a valid inode but an identifier that is a
     * relative path rather than a UUID.
     * Expected Result: The bundle is refused and no identifier is created for it.
     */
    @Test
    public void TEST_BUNDLE_CONTENT_WITH_PATH_AS_IDENTIFIER_IS_REFUSED() throws Exception {
        final ContentType contentType = binaryContentType();
        final File bundleFolder = newBundleFolder();
        try {
            final Contentlet source = persistedSource(contentType, "bundle binary");
            final String identifier = "../" + UUIDGenerator.shorty();
            writeContentXml(bundleFolder, source, UUIDGenerator.generateUuid(), identifier, null);

            assertThrows(DotPublishingException.class, () -> handlerFor(bundleFolder).handle(bundleFolder));

            assertTrue("No identifier may be created",
                    new DotConnect().setSQL("select id from identifier where id = ?")
                            .addParam(identifier).loadResults().isEmpty());
        } finally {
            FileUtils.deleteQuietly(bundleFolder);
            ContentTypeDataGen.remove(contentType);
        }
    }

    /**
     * Method to test: {@link ContentHandler#handle(File)}
     * Given scenario: A bundle whose content item does not carry its binary under the bundle's
     * {@code assets} folder, while the item's binary value in the content XML names a file
     * elsewhere on the receiver.
     * Expected Result: The item is imported without a binary: only files the bundle itself carries
     * are used, so the named file is not copied into the asset store.
     */
    @Test
    public void TEST_BUNDLE_BINARY_VALUE_OUTSIDE_THE_BUNDLE_IS_NOT_IMPORTED() throws Exception {
        final ContentType contentType = binaryContentType();
        final File bundleFolder = newBundleFolder();
        final File outsideFolder = Files.createTempDirectory("ct-outside").toFile();
        try {
            final File outsideFile = new File(outsideFolder, "outside.txt");
            FileUtils.writeStringToFile(outsideFile, "outside the bundle", StandardCharsets.UTF_8);
            final Contentlet source = persistedSource(contentType, "bundle binary");
            final String inode = UUIDGenerator.generateUuid();
            writeContentXml(bundleFolder, source, inode, UUIDGenerator.generateUuid(), outsideFile);
            new File(bundleFolder, "assets").mkdirs();

            handlerFor(bundleFolder).handle(bundleFolder);

            final Contentlet imported = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
            assertNotNull("The item itself must still be imported", imported);
            assertNull("A file outside the bundle must not become the item's binary",
                    imported.getBinary(BINARY_VAR));
        } finally {
            FileUtils.deleteQuietly(outsideFolder);
            FileUtils.deleteQuietly(bundleFolder);
            ContentTypeDataGen.remove(contentType);
        }
    }

    /**
     * Method to test: {@link ContentHandler#handle(File)}
     * Given scenario: An ordinary bundle: the content item has a UUID inode and carries its binary
     * under {@code assets/<i0>/<i1>/<inode>/<field>/}.
     * Expected Result: The item is imported and its binary, read from the asset store, has the
     * content of the file in the bundle.
     */
    @Test
    public void TEST_BUNDLE_CONTENT_WITH_BINARY_IS_IMPORTED() throws Exception {
        final ContentType contentType = binaryContentType();
        final File bundleFolder = newBundleFolder();
        try {
            final Contentlet source = persistedSource(contentType, "bundle binary");
            final String inode = UUIDGenerator.generateUuid();
            writeContentXml(bundleFolder, source, inode, UUIDGenerator.generateUuid(), null);
            FileUtils.writeStringToFile(new File(bundleFolder, "assets/" + inode.charAt(0) + "/"
                            + inode.charAt(1) + "/" + inode + "/" + BINARY_VAR + "/from-bundle.txt"),
                    "carried by the bundle", StandardCharsets.UTF_8);

            handlerFor(bundleFolder).handle(bundleFolder);

            final Contentlet imported = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
            assertNotNull(imported);
            final File binary = imported.getBinary(BINARY_VAR);
            assertNotNull("The binary carried by the bundle must be imported", binary);
            assertEquals("carried by the bundle", FileUtils.readFileToString(binary, StandardCharsets.UTF_8));
            assertTrue("The binary must be stored in the asset store",
                    binary.getCanonicalPath().startsWith(new File(
                            APILocator.getFileAssetAPI().getRealAssetsRootPath()).getCanonicalPath() + File.separator));
        } finally {
            FileUtils.deleteQuietly(bundleFolder);
            ContentTypeDataGen.remove(contentType);
        }
    }

    private static final String BINARY_VAR = "upload";

    private static ContentType binaryContentType() {
        return new ContentTypeDataGen()
                .field(new FieldDataGen().type(TextField.class).velocityVarName("title").next())
                .field(new FieldDataGen().type(BinaryField.class).velocityVarName(BINARY_VAR).next())
                .nextPersisted();
    }

    /**
     * Creates and saves the item the test bundles are made from, with a small text file as its
     * binary.
     */
    private static Contentlet persistedSource(final ContentType contentType, final String binaryText)
            throws Exception {
        final File binary = new File(Files.createTempDirectory("ct-source").toFile(), "source.txt");
        FileUtils.writeStringToFile(binary, binaryText, StandardCharsets.UTF_8);
        final Contentlet saved = new ContentletDataGen(contentType)
                .setProperty("title", "bundle item " + UUIDGenerator.shorty())
                .setProperty(BINARY_VAR, binary)
                .nextPersisted();
        return APILocator.getContentletAPI().find(saved.getInode(), APILocator.systemUser(), false);
    }

    /**
     * A folder under the receiver's bundle directory, where received bundles are extracted.
     */
    private static File newBundleFolder() {
        final File folder = new File(ConfigUtils.getBundlePath(), "ct-bundle-" + UUIDGenerator.shorty());
        folder.mkdirs();
        return folder;
    }

    private static ContentHandler handlerFor(final File bundleFolder) {
        if (APILocator.getPublisherAPI().getFilterDescriptorByKey("") == null) {
            new FilterDescriptorDataGen().clearFilterList(false).nextPersisted();
        }
        final PublisherConfig config = new PublisherConfig();
        config.setId(bundleFolder.getName() + ".tar.gz");
        return new ContentHandler(config);
    }

    /**
     * Writes the content XML for {@code source} the way the sender does, then replaces the item's
     * inode and identifier in it, as an edited bundle would.
     *
     * @param binaryValue if set, the file the binary field's value names in the XML
     */
    private static void writeContentXml(final File bundleFolder, final Contentlet source,
            final String inode, final String identifier, final File binaryValue) throws Exception {
        final File sourceBinary = source.getBinary(BINARY_VAR);
        final PushContentWrapper wrapper = new PushContentWrapper();
        wrapper.setContent(source);
        wrapper.setInfo(APILocator.getVersionableAPI()
                .getContentletVersionInfo(source.getIdentifier(), source.getLanguageId()).orElseThrow());
        wrapper.setId(APILocator.getIdentifierAPI().find(source.getIdentifier()));
        wrapper.setTags(new ArrayList<>());
        wrapper.setTree(new ArrayList<>());
        wrapper.setMultiTree(new ArrayList<>());
        wrapper.setCategories(new ArrayList<>());
        wrapper.setContentTags(new HashMap<>());
        wrapper.setBinariesMetadata(new HashMap<>());
        wrapper.setOperation(PushPublisherConfig.Operation.PUBLISH);
        wrapper.setLanguage(APILocator.getLanguageAPI().getLanguage(source.getLanguageId()));

        String xml = XStreamHandler.newXStreamInstance().toXML(wrapper);
        if (binaryValue != null) {
            xml = xml.replace(sourceBinary.getAbsolutePath(), binaryValue.getAbsolutePath());
        }
        xml = xml.replace(source.getInode(), inode).replace(source.getIdentifier(), identifier);
        FileUtils.writeStringToFile(new File(bundleFolder, "working/1/0-content.content.xml"), xml,
                StandardCharsets.UTF_8);
    }

}
