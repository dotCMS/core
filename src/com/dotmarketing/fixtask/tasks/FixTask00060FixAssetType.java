package com.dotmarketing.fixtask.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class FixTask00060FixAssetType implements FixTask {
    
    private List<Map<String, String>> modifiedData = new ArrayList<Map<String,String>>();
    
    @Override
    public boolean shouldRun() {
        return true;
    }
    
    @SuppressWarnings({"unchecked","deprecation"})
    @Override
    public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
        List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
        if (!FixAssetsProcessStatus.getRunning()) {
            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus.setDescription("60 Fix asset_type");
                int total=0;
                DotConnect dc=new DotConnect();
                
                // contentlets
                dc.setSQL("select identifier.* from contentlet join identifier on (identifier=id) where asset_type<>'contentlet'");
                List<Map<String,String>> results = dc.getResults();
                total+=results.size();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='contentlet' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                    modifiedData.add(rr);
                }
                
                // files
                dc.setSQL("select identifier.* from file_asset join identifier on (identifier=id) where asset_type<>'file_asset'");
                results = dc.getResults();
                total+=results.size();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='file_asset' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                }
                
                // containers
                dc.setSQL("select identifier.* from containers join identifier on (identifier=id) where asset_type<>'containers'");
                results = dc.getResults();
                total+=results.size();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='containers' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                    modifiedData.add(rr);
                }
                
                // templates
                dc.setSQL("select identifier.* from template join identifier on (identifier=id) where asset_type<>'template'");
                results = dc.getResults();
                total+=results.size();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='template' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                }
                
                // links
                dc.setSQL("select identifier.* from links join identifier on (identifier=id) where asset_type<>'links'");
                results = dc.getResults();
                total+=results.size();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='links' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                }
                
                // htmlpage
                dc.setSQL("select identifier.* from htmlpage join identifier on (identifier=id) where asset_type<>'htmlpage'");
                results = dc.getResults();
                total+=results.size();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='htmlpage' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                }
                
                // folder
                dc.setSQL("select identifier.* from folder join identifier on (identifier=id) where asset_type<>'folder'");
                results = dc.getResults();
                for(Map<String,String> rr : results) {
                    try {
                        dc.setSQL("update identifier set asset_type='folder' where id=?");
                        dc.addParam(rr.get("id"));
                        dc.loadResult();
                        modifiedData.add(rr);
                        total++;
                    } catch(Exception ex) {
                        Logger.warn(this, "error fixing asset_type on id="+rr.get("id"));
                    }
                }
                
                FixAssetsProcessStatus.setTotal(total);
                returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
                
            }
            catch(Exception ex) {
                FixAssetsProcessStatus.setActual(-1);
            }
            finally {
                FixAssetsProcessStatus.stopProgress();
                CacheLocator.getIdentifierCache().clearCache();
            }
        }
        
        return returnValue;
    }
    
    @Override
    public List<Map<String, String>> getModifiedData() {
        if (modifiedData.size() > 0) {
            XStream _xstream = new XStream(new DomDriver());
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String lastmoddate = sdf.format(date);
            File _writing = null;

            if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
                new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdir();
            }
            _writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator  + lastmoddate + "_"
                    + "FixTask00060FixAssetType" + ".xml");

            BufferedOutputStream _bout = null;
            try {
                _bout = new BufferedOutputStream(new FileOutputStream(_writing));
            } catch (FileNotFoundException e) {

            }
            _xstream.toXML(modifiedData, _bout);
        }
        return modifiedData;
    }
    
}
