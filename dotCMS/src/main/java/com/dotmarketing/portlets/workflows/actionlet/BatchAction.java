package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public interface BatchAction <T> {

    void executeBatchAction(final User user,
            final ConcurrentMap<String, Object> batchActionContext,
            final WorkflowActionClass actionClass,
            final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException;


    default void preBatchAction(final WorkflowProcessor processor,
            final WorkflowActionClass actionClass,
            final Map<String, WorkflowActionClassParameter> params) {
        final Contentlet contentlet = processor.getContentlet();
        final String inode = contentlet.getInode();
        final String identifier = contentlet.getIdentifier();

        final String actionletInstanceId = actionClass.getId();
        final Map<String, Object> values = ImmutableMap
                .of("identifier", identifier, "inode", inode);
        final ConcurrentMap <String,Object> context = processor.getBatchActionContext();

        List.class.cast(context.computeIfAbsent(actionletInstanceId, s -> new ArrayList<>())).add(values);

    }

    List <T> getObjectsForBatch(final ConcurrentMap<String, Object> context, final WorkflowActionClass actionClass);

}
