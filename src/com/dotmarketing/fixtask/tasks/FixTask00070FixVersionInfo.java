package com.dotmarketing.fixtask.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.util.UtilMethods;

public class FixTask00070FixVersionInfo implements FixTask {
    
    @Override
    public boolean shouldRun() {
        return true;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
        List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
        if (!FixAssetsProcessStatus.getRunning()) {
            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus.setDescription("70 Fix versionInfo");
                int total=0;
                DotConnect dc=new DotConnect();
                
                String[] versionables=new String[] {
                        "file_asset","htmlpage",
                        "template","containers","links"};
                for(String table : versionables) {
                    String vitable=UtilMethods.getVersionInfoTableName(table);
                    String sql = " select distinct id from "+table+" join identifier on (id=identifier) " +
                    		     " left outer join " + vitable +
                    		     " on("+table+".identifier="+vitable+".identifier) " +
                    		     " where working_inode is null";
                    dc.setSQL(sql);
                    List<Map<String, Object>> results = dc.loadObjectResults();
                    for(Map<String, Object> rr : results) {
                        String id=rr.get("id").toString();
                        sql="select inode from "+table+" where identifier=? order by mod_date desc";
                        dc.setSQL(sql);
                        dc.addParam(id);
                        List<Map<String, Object>> versions = dc.loadObjectResults();
                        String inode=versions.get(0).get("inode").toString();
                        
                        HibernateUtil hu=new HibernateUtil(UtilMethods.getVersionableClass(table));
                        Versionable workingVersion=(Versionable) hu.load(inode);
                        APILocator.getVersionableAPI().setWorking(workingVersion);
                        
                        total++;
                    }
                }
                
                // contentlets are different because of language_id
                String sql="select distinct id,language_id from contentlet join identifier on(id=identifier) " +
                		   "   left outer join contentlet_version_info " +
                		   "   on (contentlet.identifier=contentlet_version_info.identifier " +
                		   "   and contentlet.language_id=contentlet_version_info.lang) " +
                		   " where working_inode is null";
                dc.setSQL(sql);
                List<Map<String, Object>> results = dc.loadObjectResults();
                for(Map<String, Object> rr : results) {
                    String id=rr.get("id").toString();
                    Integer langId=Integer.parseInt(rr.get("language_id").toString());
                    sql="select inode from contentlet where identifier=? and language_id=? order by mod_date desc";
                    dc.setSQL(sql);
                    dc.addParam(id);
                    dc.addParam(langId);
                    List<Map<String, Object>> versions = dc.loadObjectResults();
                    String inode=versions.get(0).get("inode").toString();
                    APILocator.getVersionableAPI().setWorking(
                       APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false)
                    );
                    total++;
                }
                
                FixAssetsProcessStatus.setTotal(total);
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
        return new ArrayList<Map<String,String>>();
    }
    
}
