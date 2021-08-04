package com.dotcms.cache.lettuce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import com.dotmarketing.exception.DotRuntimeException;
import io.lettuce.core.codec.RedisCodec;

/**
 * This method is used by Lettuce Redis Driver
 * to wrap and serialize objects being put to redis
 * @author will
 *
 */
public class DotObjectCodec<K,V> implements RedisCodec<K, V> {

    private final Charset charset = Charset.forName("UTF-8");

    @Override
    public K decodeKey(final ByteBuffer bytes) {

        return (K)charset.decode(bytes).toString();
    }

    @Override
    public V decodeValue(final ByteBuffer bytes) {

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.array()))){
        
            final Object o = in.readObject();
            return  (V)o;
        } catch(Exception e) {

            throw new DotRuntimeException(e);
        }
    }

    @Override
    public ByteBuffer encodeKey(final K key) {

        return charset.encode(key.toString());
    }

    @Override
    public ByteBuffer encodeValue(final V value) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(baos)) {

            output.writeObject(value);
            output.flush();
            return ByteBuffer.wrap(baos.toByteArray());
        } catch(Exception e) {

            throw new DotRuntimeException(e);
        }
    }
}
