package com.dotmarketing.util;

import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class HashBuilderTest {

    @Test
    public void Test_BuildUnixHash () throws Exception {

        final String text1 = "this is a test";
        final HashBuilder hashBuilder = Encryptor.Hashing.sha256();
        final String hash1 =  hashBuilder.append(text1.getBytes(StandardCharsets.UTF_8)).buildUnixHash();

        final String subtext1 = "this ";
        final String subtext2 = "is a test";
        final String text2 = subtext1 + subtext2;
        final HashBuilder hashBuilder2 = Encryptor.Hashing.sha256();
        final String hash2 =  hashBuilder2.append(text2.getBytes(StandardCharsets.UTF_8)).buildUnixHash();

        Assert.assertEquals("both should be the same", hash1, hash2);

        final HashBuilder hashBuilder3 = Encryptor.Hashing.sha256();
        final String hash3 =  hashBuilder3.append(subtext1.getBytes(StandardCharsets.UTF_8)).append(subtext2.getBytes(StandardCharsets.UTF_8)).buildUnixHash();

        Assert.assertEquals("both should be the same", hash3, hash2);
        Assert.assertEquals("both should be the same", hash1, hash3);
    }


    @Test
    public void Test_Identical_Files_Different_Name_Location_BuildUnixHash () throws Exception {
        final File file1 = staticTestFile();
        final File file2 = staticTestFile();
        Assert.assertNotSame(file1, file2);
        Assert.assertEquals(FileUtil.sha256toUnixHash(file1),FileUtil.sha256toUnixHash(file2));
    }

    private static File staticTestFile() throws Exception {
        final String data = "{\"content\":\"this is the content of the file\\\\n\",\"contentType\":\"text/plain; charset=ISO-8859-1\",\"fileSize\":31,\"isImage\":false,\"length\":31,\"modDate\":1619466298000,\"name\":\"hello.txt\",\"path\":\"c/b/cb9340f6-324d-4766-b95d-1c78684d0618/fileAsset/hello.txt\",\"sha256\":\"3ab847baadfebc8ff1c36464ce67009e4a3e3092fe306c90bdb2a6a0c321a00b\",\"title\":\"hello.txt\"}";
        final File temp = File.createTempFile("testFile", ".bin");
        try (FileOutputStream output = new FileOutputStream(temp, true)) {
            output.write(data.getBytes());
        }
        return temp;
    }

}
