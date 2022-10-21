package com.dotcms.datagen;

import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;


public class VariantDataGen extends AbstractDataGen<Variant> {

    private String name;
    private String description;
    private boolean archived = false;

    public VariantDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public VariantDataGen description(final String description) {
        this.description = description;
        return this;
    }

    public VariantDataGen archived(final boolean deleted) {
        this.archived = deleted;
        return this;
    }

    @Override
    public Variant next() {

        final String innerName = name == null ? "VariantTest_" + RandomStringUtils.randomAlphanumeric(10) : name;
        final String innerDescription = description
                == null ? "Description for: " + innerName : description;
        return Variant.builder()
                .description(Optional.of(innerDescription))
                .name(innerName)
                .archived(archived)
                .build();
    }

    @Override
    public Variant persist(final Variant variant) {
        try {
            return  FactoryLocator.getVariantFactory().save(variant);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }
}
