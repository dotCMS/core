package com.dotmarketing.fixtask.tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;

public class FixTask00085FixEmptyParentPathOnIdentifier implements FixTask {

    private int total = 0;

    @Override
    public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {

        List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();

        Logger.info(FixTask00085FixEmptyParentPathOnIdentifier.class,
                "Beginning FixTask00085FixEmptyParentPathOnIdentifier");

        if (!FixAssetsProcessStatus.getRunning()) {

            FixAssetsProcessStatus.startProgress();
            FixAssetsProcessStatus
                    .setDescription("Task 85: Updating Empty Parent Path in Identifiers");
            FixAssetsProcessStatus.setTotal(total);

            try {
                HibernateUtil.startTransaction();
                DotConnect dc = new DotConnect();

                dc.setSQL(
                        "update identifier set parent_path = '/' where parent_path is null or parent_path = ''");

                try {
                    dc.loadResult();
                } catch (DotDataException e) {
                    Logger.error(this, e.getMessage(), e);
                }

                FixAssetsProcessStatus.setErrorsFixed(total);

                FixAudit Audit = new FixAudit();
                Audit.setTableName("identifier");
                Audit.setDatetime(new Date());
                Audit.setRecordsAltered(total);
                Audit.setAction("Task 85: Fixed EmptyParentPathInIdentifiers");
                HibernateUtil.save(Audit);
                HibernateUtil.commitTransaction();
                MaintenanceUtil.flushCache();

                returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
                FixAssetsProcessStatus.stopProgress();

                Logger.debug(FixTask00085FixEmptyParentPathOnIdentifier.class,
                        "Ending FixEmptyParentPathInIdentifiers");

            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                HibernateUtil.rollbackTransaction();
                FixAssetsProcessStatus.stopProgress();
                FixAssetsProcessStatus.setActual(-1);
            }
        }
        return returnValue;
    }

    @Override
    public List<Map<String, String>> getModifiedData() {
        return new ArrayList<Map<String, String>>();
    }

    @Override
    public boolean shouldRun() {
        List<HashMap<String, String>> identifiers = null;

        DotConnect db = new DotConnect();
        String query = "select id, parent_path from identifier where parent_path is null or parent_path = ''";
        db.setSQL(query);
        try {
            identifiers = db.getResults();

            total = identifiers.size();
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
        }

        return (total > 0);
    }
}
