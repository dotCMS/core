package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLED_WYSIWYG_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLE_WORKFLOW;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DONT_VALIDATE_ME;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DOT_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IS_TEST_MODE;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LAST_REVIEW_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.NULL_PROPERTIES;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.REVIEW_INTERNAL_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ACTION_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ASSIGN_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_COMMENTS_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_IN_PROGRESS;

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
 * @param <T>
 */
public abstract class AbstractTransformStrategy<T extends Contentlet> {

    //This set contains all the properties that we want to prevent from making it into the final contentlet or transformed map.
    public static final Set<String> privateInternalProperties = ImmutableSet
            .of(NULL_PROPERTIES,
                DISABLE_WORKFLOW,
                DONT_VALIDATE_ME,
                LAST_REVIEW_KEY,
                REVIEW_INTERNAL_KEY,
                DISABLED_WYSIWYG_KEY,
                DOT_NAME_KEY,
                WORKFLOW_IN_PROGRESS,
                WORKFLOW_ASSIGN_KEY,
                WORKFLOW_ACTION_KEY,
                WORKFLOW_COMMENTS_KEY,
                DONT_VALIDATE_ME,
                IS_TEST_MODE
            );

    static final String NOT_APPLICABLE = "N/A";

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
