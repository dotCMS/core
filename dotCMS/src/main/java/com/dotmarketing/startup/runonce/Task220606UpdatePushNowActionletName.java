package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet;
import com.dotmarketing.startup.StartupTask;

/**
 * Startup tas that renames any exising entry on workflow_action_class associated with `PushNowActionlet` Class and changes display name
 */
public class Task220606UpdatePushNowActionletName implements StartupTask {

    private final DotConnect dotConnect = new DotConnect();

    @Override
    public boolean forceRun() {
        final String name = dotConnect.setSQL("SELECT name FROM workflow_action_class WHERE clazz = ? ").addParam(PushNowActionlet.class.getName()).getString("name");
        return (null != name && !new PushNowActionlet().getName().equals(name));
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        dotConnect.setSQL(" UPDATE workflow_action_class SET name = ? WHERE clazz = ? ")
                .addParam(new PushNowActionlet().getName())
                .addParam(PushNowActionlet.class.getName())
                .loadResult();
        CacheLocator.getWorkFlowCache().clearCache();
    }
}
