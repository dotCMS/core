package com.dotcms.saml.service;

/**
 * Just a class that needs to be aware of any system invalidation to do his part.
 */
public interface InvalidateAware {

    void invalidate();
}
