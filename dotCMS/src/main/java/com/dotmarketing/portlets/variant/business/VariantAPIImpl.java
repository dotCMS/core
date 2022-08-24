package com.dotmarketing.portlets.variant.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import java.util.Optional;

/**
 * API of {@link Variant}
 */
public class VariantAPIImpl implements VariantAPI {
    private VariantFactory variantFactory;

    public VariantAPIImpl(){
        variantFactory = FactoryLocator.getVariantFactory();
    }

    /**
     * Implementation for {@link VariantAPI#save(Variant)}
     * @param variant
     *
     * @return
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public Variant save(final Variant variant) throws DotDataException {

        Preconditions.checkNotNull(variant.getName(), "Variant name should not be null");

        if (variant.isArchived()) {
            throw new IllegalArgumentException("Variant can not be created as archive");
        }

        return variantFactory.save(variant);
    }

    /**
     * Implementation for {@link VariantAPI#update(Variant)}
     * @param variant
     *
     * @throws DotDataException
     */
    @Override
    public void update(final Variant variant) throws DotDataException {
        Preconditions.checkNotNull(variant.getName(), "Variant name should not be null");
        Preconditions.checkNotNull(variant.getIdentifier(), "Variant ID should not be null");

        variantFactory.update(variant);
    }

    /**
     * Implementation for {@link VariantAPI#delete(String)}
     * @param id Variant's id to be deleted
     */
    @Override
    public void delete(String id) throws DotDataException {
        variantFactory.delete(id);
    }

    /**
     * Implementation for {@link VariantAPI#archive(String)}
     * @param id Variant's id to be archive
     */
    @Override
    public void archive(final String id) throws DotDataException {
        final Variant variant = get(id)
                .orElseThrow(() -> new DoesNotExistException("The Variant does not exists"));

        final Variant variantArchived = new Variant(variant.getIdentifier(), variant.getName(), true);

        update(variantArchived);
    }

    /**
     * Implementation for {@link VariantAPI#get(String)}
     *
     * @param identifier {@link Variant}'s identifier
     * @return
     */
    @Override
    public Optional<Variant> get(final String identifier) throws DotDataException {
        Preconditions.checkNotNull(identifier, "Variant ID should not be null");
        return variantFactory.get(identifier);
    }
}
