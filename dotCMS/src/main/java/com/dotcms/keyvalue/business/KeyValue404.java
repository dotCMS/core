package com.dotcms.keyvalue.business;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.Serializable;

public final class KeyValue404 extends Contentlet implements KeyValue, Serializable {

    public static final String KEY_VALUE_404 = "KEY_VALUE__404";
    @Override
    public String getKey() {
        return KEY_VALUE_404;
    }

    @Override
    public String getInode() {
        return KEY_VALUE_404;
    }

    @Override
    public String getValue() {
        return KEY_VALUE_404;
    }

}
