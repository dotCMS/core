package com.dotcms.datagen;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import com.dotmarketing.util.UUIDGenerator;

public class VariantDataGen extends AbstractDataGen<Variant> {

    private String id;
    private String name;
    private boolean archived = false;

    public VariantDataGen id(final String id) {
        this.id = id;
        return this;
    }

    public VariantDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public VariantDataGen archived(final boolean deleted) {
        this.archived = deleted;
        return this;
    }

    @Override
    public Variant next() {

        final String innerId = id == null ? UUIDGenerator.generateUuid() : id;
        final String innerName = name == null ? "Variant_" + System.currentTimeMillis() : name;
        return new Variant(innerId, innerName, archived);
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
