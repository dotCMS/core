package com.dotcms.security.multipart;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.Test;

public class ContentDispositionFileNameParserTest {

    /**
     * This method test parser
     * Given scenario: Happy path testing when there a content-disposition
     * Expected Result: recovers the filename
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_happy_path()
            throws DotDataException, DotSecurityException {

        final String filenameHeader = "Content-Disposition: attachment; filename=\"filename.jpg\"";
        final String filename = ContentDispositionFileNameParser.parse(filenameHeader);

        Assert.assertEquals("filename.jpg", filename);
    }
}
