package com.dotmarketing.util;

import com.google.common.hash.Hashing;
import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HashBuilderTest {

    @Test
    public void test_buildBytes () throws Exception {

        final String text = "this is a test";
        final HashBuilder hashBuilder = Encryptor.Hashing.sha256();
        hashBuilder.append(text.getBytes(StandardCharsets.UTF_8));
        final byte [] bytes = hashBuilder.buildBytes();
        System.out.println(new String(bytes));
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
}
