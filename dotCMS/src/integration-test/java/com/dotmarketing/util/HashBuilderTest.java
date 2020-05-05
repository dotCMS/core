package com.dotmarketing.util;

import com.google.common.hash.Hashing;
import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

public class HashBuilderTest {

    @Test
    public void test_buildBytes () throws Exception {

        final String text = "this is a test";
        final HashBuilder hashBuilder = Encryptor.Hashing.sha256();
        hashBuilder.append(text.getBytes(StandardCharsets.UTF_8));
        final byte [] bytes = hashBuilder.buildBytes();
        System.out.println(new String(bytes));
        System.out.println(toString(bytes));
        System.out.println(new String(Base64.getEncoder().encode(bytes)));

        final HashBuilder hashBuilder2 = Encryptor.Hashing.sha256();
        hashBuilder2.append(text.getBytes(StandardCharsets.UTF_8));
        System.out.println(hashBuilder2.buildHexa());

        final String sha256hex1 = Hashing.sha256().newHasher().putString(text, StandardCharsets.UTF_8).hash().toString();
        System.out.println(sha256hex1);
        String sha256hex2 = Hashing.sha256().hashString(text, StandardCharsets.UTF_8)
                .toString();

        System.out.println(sha256hex2); // 2e99758548972a8e8822ad47fa1017ff72f06f3ff6a016851f45c398732bc50c
    }

    public final String toString(final byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    static int[] buffers = {1024, 1024 * 2, 1024 * 4, 1024 * 8, 1024 * 16, 1024 * 32, 1024 * 64};

    @Test
    public void test_buildBytes2 () throws Exception {

    File inputFile = new File("/Users/jsanca/Downloads/[JAVA][Beginning Java 8 Games Development].pdf");

        for (int buff : buffers) {
            long time = System.currentTimeMillis();
            System.out.println("buffer  :" + buff);
            System.out.println("sha     :" + sha256(inputFile, buff));
            System.out.println("took    :" + (System.currentTimeMillis() - time) + "ms");
            System.out.println("");
        }


    }


    private static String sha256(File inputFile, int buff) throws Exception {

        byte[] buffer = new byte[buff];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream bis = new BufferedInputStream(Files.newInputStream(inputFile.toPath()))) {


            while ((count = bis.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
        }
        byte[] hash = digest.digest();

        return getHexaString(hash);



    }



    private static String getHexaString(byte[] data) {
        String result = new BigInteger(1, data).toString(16);
        return result;
    }

}
