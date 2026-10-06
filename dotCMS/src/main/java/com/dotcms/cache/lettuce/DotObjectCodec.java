package com.dotcms.cache.lettuce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import io.lettuce.core.codec.RedisCodec;

/**
 * This method is used by Lettuce Redis Driver
 * to wrap and serialize objects being put to redis
 * @author will
 *
 */
public class DotObjectCodec<K,V> implements RedisCodec<K, V> {

    private final Charset charset = StandardCharsets.UTF_8;

    @Override
    public K decodeKey(final ByteBuffer bytes) {

        return (K)charset.decode(bytes).toString();
    }

    @Override
    public V decodeValue(final ByteBuffer bytes) {

        try {
            // copy exactly the readable region: bytes.array() ignores position/limit/arrayOffset and can
            // read stale bytes when the buffer is a slice. (null buffer -> NPE -> DecodeException, as before.)
            final byte[] data = new byte[bytes.remaining()];
            bytes.get(data);

            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {

                final Object o = in.readObject();
                return (V) o;
            }
        } catch (Exception e) {

            throw new DecodeException(e);
        }
    }

    @Override
    public ByteBuffer encodeKey(final K key) {

        return null != key? charset.encode(key.toString()): null;
    }

    @Override
    public ByteBuffer encodeValue(final V value) {

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final ObjectOutputStream output = new ObjectOutputStream(baos)) {

            output.writeObject(value);
            output.flush();
            return ByteBuffer.wrap(baos.toByteArray());
        } catch(Exception e) {

            throw new EncodeException(e);
        }
    }
}
