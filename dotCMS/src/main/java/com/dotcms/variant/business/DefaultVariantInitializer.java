package com.dotcms.variant.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import io.vavr.control.Try;
import java.util.Optional;

/**
 * Creates a DEFAULT variant if it doesn't exist
 */
public class DefaultVariantInitializer implements DotInitializer  {

    final VariantAPI variantAPI = APILocator.getVariantAPI();

    @Override
    public void init() {
        Optional<Variant> defaultVariant = Try.of(()->variantAPI
                .get(VariantAPI.DEFAULT_VARIANT.name())).getOrElse(Optional.empty());

        if(defaultVariant.isEmpty()) {
            Try.of(()->variantAPI.save(VariantAPI.DEFAULT_VARIANT))
                    .getOrElseThrow(()->new DotStateException("Unable to create DEFAULT Variant"));
        }

    }
}
