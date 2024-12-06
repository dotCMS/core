package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * This class represents a Response View for objects of type {@link Field}. It's very useful for
 * returning a list of fields, or a filtered sub-set if necessary.
 *
 * @author Jose Castro
 * @since Nov 29th, 2024
 */
public class FieldResponseView extends ResponseEntityView<List<Field>> {

    public FieldResponseView(final List<Field> entity) {
        super(entity);
    }

}
