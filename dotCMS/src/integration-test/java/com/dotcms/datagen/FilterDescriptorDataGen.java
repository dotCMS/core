package com.dotcms.datagen;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FilterDescriptorDataGen extends AbstractDataGen<FilterDescriptor> {

    private boolean dependencies = true;
    private boolean relationships = true;
    private boolean forcePush = false;
    private boolean defaultFilter = true;
    private String title;
    private String key;
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

    @Override
    public FilterDescriptor next() {

        final Map<String,Object> filtersMap = ImmutableMap.of(
                "dependencies",dependencies,
                "relationships",relationships,
                "forcePush",forcePush,
                "excludeDependencyClasses", excludeDependencyClasses
        );

        try {
            return new FilterDescriptor(
                    key != null ? key : String.format("filterKey_%s.yml", System.currentTimeMillis()),
                    title != null ? title : "FileDescriptor " + System.currentTimeMillis(),
                    filtersMap,
                    defaultFilter,
                    APILocator.getRoleAPI().loadCMSAdminRole().getName()
            );
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FilterDescriptor persist(FilterDescriptor filterDescriptor) {
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
        return APILocator.getPublisherAPI().getFilterDescriptorByKey(filterDescriptor.getKey());
    }

    public FilterDescriptorDataGen excludeDependencyClasses(final List<String> excludeDependencyClasses) {
        this.excludeDependencyClasses = excludeDependencyClasses;
        return this;
    }
}
