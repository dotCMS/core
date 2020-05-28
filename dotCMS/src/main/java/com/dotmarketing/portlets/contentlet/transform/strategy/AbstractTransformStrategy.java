package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.util.Map;
import java.util.Set;

public abstract class AbstractTransformStrategy<T extends Contentlet> {

    protected final TransformToolbox toolBox;

    AbstractTransformStrategy(final TransformToolbox toolBox) {
        this.toolBox = toolBox;
    }

    abstract T fromContentlet(Contentlet contentlet);

    abstract Map<String, Object> transform(T contentlet,
            Map<String, Object> map,
            Set<TransformOptions> options) throws DotDataException, DotSecurityException;

    public void apply(final T contentlet, final Map<String, Object> map, final Set<TransformOptions> includeOptions) {
        final T subtype = fromContentlet(contentlet);
        try {
             transform(subtype, map, includeOptions);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(AbstractTransformStrategy.class, String.format(
               "Error applying transformation to contentlet with id `%s` ",
               subtype.getIdentifier()), e
            );
        }
    }

}
