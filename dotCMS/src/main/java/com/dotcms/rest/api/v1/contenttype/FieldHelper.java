package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.WebResource;

import java.io.Serializable;

/**
 * Content-type Field helper.
 */
public class FieldHelper implements Serializable {

    private static class SingletonHolder {
        private static final FieldHelper INSTANCE = new FieldHelper();
    }

    public static FieldHelper getInstance() {

        return FieldHelper.SingletonHolder.INSTANCE;
    }

    private final WebResource webResource;

    public FieldHelper() {
        this(new WebResource());
    }

    @VisibleForTesting
    protected FieldHelper(WebResource webResource) {
        this.webResource = webResource;
    }
} // E:O:F:FieldHelper.
