package com.dotcms.util.filtering.contenttype.field;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.util.filtering.Specification;

/**
 * This simple Specification serves as a base/placeholder for developers to combine multiple
 * {@link Specification} objects. This is very useful when the given list of Specifications is
 * dynamic.
 *
 * @author Jose Castro
 * @since Nov 29th, 2024
 */
public class CombinedSpecification implements Specification<Field> {

    @Override
    public boolean isSatisfiedBy(final Field field) {
        // This base Specification returns 'true' by default
        return true;
    }

}
