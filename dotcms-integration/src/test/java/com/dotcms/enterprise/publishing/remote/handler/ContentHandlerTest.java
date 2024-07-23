package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotcms.util.xstream.XStreamHandler.TrustedListMatcher;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

}
