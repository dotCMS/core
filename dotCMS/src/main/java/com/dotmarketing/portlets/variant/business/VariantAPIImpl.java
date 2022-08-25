package com.dotmarketing.portlets.variant.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import com.dotmarketing.util.Logger;
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
    public Variant save(final Variant variant) {

        Preconditions.checkNotNull(variant.getName(), "Variant name should not be null");

        if (variant.isArchived()) {
            throw new IllegalArgumentException("Variant can not be created as archive");
        }

        Logger.debug(this, ()-> "Saving Variant: " + variant);

        try {
            return variantFactory.save(variant);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Implementation for {@link VariantAPI#update(Variant)}
     * @param variant
     *
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void update(final Variant variant) {
        Preconditions.checkNotNull(variant.getName(), "Variant name should not be null");
        Preconditions.checkNotNull(variant.getIdentifier(), "Variant ID should not be null");

        try {
            get(variant.getIdentifier())
                    .orElseThrow(() -> new DoesNotExistException("The variant does not exists"));

            Logger.debug(this, () -> "Updating Variant: " + variant);
            variantFactory.update(variant);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Implementation for {@link VariantAPI#delete(String)}
     * @param id Variant's id to be deleted
     */
    @Override
    @WrapInTransaction
    public void delete(String id) {
        final Variant variant = get(id).orElseThrow(() -> new DoesNotExistException("The variant must exists"));
        if (!variant.isArchived()) {
            throw new IllegalStateException("The Variant must be archived to be able to delete it");
        }

        Logger.debug(this, ()-> "Deleting Variant: " + variant);

        try {
            variantFactory.delete(id);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Implementation for {@link VariantAPI#archive(String)}
     * @param id Variant's id to be archive
     */
    @Override
    @WrapInTransaction
    public void archive(final String id) {
        final Variant variant = get(id)
                .orElseThrow(() -> new DoesNotExistException("The Variant does not exists"));

        final Variant variantArchived = new Variant(variant.getIdentifier(), variant.getName(),
                true);
        Logger.debug(this, () -> "Archiving Variant: " + variant);
        update(variantArchived);
    }

    /**
     * Implementation for {@link VariantAPI#get(String)}
     *
     * @param identifier {@link Variant}'s identifier
     * @return
     */
    @Override
    public Optional<Variant> get(final String identifier)  {
        Preconditions.checkNotNull(identifier, "Variant ID should not be null");
        Logger.debug(this, ()-> "Getting Variant: " + identifier);

        try {
            return variantFactory.get(identifier);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }
}
