package com.dotcms.rest.annotation;

import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Encapsulates a decorator for a header annotation
 * @author jsanca
 */
public interface HeaderDecorator extends Serializable {

    /**
     * Based on the annotation meta info, decorates the headers
     * @param annotation {@link Annotation}
     * @param headers {@link MultivaluedMap}
     */
    void decorate (final Annotation annotation, final MultivaluedMap<String, Object> headers);

} // E:O:F:HeaderDecorator.
