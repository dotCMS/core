package com.dotmarketing.fixtask.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.listeners.CommitAPI;
import com.dotmarketing.db.listeners.FlushCacheListener;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

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
    private static final String VERIFICATION_QUERY = "SELECT inode AS inode FROM relationship WHERE NOT EXISTS " +
            "(SELECT * FROM structure WHERE relationship.parent_structure_inode = structure.inode) " +
            " OR NOT EXISTS " +
            "(SELECT * FROM structure WHERE relationship.child_structure_inode = structure.inode) ";

    @Override
    @WrapInTransaction
    public List<Map<String, Object>> executeFix() throws DotDataException {
        Logger.info(FixTask00095DeleteOrphanRelationships.class,
                "Beginning DeleteOrphanRelationships");
        final List<Map<String, Object>> returnValue = new ArrayList<>();

        if (!FixAssetsProcessStatus.getRunning()) {
            int total = 0;
            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus
                        .setDescription("task 95: DeleteOrphanRelationships");
                final DotConnect dc = new DotConnect();
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
                        "An issue happened during execution of DeleteOrphanRelationships task",
                        e1);
                FixAssetsProcessStatus.setActual(-1);
            } catch (Exception e2) {
                Logger.debug(
                        FixTask00095DeleteOrphanRelationships.class,
                        "There was an unexpected problem during execution of DeleteOrphanRelationships task",
                        e2);
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
            final XStream xstreamObj = new XStream(new DomDriver());
            final LocalDate date = LocalDate.now();
            final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            final String lastModDate = sdf.format(date);
            File writingObj = null;
            if (!new File(ConfigUtils.getBackupPath() + File.separator
                    + fixesUriSubstring).exists()) {
                new File(ConfigUtils.getBackupPath() + File.separator + fixesUriSubstring)
                        .mkdirs();
            }
            writingObj = new File(ConfigUtils.getBackupPath() + File.separator
                    + fixesUriSubstring + File.separator + lastModDate + "_"
                    + "FixTask00095DeleteOrphanRelationships" + ".xml");

            BufferedOutputStream bufferedOutObj = null;
            try {
                bufferedOutObj = new BufferedOutputStream(Files.newOutputStream(writingObj.toPath()));
            } catch (FileNotFoundException e) {
                Logger.error(FixTask00095DeleteOrphanRelationships.class, 
                        "Cannot write FixTask status file. File in filesystem cannot be found.", e);
            } catch (IOException e) {
                Logger.error(FixTask00095DeleteOrphanRelationships.class, 
                        "Cannot write FixTask Status file due to IOException.", e);
            }
            xstreamObj.toXML(new ArrayList<>(modifiedData), bufferedOutObj);
            CloseUtils.closeQuietly(bufferedOutObj);
        }
        return new ArrayList<>(modifiedData);
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
        
        final int total = (inodesInStructure != null && !inodesInStructure.isEmpty()) ? 
                inodesInStructure.size() : 0;
                
        Logger.debug(CMSMaintenanceFactory.class, "Found " + total + " invalid inodes.");
        
        FixAssetsProcessStatus.setTotal(total);
        return total > 0 ? true : false;
    }

    
    private void startCleanup() throws DotDataException {
        try {
            
            cleanUpOrphanRelationships();
            // Set the number of records that were fixed
            FixAssetsProcessStatus.setErrorsFixed(modifiedData.size());
        } catch (SQLException | DotHibernateException e1) {
            Logger.error(this, "There was an unexpected problem with cleaning up relationship in Database.", e1);
            modifiedData.clear();
        }
    }
    
    private void saveFixAuditRecord(final int total) throws DotHibernateException {
        final FixAudit auditObj = new FixAudit();
        auditObj.setTableName("relationship");
        auditObj.setDatetime(new Date());
        auditObj.setRecordsAltered(total);
        auditObj.setAction("task 95: DeleteOrphanRelationships");
        HibernateUtil.save(auditObj);
    }
    
    private void cleanUpOrphanRelationships() throws SQLException, DotDataException{
        
        
        String query1 = "DELETE FROM relationship WHERE parent_structure_inode = ?";
        String query2 = "DELETE FROM relationship WHERE child_structure_inode = ?";
        String query3 = String.format("DELETE FROM inode WHERE NOT EXISTS (SELECT * FROM relationship " + 
                "WHERE relationship.inode = inode.inode) and inode.type like '%s' ",Inode.Type.RELATIONSHIP.getTableName());
        
        List<Relationship> relsToClearInCache = new ArrayList<>();
        
        final DotConnect dc = new DotConnect();
        dc.setSQL(VERIFICATION_QUERY);
        List<Map<String,Object>> results = dc.loadObjectResults();
        
        List<String> relInodes = ((Map<String, String>) results).values()
                .stream().map(Object::toString).collect(Collectors.toList());

        relInodes.forEach((String item) -> {
            try {
                Relationship rel = APILocator.getRelationshipAPI().byInode(item);
                relsToClearInCache.add(rel);    
                
                dc.setSQL(query1);
                dc.addParam(item);        
                dc.loadResult();
                
                dc.setSQL(query2);
                dc.addParam(item);        
                dc.loadResult();
            } catch (DotDataException e){
                Logger.error(this, "There was an unexpected problem with cleaning up relationship in Database.", e);
                modifiedData.clear();
            }
        });
        
        dc.executeStatement(query3);
        flushRelationshipCacheRegion(relsToClearInCache);
    }
    
    private void flushRelationshipCacheRegion(List<Relationship> inodesToClearInCache) throws DotHibernateException {
        CommitAPI.getInstance().addFlushCacheAsync(new FlushCacheListener() {
            
            public void run () {
                //Invalidating Relationships in Cache
                inodesToClearInCache.forEach(
                        (Relationship rel) -> CacheLocator.getRelationshipCache().removeRelationshipByInode(rel));
            }
            
            @Override
            public String key() {
                // TODO Auto-generated method stub
                return String.valueOf(inodesToClearInCache.hashCode());
            }
        }); 
    }
}
