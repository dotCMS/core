package com.dotcms.osgi.fixasset;

import java.util.ArrayList;
import java.util.Date;
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


public class SampleFixTask implements FixTask {



    public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {

        Logger.info(this.getClass(), "Beginning:" + this.getClass().getName());
        int total = 0;
        List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();



        if (!FixAssetsProcessStatus.getRunning()) {
            FixAssetsProcessStatus.startProgress();
            FixAssetsProcessStatus
                    .setDescription("task " + this.getClass().getName() + ": Fixing things!");
            HibernateUtil.startTransaction();
            System.out.println("task " + this.getClass().getName() + ": Fixing things!");



            FixAudit Audit = new FixAudit();
            Audit.setTableName("contentlet");
            Audit.setDatetime(new Date());
            Audit.setRecordsAltered(total);
            Audit.setAction("delete assets with missing identifiers");
            HibernateUtil.save(Audit);

            try {
                returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            FixAssetsProcessStatus.stopProgress();
            FixAssetsProcessStatus.setActual(-1);

        }
        return returnValue;

    }



    public boolean shouldRun() {
        System.out.println("task " + this.getClass().getName() + ": shouldRun()");
        DotConnect dc = new DotConnect();
        final String countInodes = "select count(*) as test from inode";
        dc.setSQL(countInodes);
        return dc.getInt("test") > 0;


    }


    private List<Map<String, String>> modifiedData = new ArrayList<Map<String, String>>();



    @Override
    public List<Map<String, String>> getModifiedData() {

        return modifiedData;
    }

}
