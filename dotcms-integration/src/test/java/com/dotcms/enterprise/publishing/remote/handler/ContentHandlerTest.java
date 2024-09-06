package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotcms.util.xstream.XStreamHandler.TrustedListMatcher;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UUIDGenerator;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentHandlerTest {

    private static final String INPUT_FILE_PATH = "/bundlers-test/file-asset/contentlet.fileAsset.xml";


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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

    @Test
    public void Test_TrustedListMatcher() {
        // Classes that should match the patterns
        Class<?> allowedClass1 = java.util.ArrayList.class;
        Class<?> allowedClass2 = java.lang.String.class;
        Class<?> allowedClass3 = com.google.common.collect.Lists.class;

        // Classes that should not match the patterns
        Class<?> disallowedClass1 = java.nio.file.Paths.class;
        Class<?> disallowedClass2 = org.junit.jupiter.api.Test.class;

        // Verifications
        assertTrue(TrustedListMatcher.matches(allowedClass1));
        assertTrue(TrustedListMatcher.matches(allowedClass2));
        assertTrue(TrustedListMatcher.matches(allowedClass3));

        assertFalse(TrustedListMatcher.matches(disallowedClass1));
        assertFalse(TrustedListMatcher.matches(disallowedClass2));
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





}
