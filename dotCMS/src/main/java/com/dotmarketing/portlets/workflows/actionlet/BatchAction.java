package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * This interface establishes the contact between the WorkflowAPI and any WorkFlowActionlet willing to add
 * a In-Batch type of behavior to its own implementation.
 * @param <T>
 */
public interface BatchAction <T> {


    /**
     * Once the actions have been added into the shared context. This will get them fired-up.
     * @param user
     * @param actionsContext
     * @param actionClass
     * @param params
     * @throws WorkflowActionFailureException
     */
    void executeBatchAction(final User user,
            final ConcurrentMap<String, Object> actionsContext,
            final WorkflowActionClass actionClass,
            final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException;


    /**
     * Before the Action can be executed it must be prepared adding the action to a shared context.
     * Thi is sample default implementation however depending on your needs and the data structure of choice it will require adjustments.
     * @param processor
     * @param actionClass
     * @param params
     */
    default void preBatchAction(final WorkflowProcessor processor,
            final WorkflowActionClass actionClass,
            final Map<String, WorkflowActionClassParameter> params) {
        final Contentlet contentlet = processor.getContentlet();
        final String inode = contentlet.getInode();
        final String identifier = contentlet.getIdentifier();

        final String actionletInstanceId = actionClass.getId();
        final Map<String, Object> values = ImmutableMap
                .of("identifier", identifier, "inode", inode);
        final ConcurrentMap <String,Object> context = processor.getActionsContext();

        List.class.cast(context.computeIfAbsent(actionletInstanceId, s -> new ArrayList<>())).add(values);

    }

    /**
     * This method must instruct the Batch Action how to extract the object out of the shared context.
     * Again, default implemention that might require adjustments.
     * @param context
     * @param actionClass
     * @return
     */
    default List <T> getObjectsForBatch(final ConcurrentMap<String, Object> context, final WorkflowActionClass actionClass){
        final String actionletInstanceId = actionClass.getId();
        final Object object = context.get(actionletInstanceId);
        if(null == object) {
            return Collections.emptyList();
        }
        return (List<T>) context.get(actionletInstanceId);
    }

}
