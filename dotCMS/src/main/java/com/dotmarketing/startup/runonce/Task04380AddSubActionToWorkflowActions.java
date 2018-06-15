package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import java.util.List;
import java.util.Map;

/**
 * Upgrade task that will add to all the existing Actions that have the Requires Lock
 * (requires_checkout) in TRUE a <strong>Save content</strong> sub-action but excluding the Actions
 * of the System Workflow. The <strong>Save content</strong> sub-action should be first in the list
 * of sub-actions.
 *
 * @author Jonathan Gamba 6/8/18
 */
public class Task04380AddSubActionToWorkflowActions implements StartupTask {

    private static final String SYSTEM_WORKFLOW_ID = WorkflowAPI.SYSTEM_WORKFLOW_ID;

    private static final String SELECT_ACTIONS = "SELECT id FROM workflow_action WHERE scheme_id != ? AND requires_checkout = ?";
    private static final String INCREMENT_ACTION_CLASS_ORDER = "UPDATE workflow_action_class SET my_order = my_order + 1 where action_id = ?";
    private static final String INSERT_ACTION_CLASS = "INSERT INTO workflow_action_class (id, action_id, name, my_order, clazz) VALUES (?, ?, ?, ?, ?)";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final DotConnect dotConnect = new DotConnect();

        Logger.info(this, "Adding Save Content Actiontlet to the 'workflow_action_class' table.");

        /*
        First get the list of actions to modify:
        WHERE scheme_id != SYSTEM_WORFLOW AND requires_checkout = TRUE;
         */
        final List<Map<String, Object>> results =
                dotConnect.setSQL(SELECT_ACTIONS).addParam(SYSTEM_WORKFLOW_ID)
                        .addParam(Boolean.TRUE).loadObjectResults();

        results.forEach(row -> {

            String actionId = (String) row.get("id");

            /*
            Increment the order of each actionlet of this action id in order to prepare everything
            to insert the new actionlet in the first position.
             */
            incrementActionletsOrderForAction(dotConnect, actionId);

            //Insert the Save Content Actionlet to this Workflow Action
            insertActionletToAction(dotConnect, actionId);
        });
    }

    /**
     * Updates the order of the Actionlets associated to a given Action incrementing in 1 the order
     * column (my_order) value in order to allow a new Actionlet to be inserted in the first
     * position (0).
     */
    private void incrementActionletsOrderForAction(DotConnect dc, String actionId) {

        dc.setSQL(INCREMENT_ACTION_CLASS_ORDER);
        dc.addParam(actionId);

        Logger.debug(this,
                "Updating to workflow_action_class Actiontlets order for the Action: "
                        + actionId);

        try {
            dc.loadResult();
        } catch (DotDataException e) {

            Logger.error(this,
                    "ERROR on updating to workflow_action_class Actiontlets order for the Action: "
                            + actionId + ", err:" + e.getMessage(), e);
            throw new DotRuntimeException(
                    "ERROR on updating Actionlets order.",
                    e);
        }
    }

    /**
     * Inserts into a given action the <strong>Save content</strong> Actionlet in the first
     * position
     */
    private void insertActionletToAction(DotConnect dc, String actionId) {

        dc.setSQL(INSERT_ACTION_CLASS);
        dc.addParam(UUIDGenerator.generateUuid());
        dc.addParam(actionId);
        dc.addParam("Save content");
        dc.addParam(0);
        dc.addParam("com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet");

        Logger.debug(this,
                "Adding to workflow_action_class the Save Content Actiontlet for the Action: "
                        + actionId);

        try {
            dc.loadResult();
        } catch (DotDataException e) {

            Logger.error(this, "ERROR on adding Actionlet to workflow_action_class for Action: "
                    + actionId + ", err:" + e.getMessage(), e);
            throw new DotRuntimeException(
                    "ERROR on adding Actionlet to 'workflow_action_class' table.",
                    e);
        }
    }

}