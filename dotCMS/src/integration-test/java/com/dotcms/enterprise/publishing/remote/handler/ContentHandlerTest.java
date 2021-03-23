package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotcms.test.util.FileTestUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.Test;

public class ContentHandlerTest {

    static final String INPUT_FILE_PATH = "/bundlers-test/file-asset/contentlet.fileAsset.xml";

    /**
     * Test-Method: {@Link ContentHandler#newXStreamInstance}
     * Given scenario:  We feed XStream with an xml file filled with properties that no longer exist in the current version of FileAsset
     * Expected Result XStream should be able to recover and ignore the unknown un-mapped properties
     * @throws IOException
     */
    @Test
    public void Test_XStream_Deserializer() throws IOException {
        final XStream xStream = ContentHandler.newXStreamInstance();

        final File file = FileTestUtil.getFileInResources(INPUT_FILE_PATH);
        ContentWrapper wrapper;
        try(final InputStream input = Files.newInputStream(file.toPath())){
            wrapper = (ContentWrapper) xStream.fromXML(input);
        }
        assertNotNull(wrapper);
        final Contentlet contentlet = wrapper.getContent();
        assertTrue(contentlet.isFileAsset());
    }


}
