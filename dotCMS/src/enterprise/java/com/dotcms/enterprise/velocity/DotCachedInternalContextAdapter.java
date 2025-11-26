/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.velocity;

import java.io.Serializable;
import java.util.Map;
import org.apache.velocity.context.ChainedInternalContextAdapter;
import org.apache.velocity.context.InternalContextAdapter;

public class DotCachedInternalContextAdapter extends ChainedInternalContextAdapter {

    private final Map<String, Serializable> cachedContext;


    public DotCachedInternalContextAdapter(InternalContextAdapter inner, Map<String, Serializable> cachedContext) {
        super(inner);
        this.cachedContext = cachedContext;
    }


    @Override
    public Object get(String key) {

        return cachedContext.containsKey(key) ? cachedContext.get(key) : super.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null || value == null) {
            return null;
        } else if (value instanceof Serializable) {
            cachedContext.put(key, (Serializable) value);
        } else {
            cachedContext.put(key, value.toString());
        }

        return super.put(key, value);
    }


    @Override
    public boolean containsKey(Object key) {
        return cachedContext.containsKey(key) || super.containsKey(key);
    }


    @Override
    public Object remove(Object key) {
        Object obj = cachedContext.remove(key);
        Object obj2 = super.remove(key);
        return obj == null ? obj2 : obj;
    }



}
