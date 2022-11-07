package com.dotcms.datagen;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data Generator for Push Publishing Filters.
 *
 * @author Freddy Rodriguez
 * @since Feb 9th, 2021
 */
public class FilterDescriptorDataGen extends AbstractDataGen<FilterDescriptor> {

    private boolean dependencies = true;
    private boolean relationships = true;
    private boolean forcePush = false;
    private boolean defaultFilter = true;
    private boolean clearFilterList = Boolean.TRUE;
    private String title;
    private String key;
    private String sort;
    private List<String> excludeDependencyClasses =  Collections.emptyList();

    public FilterDescriptorDataGen dependencies(boolean dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public FilterDescriptorDataGen relationships(boolean relationships) {
        this.relationships = relationships;
        return this;
    }

    public FilterDescriptorDataGen forcePush(boolean forcePush) {
        this.forcePush = forcePush;
        return this;
    }

    public FilterDescriptorDataGen title(String title) {
        this.title = title;
        return this;
    }

    public FilterDescriptorDataGen key(String key) {
        this.key = key;
        return this;
    }

    public FilterDescriptorDataGen sort(final String sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Determines if the existing Filter Descriptor list must be cleared every time a new Filter Descriptor is added.
     * Clearing the list is the default behavior.
     *
     * @param clearFilterList If the list must NOT be cleared when adding this Filter, set to {@code false}. This is
     *                        important when creating several test Filter Descriptors.
     *
     * @return The {@link FilterDescriptorDataGen} object.
     */
    public FilterDescriptorDataGen clearFilterList(final boolean clearFilterList) {
        this.clearFilterList = clearFilterList;
        return this;
    }

    public FilterDescriptorDataGen defaultFilter(final boolean defaultFilter) {
        this.defaultFilter = defaultFilter;
        return this;
    }

    @Override
    public FilterDescriptor next() {

        final Map<String,Object> filtersMap = ImmutableMap.of(
                FilterDescriptor.DEPENDENCIES_KEY,dependencies,
                FilterDescriptor.RELATIONSHIPS_KEY,relationships,
                FilterDescriptor.FORCE_PUSH_KEY,forcePush,
                FilterDescriptor.EXCLUDE_DEPENDENCY_CLASSES_KEY, excludeDependencyClasses
        );

        try {
            return new FilterDescriptor(
                    key != null ? key : String.format("filterKey_%s.yml", System.currentTimeMillis()),
                    title != null ? title : "FileDescriptor " + System.currentTimeMillis(),
                    UtilMethods.isSet(sort) ? sort : FilterDescriptor.DEFAULT_SORT_VALUE,
                    filtersMap,
                    defaultFilter,
                    APILocator.getRoleAPI().loadCMSAdminRole().getName()
            );
        } catch (final DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FilterDescriptor persist(FilterDescriptor filterDescriptor) {
        if (this.clearFilterList) {
            PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        }
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
        return APILocator.getPublisherAPI().getFilterDescriptorByKey(filterDescriptor.getKey());
    }

    public FilterDescriptorDataGen excludeDependencyClasses(final List<String> excludeDependencyClasses) {
        this.excludeDependencyClasses = excludeDependencyClasses;
        return this;
    }

}
