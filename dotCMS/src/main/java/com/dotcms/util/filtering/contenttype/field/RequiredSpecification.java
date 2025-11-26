package com.dotcms.util.filtering.contenttype.field;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.util.filtering.Specification;

/**
 * This Specification is responsible for verifying if a Field is flagged as {@code Required}.
 *
 * @author Jose Castro
 * @since Nov 29th, 2024
 */
public class RequiredSpecification implements Specification<Field> {

    @Override
    public boolean isSatisfiedBy(final Field field) {
        return field.required();
    }

}
