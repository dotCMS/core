package com.dotcms.security.multipart;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class BoundedBufferedReaderTest {

    private String LINES = "line 1 \n" +
            "line 2 \n" +
            "line 3 \n" +
            "line 4 \n" +
            "line 5 \n";
    /**
     * This method test readLine
     * Given scenario: Happy path testing a string with 5 lines
     * Expected Result: 5 lines are being read
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_happy_path()
            throws DotDataException, DotSecurityException, IOException {

        final BoundedBufferedReader boundedBufferedReader = new BoundedBufferedReader(new StringReader(LINES));
        int count = 0;
        String line = null;
        while ((line = boundedBufferedReader.readLine()) != null) {
            count++;
        }
        Assert.assertEquals(5, count);
    }
}
