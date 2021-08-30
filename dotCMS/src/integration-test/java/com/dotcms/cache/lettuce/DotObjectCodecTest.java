package com.dotcms.cache.lettuce;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class DotObjectCodecTest {

    @Test
    public void test_encode_null_key() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        Assert.assertNull(codec.encodeKey(null));
    }

    @Test
    public void test_encode_decode_key() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        final String key = "key1";
        final ByteBuffer byteBuffer =  codec.encodeKey(key);
        Assert.assertNotNull(byteBuffer);
        final String recoveryKey = codec.decodeKey(byteBuffer);
        Assert.assertNotNull(recoveryKey);
        Assert.assertEquals(key, recoveryKey);
    }

    @Test()
    public void test_encode_null_value() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        final ByteBuffer byteBuffer = codec.encodeValue(null);
        Assert.assertNotNull(byteBuffer);
    }

    @Test(expected = DecodeException.class)
    public void test_decode_null_value() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        codec.decodeValue(null);
    }

    @Test
    public void test_encode_decode_value() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        final String value = "value1";
        final ByteBuffer byteBuffer =  codec.encodeValue(value);
        Assert.assertNotNull(byteBuffer);
        final String recoveryValue = (String) codec.decodeValue(byteBuffer);
        Assert.assertNotNull(recoveryValue);
        Assert.assertEquals(value, recoveryValue);
    }
}
