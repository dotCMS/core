package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.util.filtering.Specification;
import com.dotcms.util.filtering.contenttype.field.CombinedSpecification;
import com.dotcms.util.filtering.contenttype.field.RequiredSpecification;
import com.dotcms.util.filtering.contenttype.field.ShowInListSpecification;
import com.dotcms.util.filtering.contenttype.field.SystemIndexedSpecification;
import com.dotcms.util.filtering.contenttype.field.UniqueSpecification;
import com.dotcms.util.filtering.contenttype.field.UserSearchableSpecification;

import java.util.Set;

/**
 * Enum that represents the different criteria that can be used to filter fields in a Content Type.
 * This allows you to get a specific list of {@link Field}s from a Content Type that meet your
 * specified criteria via @{link Specification} classes. In here, you can define as many
 * Specifications as you need.
 * <p>The initial specifications represent the default attributes that you can enable for a given
 * field, such as:</p>
 * <ul>
 *     <li>Required.</li>
 *     <li>User Searchable.</li>
 *     <li>System Indexed.</li>
 *     <li>Show in List.</li>
 *     <li>Unique.</li>
 * </ul>
 *
 * @author Jose Castro
 * @since Nov 30th, 2024
 */
public enum FilteringCriteria {

    REQUIRED(new RequiredSpecification()),
    USER_SEARCHABLE(new UserSearchableSpecification()),
    SYSTEM_INDEXED(new SystemIndexedSpecification()),
    SHOW_IN_LIST(new ShowInListSpecification()),
    UNIQUE(new UniqueSpecification());

    final Specification<Field> specification;

    FilteringCriteria(final Specification<Field> specification) {
        this.specification = specification;
    }

    public Specification<Field> specification() {
        return this.specification;
    }

    /**
     * Takes the list of criteria that will be used to query for specific fields in a Content Type
     * and generates the appropriate Specification object to be used in the filtering.
     *
     * @param criteria A Set of FilteringCriteria objects that will be used to filter the fields.
     *
     * @return The {@link Specification} object that will be used to filter the fields.
     */
    public static Specification<Field> specificationsFrom(final Set<FilteringCriteria> criteria) {
        Specification<Field> mainSpec = new CombinedSpecification();
        for (final FilteringCriteria criterion : criteria) {
            mainSpec = mainSpec.and(criterion.specification());
        }
        return mainSpec;
    }

}
