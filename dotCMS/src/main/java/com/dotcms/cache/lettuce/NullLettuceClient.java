package com.dotcms.cache.lettuce;

public class NullLettuceClient implements LettuceClient {

    @Override
    public LettuceConnectionWrapper get() {
        return new LettuceConnectionWrapper(null);
    }

}
