package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolbox.privateInternalProperties;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

/**
 * Entry point class for the Strategies hierarchy
 * @param <T>
 */
public abstract class AbstractTransformStrategy<T extends Contentlet> {

    protected final TransformToolbox toolBox;

    AbstractTransformStrategy(final TransformToolbox toolBox) {
        this.toolBox = toolBox;
    }

    /**
     * Any descendant that has a particular way to retrieve it's own concrete type should override this
     * @param contentlet
     * @return
     */
     T fromContentlet(Contentlet contentlet){
         return (T)contentlet;
     }

    abstract Map<String, Object> transform(T contentlet,
            Map<String, Object> map,
            Set<TransformOptions> options,
            User user) throws DotDataException, DotSecurityException;

    public void apply(final T contentlet, final Map<String, Object> map,
            final Set<TransformOptions> includeOptions, final User user) {
        final T subtype = fromContentlet(contentlet);
        try {
             transform(subtype, map, includeOptions, user);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(AbstractTransformStrategy.class, String.format(
               "Error applying transformation to contentlet with id `%s` ",
               subtype.getIdentifier()), e
            );
        }
    }

    public void cleanup(final Map<String, Object> map) {
        map.keySet().removeAll(privateInternalProperties);
    }

}
