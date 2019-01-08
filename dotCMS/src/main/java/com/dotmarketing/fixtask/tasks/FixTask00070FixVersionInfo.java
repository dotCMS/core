package com.dotmarketing.fixtask.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.UtilMethods;

public class FixTask00070FixVersionInfo implements FixTask {
    
    @Override
    public boolean shouldRun() {
        return true;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> executeFix() throws DotRuntimeException {
        final List<Map<String, Object>> returnValue = new ArrayList<>();
        if (!FixAssetsProcessStatus.getRunning()) {
            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus.setDescription("70 Fix versionInfo");
                StringBuffer sb;
                DotConnect dc=new DotConnect();
                
                String[] versionables=new String[] {
                        "template", Inode.Type.CONTAINERS.getTableName(),"links"};
                for(String table : versionables) {
                    String vitable;
                    if(Inode.Type.CONTAINERS.getTableName().equals(table)){
                        vitable = Inode.Type.CONTAINERS.getVersionTableName();
                    } else {
                        vitable = Inode.Type.valueOf(table.toUpperCase()).getVersionTableName();
                    }
                    sb = new StringBuffer(" select distinct id from "+table+" join identifier on (id=identifier) " +
                    		     " left outer join " + vitable +
                    		     " on("+table+".identifier="+vitable+".identifier) " +
                    		     " where working_inode is null");
                    dc.setSQL(sb.toString());
                    List<Map<String, Object>> results = dc.loadObjectResults();
                    FixAssetsProcessStatus.addTotal(results.size());
                    for(Map<String, Object> rr : results) {
                        String id=rr.get("id").toString();
                        sb = new StringBuffer("select inode from "+table+" where identifier=? order by mod_date desc");
                        dc.setSQL(sb.toString());
                        dc.addParam(id);
                        List<Map<String, Object>> versions = dc.loadObjectResults();
                        String inode=versions.get(0).get("inode").toString();
                        
                        HibernateUtil hu=new HibernateUtil(UtilMethods.getVersionableClass(table));
                        Versionable workingVersion=(Versionable) hu.load(inode);
                        APILocator.getVersionableAPI().setWorking(workingVersion);
                        
                        FixAssetsProcessStatus.addAErrorFixed();
                    }
                }

                // contentlets are different because of language_id
                sb = new StringBuffer("select distinct id,language_id from contentlet join identifier on(id=identifier) " +
                        "   left outer join contentlet_version_info " +
                        "   on (contentlet.identifier=contentlet_version_info.identifier " +
                        "   and contentlet.language_id=contentlet_version_info.lang) " +
                        " where working_inode is null");
                dc.setSQL(sb.toString());
                List<Map<String, Object>> results = dc.loadObjectResults();
                FixAssetsProcessStatus.addTotal(results.size());
                for(Map<String, Object> rr : results) {
                    String id=rr.get("id").toString();
                    Integer langId=Integer.parseInt(rr.get("language_id").toString());
                    sb = new StringBuffer("select inode from contentlet where identifier=? and language_id=? order by mod_date desc");
                    dc.setSQL(sb.toString());
                    dc.addParam(id);
                    dc.addParam(langId);
                    List<Map<String, Object>> versions = dc.loadObjectResults();
                    String inode=versions.get(0).get("inode").toString();
                    APILocator.getVersionableAPI().setWorking(
                       APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false)
                    );

                    FixAssetsProcessStatus.addAErrorFixed();
                }

                returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
            }
            catch(Exception ex) {
                FixAssetsProcessStatus.setActual(-1);
            }
            finally {
                FixAssetsProcessStatus.stopProgress();
                CacheLocator.getVersionableCache().clearCache();
            }
        }
        
        return returnValue;
    }
    
    @Override
    public List<Map<String, String>> getModifiedData() {
        return new ArrayList<>();
    }
    
}
