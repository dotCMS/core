

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationFeature;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.velocity.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This upgrade task associated the system workflow to all contentlet that are not already running a workflow.
 * Depending of the state of last version, will set to the step published, unpublish, archive.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04340IncludeSystemWorkflowOnContentlets implements StartupTask {


    private
    public    static final String SYSTEMWORKFLOW_JSON_PATH = "com/dotmarketing/startup/runonce/json/systemworkflow.json";
    protected static final String SELECT_SCHEME_SQL        = "select * from workflow_scheme where id = ?";
    protected static final String INSERT_SCHEME            = "insert into workflow_scheme (id, name, description, archived, mandatory, entry_action_id, default_scheme, mod_date) values (?,?,?,?,?,?,?,?)";
    protected static final String INSERT_STEP              = "insert into workflow_step (id, name, scheme_id,my_order,resolved,escalation_enable,escalation_action,escalation_time) values (?, ?, ?, ?, ?, ?, ?, ?) ";
    protected static final String INSERT_ACTION            = "insert into workflow_action (id, scheme_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout, show_on) values (?, ?, ?, ?, ?, ?, ?,?, ?, ?,?,?,?)";
    protected static final String INSERT_ACTION_FOR_STEP   = "insert into workflow_action_step(action_id, step_id, action_order) values (?,?,?)";
    protected static final String INSERT_ACTION_CLASS      = "insert into workflow_action_class (id, action_id, name, my_order, clazz) values (?,?, ?, ?, ?)";
    protected static final String DELIMITER                = ",";


    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {


    } // executeUpgrade.

}
