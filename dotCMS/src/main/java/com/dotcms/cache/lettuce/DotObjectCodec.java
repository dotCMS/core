package com.dotmarketing.business.cache.provider.lettuce;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.nustaq.serialization.FSTConfiguration;
import io.lettuce.core.codec.RedisCodec;

public class DotObjectCodec implements RedisCodec<String, Object> {
    private Charset charset = Charset.forName("UTF-8");
    final static FSTConfiguration fstConf = FSTConfiguration.createDefaultConfiguration();

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return charset.decode(bytes).toString();
    }

    @Override
    public Object decodeValue(ByteBuffer bytes) {
        return fstConf.asObject(bytes.array());
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return charset.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(Object value) {

        return ByteBuffer.wrap(fstConf.asByteArray(value));

    }
}
