package com.dotmarketing.fixtask.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;
import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

/**
 * Fix broken relationships 
 * pointing to non-existing Content Types in Structure Table 
 * @author joseorsini
 *
 */

public class FixTask00095DeleteOrphanRelationships implements FixTask{

    private List<Map<String, String>> modifiedData = new ArrayList<>();

    /** Lookup of records in Relationship table pointing 
     * to missing Content Types in the Structure table */
    private static final String VERIFICATION_QUERY = "SELECT inode FROM relationship WHERE NOT EXISTS " +
            "(SELECT * FROM structure WHERE relationship.parent_structure_inode = structure.inode) " +
            " OR NOT EXISTS " +
            "(SELECT * FROM structure WHERE relationship.child_structure_inode = structure.inode) ";

    @Override
    public List<Map<String, Object>> executeFix() throws DotDataException {
        Logger.info(FixTask00095DeleteOrphanRelationships.class,
                "Beginning DeleteOrphanRelationships");
        List<Map<String, Object>> returnValue = new ArrayList<>();

        if (!FixAssetsProcessStatus.getRunning()) {
            HibernateUtil.startTransaction();
            int total = 0;
            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus
                        .setDescription("task 95: DeleteOrphanRelationships");
                DotConnect dc = new DotConnect();
                dc.setSQL(VERIFICATION_QUERY);
                modifiedData = dc.loadResults();
                total = modifiedData != null ? modifiedData.size() : 0;
                FixAssetsProcessStatus.setTotal(total);
                getModifiedData();
                if (total > 0) {
                    startCleanup();
                    saveFixAuditRecord(total);
                    returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
                    Logger.debug(
                            FixTask00095DeleteOrphanRelationships.class,
                            "Ending DeleteOrphanRelationships");
                }
            } catch (DotHibernateException e1) {
                Logger.debug(
                        FixTask00095DeleteOrphanRelationships.class,
                        "A Hibernate Exception was detected during execution of DeleteOrphanRelationships task",
                        e1);
                HibernateUtil.rollbackTransaction();
                FixAssetsProcessStatus.setActual(-1);
            } catch (Exception e2) {
                Logger.debug(
                        FixTask00095DeleteOrphanRelationships.class,
                        "There was an unexpected problem during execution of DeleteOrphanRelationships task",
                        e2);
                HibernateUtil.rollbackTransaction();
                FixAssetsProcessStatus.setActual(-1);
            } finally {
                FixAssetsProcessStatus.stopProgress();
            }
        }
        return returnValue;
    }

    @Override
    public List<Map<String, String>> getModifiedData() {
        if (modifiedData.isEmpty()) {
            final String fixesUriSubstring = "fixes";
            XStream xstreamObj = new XStream(new DomDriver());
            LocalDate date = LocalDate.now();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String lastmoddate = sdf.format(date);
            File writingObj = null;
            if (!new File(ConfigUtils.getBackupPath() + File.separator
                    + fixesUriSubstring).exists()) {
                new File(ConfigUtils.getBackupPath() + File.separator + fixesUriSubstring)
                        .mkdirs();
            }
            writingObj = new File(ConfigUtils.getBackupPath() + File.separator
                    + fixesUriSubstring + java.io.File.separator + lastmoddate + "_"
                    + "FixTask00095DeleteOrphanRelationships" + ".xml");

            BufferedOutputStream bufferedOutObj = null;
            try {
                bufferedOutObj = new BufferedOutputStream(new FileOutputStream(writingObj));
            } catch (FileNotFoundException e) {
                Logger.error(FixTask00095DeleteOrphanRelationships.class, 
                        "Could not write to Fix Task status file.", e);
            }
            xstreamObj.toXML(modifiedData, bufferedOutObj);
        }
        return modifiedData;
    }

    @Override
    public boolean shouldRun() {
        Logger.debug(CMSMaintenanceFactory.class, "Running query for relationships: "
                + VERIFICATION_QUERY);
        DotConnect dc = new DotConnect();
        dc.setSQL(VERIFICATION_QUERY);
        List<Map<String, Object>> inodesInStructure = new ArrayList<>();
        
        try {
            inodesInStructure = dc.loadObjectResults();
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
        }
        
        int total = (inodesInStructure != null && !inodesInStructure.isEmpty()) ? 
                inodesInStructure.size() : 0;
                
        Logger.debug(CMSMaintenanceFactory.class, "Found " + total + " invalid inodes.");
        
        FixAssetsProcessStatus.setTotal(total);
        return total > 0 ? true : false;
    }

    
    private void startCleanup() throws DotHibernateException {
        try {
            HibernateUtil.startTransaction();
            cleanUpOrphanRelationships();
            HibernateUtil.commitTransaction();
            // Set the number of records that were fixed
            FixAssetsProcessStatus.setErrorsFixed(modifiedData.size());
        } catch (DotHibernateException e1) {
            Logger.error(this, "Unable to clean orphaned relationships.", e1);
            HibernateUtil.rollbackTransaction();
            modifiedData.clear();
        } catch (SQLException e2) {
            Logger.error(this, "There was an unexpected problem with cleaning up relationship in Database.", e2);
            HibernateUtil.rollbackTransaction();
            modifiedData.clear();
        } finally {
            flushCacheRegions();
        } 
    }
    
    private void saveFixAuditRecord(int total) throws DotHibernateException {
        FixAudit auditObj = new FixAudit();
        auditObj.setTableName("relationship");
        auditObj.setDatetime(new Date());
        auditObj.setRecordsAltered(total);
        auditObj.setAction("task 95: DeleteOrphanRelationships");
        HibernateUtil.save(auditObj);
        HibernateUtil.commitTransaction();
    }
    
    private void cleanUpOrphanRelationships() throws SQLException{
        final String query1 = "DELETE FROM relationship WHERE NOT EXISTS " 
                + " (SELECT * FROM structure WHERE structure.inode = relationship.parent_structure_inode)";
        final String query2 = "DELETE FROM relationship WHERE NOT EXISTS " 
                + " (SELECT * FROM structure WHERE structure.inode = relationship.child_structure_inode)";
        final String query3 = String.format("DELETE FROM inode WHERE NOT EXISTS (SELECT * FROM relationship " + 
                "WHERE relationship.inode = inode.inode) and inode.type like '%s' ",Inode.Type.RELATIONSHIP.getTableName());
        
        DotConnect dc = new DotConnect();
        dc.executeStatement(query1);
        dc.executeStatement(query2);
        dc.executeStatement(query3);
        
    }
    
    private void flushCacheRegions() {
        CacheLocator.getRelationshipCache().clearCache();
        CacheLocator.getContentTypeCache().clearCache();
        CacheLocator.getContentTypeCache2().clearCache();
        
    }
}
