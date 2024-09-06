package com.dotcms.datagen;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.ARCHIVED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.EDITING;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.LISTING;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.LOCKED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.NEW;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.PUBLISHED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.UNLOCKED;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.UNPUBLISHED;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class TestWorkflowUtils {

    public static WorkflowScheme getDocumentWorkflow() throws DotDataException{
        return getDocumentWorkflow(null);
    }

    public static WorkflowScheme getDocumentWorkflow(String workFlowName) throws DotDataException {

        if(null == workFlowName){
            workFlowName = DM_WORKFLOW;
        }

        WorkflowScheme documentWorkflow = APILocator.getWorkflowAPI().findSchemeByName(workFlowName);
        if (null == documentWorkflow) {

            final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions = Arrays
                    .asList(
                            Tuple.of("Editing",
                                Arrays.asList(
                                // First component of the Tuple is the desired Action-Name.
                                // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                // Third is the show-When definition.
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Send for Review", "Review",  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED)),
                                        Tuple.of("Send to Legal", "Legal Approval", EnumSet.of(EDITING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Publish", "Published", EnumSet.of(EDITING, LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                            ),
                            Tuple.of("Review",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Return for Edits", "Editing", EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Send to Legal", "Legal Approval", EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Publish", "Published", EnumSet.of(EDITING, LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                            ),
                            Tuple.of("Legal Approval",
                                Arrays.asList(
                                        Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Return for Edits", "Editing", EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of("Publish", "Published", EnumSet.of(EDITING, LISTING, UNLOCKED, NEW, PUBLISHED))
                                )
                            ),
                            Tuple.of("Published",
                                Arrays.asList(
                                        Tuple.of("Republish", "Published", EnumSet.of(EDITING, LOCKED, UNLOCKED, PUBLISHED, ARCHIVED)),
                                        Tuple.of("Unpublish", "Review", EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED)),
                                        Tuple.of("Archive", "Archived", EnumSet.of(EDITING, LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                ))
                            ,
                            Tuple.of("Archived",
                                Arrays.asList(
                                        Tuple.of("Full Delete", "Archived", EnumSet.of(EDITING, LISTING, LOCKED, UNLOCKED, ARCHIVED)),
                                        Tuple.of("Restore", "Review", EnumSet.of(EDITING, LISTING, LOCKED, UNLOCKED, ARCHIVED)),
                                        Tuple.of("Reset Workflow", "Editing", EnumSet.of(LISTING, LOCKED, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED, ARCHIVED))
                                )

                            )
                     );

            documentWorkflow = new WorkflowDataGen().name(DM_WORKFLOW)
                    .stepAndAction(workflowStepsAndActions).nextPersistedWithStepsAndActions();
        }
        return documentWorkflow;
    }

    public static WorkflowScheme getSystemWorkflow() throws DotDataException {
        return APILocator.getWorkflowAPI().findSystemWorkflowScheme();
    }

}
