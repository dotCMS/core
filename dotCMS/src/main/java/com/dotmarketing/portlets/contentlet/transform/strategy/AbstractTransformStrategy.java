package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLED_WYSIWYG_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLE_WORKFLOW;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DONT_VALIDATE_ME;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DOT_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IS_TEST_MODE;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.NULL_PROPERTIES;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ACTION_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ASSIGN_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_COMMENTS_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_IN_PROGRESS;

import com.dotcms.api.APIProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

/**
 * Entry point class for the Strategies hierarchy
 * Basically any new behavior desired to be applied to mutate the resulting map should be plugged through one of these.
 * @param <T>
 */
public abstract class AbstractTransformStrategy<T extends Contentlet> {

    //This set contains all the properties that we want to prevent from making it into the final contentlet or transformed map.
    public static final Set<String> privateInternalProperties = ImmutableSet
            .of(NULL_PROPERTIES,
                DISABLE_WORKFLOW,
                DONT_VALIDATE_ME,
                DOT_NAME_KEY,
                WORKFLOW_IN_PROGRESS,
                WORKFLOW_ASSIGN_KEY,
                WORKFLOW_ACTION_KEY,
                WORKFLOW_COMMENTS_KEY,
                DONT_VALIDATE_ME,
                IS_TEST_MODE,
                "__NAME__"
            );

    static final String NOT_APPLICABLE = "N/A";

    protected final APIProvider toolBox;

    AbstractTransformStrategy(final APIProvider toolBox) {
        this.toolBox = toolBox;
    }

    /**
     * Any descendant that has a particular way to retrieve it's own concrete type should override this.
     * e.g. For File assets it would be fileAssetAPI.fromContentlet(..)
     * @param contentlet
     * @return
     */
     T fromContentlet(final Contentlet contentlet){
         return (T)contentlet;
     }

    /**
     * transform method entry point
     * @param source
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    protected abstract Map<String, Object> transform(T source,
            Map<String, Object> map,
            Set<TransformOptions> options,
            User user) throws DotDataException, DotSecurityException;

    /**
     * This serves as the entry point to apply the transformation
     * @param source is the original contentlet
     * @param targetMap is the map grabbed from the copy-contentlet
     * @param includeOptions
     * @param user
     */
    public void apply(final Contentlet source, final Map<String, Object> targetMap,
        final Set<TransformOptions> includeOptions, final User user) {
        final T subtype = fromContentlet(source);
        try {
             transform(subtype, targetMap, includeOptions, user);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(AbstractTransformStrategy.class, String.format(
               "Error applying transformation to contentlet with id `%s` ",
               subtype.getIdentifier()), e
            );
        }
    }

    /**
     * This removes any private property left overs.
     * This is visible to all descendants but it gets applied always at the end of the transformation chain
     * Override as required
     * @param map
     */
    public void cleanup(final Map<String, Object> map) {
        map.keySet().removeAll(privateInternalProperties);
    }

}
