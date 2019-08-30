package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Jonathan Gamba 2019-04-05
 */
public class WorkflowDataGen extends AbstractDataGen<WorkflowScheme> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "testName" + currentTime;
    private String description = "testDescription" + currentTime;
    private Boolean archived = Boolean.FALSE;
    private Boolean defaultScheme = Boolean.FALSE;
    private Map<String, List<Tuple3<String, String, Set<WorkflowState>>>> stepsAndActions = new LinkedHashMap<>();

    @SuppressWarnings("unused")
    public WorkflowDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowDataGen description(final String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowDataGen archived(final Boolean archived) {
        this.archived = archived;
        return this;
    }

    @SuppressWarnings("unused")
    public WorkflowDataGen defaultScheme(final Boolean defaultScheme) {
        this.defaultScheme = defaultScheme;
        return this;
    }

    /**
     *
     * @param workflowStepName
     * @param workflowActions
     * @return
     */
    public WorkflowDataGen stepAndAction(final String workflowStepName,
            final List<Tuple3<String, String, Set<WorkflowState>>> workflowActions) {
        stepsAndActions.put(workflowStepName, workflowActions);
        return this;
    }

    /**
     * feed the builder with a the workflow step and actions definition.
     * @param workflowStepsAndActions Takes a List conformed by a Tuple of 2 the first value on the tuple is the Step name.
     * The second component of the tuple is a list that basically represents the actions definition for each step.
     *.asList(
     *  Tuple.of("Editing",  <-- Workflow step
     *    Arrays.asList(  <-- Step Definitions
     *      Tuple.of("Save as Draft", "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)), <-- Step Name, Next Step Name, And a Set of the showOnStates
     *      Tuple.of("Send for Review", "Review",  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED)),
     *      Tuple.of("Send to Legal", "Legal Approval", EnumSet.of(EDITING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED)),
     *      Tuple.of("Publish", "Published", EnumSet.of(EDITING, LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
     *     )
     *   )
     * )
     *
     * @return
     */
    @WrapInTransaction
    public WorkflowDataGen stepAndAction(
            final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions) {
        for (Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>> workflowStepAndActions : workflowStepsAndActions) {
            final String workflowStepName = workflowStepAndActions._1();
            final List<Tuple3<String, String, Set<WorkflowState>>> actionsAndNextStep = workflowStepAndActions._2();
            stepAndAction(workflowStepName, actionsAndNextStep);
        }
        return this;
    }


    @Override
    public WorkflowScheme next() {
        final WorkflowScheme workflowScheme = new WorkflowScheme();
        workflowScheme.setName(name);
        workflowScheme.setArchived(archived);
        workflowScheme.setDescription(description);
        workflowScheme.setModDate(new Date());
        workflowScheme.setCreationDate(new Date());
        workflowScheme.setDefaultScheme(defaultScheme);
        return workflowScheme;
    }

    @WrapInTransaction
    @Override
    public WorkflowScheme persist(final WorkflowScheme workflowScheme) {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        try {
            workflowAPI.saveScheme(workflowScheme, APILocator.systemUser());
            return workflowScheme;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist WorkflowScheme.", e);
        }
    }

    /**
     * Creates a new {@link WorkflowScheme} instance and persists it in DB
     *
     * @return A new WorkflowScheme instance persisted in DB
     */
    @Override
    public WorkflowScheme nextPersisted() {
        return persist(next());
    }

    public static void remove(final WorkflowScheme workflowScheme) {
        remove(workflowScheme, true);
    }

    @WrapInTransaction
    public static void remove(final WorkflowScheme workflowScheme, final Boolean failSilently) {
        try {
            APILocator.getWorkflowAPI().deleteScheme(workflowScheme, APILocator.systemUser());
        } catch (Exception e) {
            if (failSilently) {
                Logger.error(ContentTypeDataGen.class, "Unable to delete WorkflowScheme.", e);
            } else {
                throw new RuntimeException("Unable to delete WorkflowScheme.", e);
            }
        }
    }

    /**
     * Creates a new {@link WorkflowScheme} instance and persists it in DB
     *
     * @return A new WorkflowScheme instance persisted in DB
     */
    @WrapInTransaction
    public WorkflowScheme nextPersistedWithDefaultStepsAndActions() {

        //First we need to create the workflow scheme
        final WorkflowScheme workflowScheme = persist(next());

        //Now the steps
        final WorkflowStep step1 = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();
        final WorkflowStep step2 = new WorkflowStepDataGen(workflowScheme.getId()).nextPersisted();

        //Now associate actions to the steps
        final WorkflowAction action1 = new WorkflowActionDataGen(workflowScheme.getId(),
                step1.getId())
                .nextStep(step2.getId())
                .nextPersisted();
        final WorkflowAction action2 = new WorkflowActionDataGen(workflowScheme.getId(),
                step2.getId()).nextPersisted();

        //Now associate action classes to the actions
        new WorkflowActionClassDataGen(action1.getId()).nextPersisted();
        new WorkflowActionClassDataGen(action2.getId()).nextPersisted();

        return workflowScheme;
    }

    @WrapInTransaction
    public WorkflowScheme nextPersistedWithStepsAndActions() {

        //First we need to create the workflow scheme
        final WorkflowScheme workflowScheme = persist(next());
        final List<WorkflowStep> persistedStepsList = new ArrayList<>();
        final Map<String, WorkflowStep> persistedStepsMap = new LinkedHashMap<>();
        final Map<String, WorkflowAction> persistedActionsMap = new HashMap<>();
        int ordinal = 0;
        for (final Entry<String, List<Tuple3<String, String, Set<WorkflowState>>>> entry : stepsAndActions.entrySet()) {
            final String stepName = entry.getKey();
            final WorkflowStep workflowStep = new WorkflowStepDataGen(workflowScheme.getId()).name(stepName).order(ordinal++).nextPersisted();
            persistedStepsMap.put(stepName, workflowStep);
            persistedStepsList.add(workflowStep);
        }

        for (WorkflowStep currentStep : persistedStepsList) {
            final List<Tuple3<String, String, Set<WorkflowState>>> workflowActions = stepsAndActions
                    .get(currentStep.getName());

            for (final Tuple3<String, String, Set<WorkflowState>> workflowAction : workflowActions) {
                final String actionName = workflowAction._1();

                persistedActionsMap.computeIfAbsent(actionName, s -> {
                    final String nextStepName = workflowAction._2();
                    final String nextStepId;
                    try {
                        nextStepId =
                                UtilMethods.isSet(nextStepName) ?
                                        ("Current Step".equals(nextStepName)
                                                ? currentStep.getId()
                                                : persistedStepsMap.get(nextStepName).getId()
                                        ) : null;
                    } catch (final Exception e) {
                        throw new RuntimeException(
                                "Unable to find a step by the name `" + nextStepName
                                        + "`. Check your Workflow definition.");
                    }

                    Set<WorkflowState> showOn = workflowAction._3();
                    showOn = (showOn == null ? WorkflowAPI.DEFAULT_SHOW_ON : showOn);

                    //Now associate actions to the steps
                    final WorkflowAction action = new WorkflowActionDataGen(workflowScheme.getId(),
                            currentStep.getId())
                            .name(actionName)
                            .nextStep(nextStepId).showOn(showOn)
                            .nextPersisted();

                    new WorkflowActionClassDataGen(action.getId()).nextPersisted();
                    return action;
                });

            }
        }

        return workflowScheme;
    }

}