package com.dotmarketing.util;

import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
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
}
