package com.dotcms.content.elasticsearch.business.field;


import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;

/**
 * Strategy to handle the field value
 * @author jsanca
 */
@FunctionalInterface
public interface FieldHandlerStrategy {

    /**
     * Apply the strategy on the contentlet, field and value
     * @param contentlet Contentlet
     * @param field Field
     * @param value Object
     * @throws DotContentletStateException
     */
    void apply(Contentlet contentlet, Field field, Object value) throws DotContentletStateException;
}
