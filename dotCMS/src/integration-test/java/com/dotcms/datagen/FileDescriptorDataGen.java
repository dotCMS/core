package com.dotcms.datagen;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publishing.FilterDescriptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class FileDescriptorDataGen extends AbstractDataGen<FilterDescriptor> {

    private boolean dependencies = true;
    private boolean relationships = true;
    private boolean forcePush = false;
    private boolean defaultFilter = true;
    private String title;
    private String key;

    public FileDescriptorDataGen dependencies(boolean dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public FileDescriptorDataGen relationships(boolean relationships) {
        this.relationships = relationships;
        return this;
    }

    public FileDescriptorDataGen forcePush(boolean forcePush) {
        this.forcePush = forcePush;
        return this;
    }

    public FileDescriptorDataGen title(String title) {
        this.title = title;
        return this;
    }

    public FileDescriptorDataGen key(String key) {
        this.key = key;
        return this;
    }

    @Override
    public FilterDescriptor next() {

        final Map<String,Object> filtersMap = ImmutableMap.of(
                "dependencies",dependencies,
                "relationships",relationships,
                "forcePush",forcePush
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
        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
        return APILocator.getPublisherAPI().getFilterDescriptorByKey(filterDescriptor.getKey());
    }
}
