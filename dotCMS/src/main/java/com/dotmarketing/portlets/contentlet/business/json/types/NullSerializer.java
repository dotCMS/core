package com.dotmarketing.portlets.contentlet.business.json.types;

import java.io.Serializable;

public class NullSerializer implements DataTypeSerializer<Serializable, Serializable>{

    @Override
    public Serializable write(Serializable in) {
        return in;
    }

    @Override
    public Serializable read(Serializable in) {
        return in;
    }
}
