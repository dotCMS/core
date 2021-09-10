package com.dotcms.cache.lettuce;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Test for {@link DotObjectCodec}
 * @author jsanca
 */
public class DotObjectCodecTest {

    /**
     * Method to test: {@link DotObjectCodec#encodeKey}
     * Given Scenario: send a null to encode
     * ExpectedResult: Expected returns a null
     *
     */
    @Test
    public void test_encode_null_key() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        Assert.assertNull(codec.encodeKey(null));
    }

    /**
     * Method to test: {@link DotObjectCodec#encodeKey} and {@link DotObjectCodec#decodeKey(ByteBuffer)}
     * Given Scenario: tries to encode and decode a key
     * ExpectedResult: the key encoded can be decoded
     *
     */
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

    /**
     * Method to test: {@link DotObjectCodec#encodeValue}
     * Given Scenario: send a null to encode
     * ExpectedResult: Expected returns a not null ByteBuffer
     *
     */
    @Test()
    public void test_encode_null_value() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        final ByteBuffer byteBuffer = codec.encodeValue(null);
        Assert.assertNotNull(byteBuffer);
    }

    /**
     * Method to test: {@link DotObjectCodec#decodeValue}
     * Given Scenario: send a null to decodeValue
     * ExpectedResult: Expected decode exception
     *
     */
    @Test(expected = DecodeException.class)
    public void test_decode_null_value() throws Exception {

        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        codec.decodeValue(null);
    }

    /**
     * Method to test: {@link DotObjectCodec#decodeValue}
     * Given Scenario: encode and decode a value
     * ExpectedResult: the encoded value will be decoded ok
     *
     */
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
