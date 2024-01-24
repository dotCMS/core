package com.dotcms.storage;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.FileUtil;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BinaryBlockFilJoinerImplTest {


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link BinaryBlockFileJoinerImpl#join(byte[], int, int)}
     * Given Scenario: Adds less than 1000 bytes
     * ExpectedResult: should return the binary and not a file
     *
     */
    @Test
    public void Test_Easy_Path() throws IOException {

        final File file = FileUtil.createTemporaryFile("dot-db-storage-recovery", ".tmp", true);
        final BinaryBlockFileJoinerImpl binaryBlockFileJoiner = new BinaryBlockFileJoinerImpl(file, 1000);

        final String s1 = "one,two,three,four,five";
        binaryBlockFileJoiner.join(s1.getBytes(StandardCharsets.UTF_8), 0, s1.length());

        final String s2 = "six,seven,eight,nine,ten";
        binaryBlockFileJoiner.join(s2.getBytes(StandardCharsets.UTF_8), 0, s2.length());

        final byte [] bytes = binaryBlockFileJoiner.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertEquals(new String(bytes,StandardCharsets.UTF_8).trim(),s1+s2);
    }

    /**
     * Method to test: {@link BinaryBlockFileJoinerImpl#join(byte[], int, int)}
     * Given Scenario: Adds less than 1000 bytes
     * ExpectedResult: should return the binary and not a file
     *
     */
    @Test
    public void Test_Flush() throws IOException {

        final File file = FileUtil.createTemporaryFile("dot-db-storage-recovery", ".tmp", true);
        final BinaryBlockFileJoinerImpl binaryBlockFileJoiner = new BinaryBlockFileJoinerImpl(file, 1000);
        final StringBuilder builder = new StringBuilder();
        for (int i=0;i<100;++i) {
            final String s1 = "one,two,three,four,five,six,seven,eight,nine,ten,one,two,three,four,five,six,seven,eight,nine,ten,one,two,three,four," +
                    "five,six,seven,eight,nine,ten,one,two,three,four,five,six,seven,eight,nine,ten" + "five,six,seven,eight,nine,ten,one,two,three,four,five,six,seven,eight,nine,ten" +
                    "five,six,seven,eight,nine,ten,one,two,three,four,five,six,seven,eight,nine,ten";
            builder.append(s1);
            binaryBlockFileJoiner.join(s1.getBytes(StandardCharsets.UTF_8), 0, s1.length());
        }

        final byte [] bytes = binaryBlockFileJoiner.getBytes(); //
        Assert.assertNull(bytes);

        binaryBlockFileJoiner.flush();
        binaryBlockFileJoiner.close();

        Assert.assertTrue(file.exists());
        final String fileContent = Files.asCharSource(file, StandardCharsets.UTF_8).read();

        Assert.assertEquals(fileContent, builder.toString());
    }

}
