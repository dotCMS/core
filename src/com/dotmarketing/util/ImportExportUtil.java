/**
 *
 */
package com.dotmarketing.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import com.dotmarketing.logConsole.model.LogMapperRow;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.persister.AbstractEntityPersister;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.PortletPreferences;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;


/**
 * @author Jason Tesser
 * @version 1.6
 *
 */
public class ImportExportUtil {
    
    /**
     * The path where tmp files are stored. This gets wiped alot
     */
    private String backupTempFilePath = ConfigUtils.getBackupPath()+File.separator+"temp";
    private ArrayList<String> classesWithIdentity = new ArrayList<String>();
    private Map<String, String> sequences;
    private Map<String, String> tableIDColumns;
    private Map<String, String> tableNames;
    private String dbType = DbConnectionFactory.getDBType();
    private static String assetRealPath = null;
    private static String assetPath = "/assets";
    private File companyXML;
    private File userXML;
    private File rolesLayoutsXML;
    private File layoutsPortletsXML;
    private File pluginPropertyXML;
    private File logMapperRowXML;
    private List<File> dashboardUserPreferencesXMLs = new ArrayList<File>();
    private List<File> analyticSummary404XMLs = new ArrayList<File>();
    private List<File> analyticSummaryRefererXMLs = new ArrayList<File>();
    private List<File> analyticSummaryContentXMLs = new ArrayList<File>();
    private List<File> analyticSummaryXMLs = new ArrayList<File>();
    private List<File> analyticSummaryPagesXMLs = new ArrayList<File>();
    private List<File> analyticSummaryVisitsXMLs = new ArrayList<File>();
    private List<File> roleXMLs = new ArrayList<File>();
    private List<File> usersRolesXML = new ArrayList<File>();
    private List<File> treeXMLs = new ArrayList<File>();
    private List<File> permissionXMLs = new ArrayList<File>();
    private List<File> contentletsXML = new ArrayList<File>();
    private List<File> menuLinksXML = new ArrayList<File>();
    private List<File> pagesXML = new ArrayList<File>();
    private List<File> structuresXML = new ArrayList<File>();
    private List<File> containersXML = new ArrayList<File>();
    private List<File> identifiersXML = new ArrayList<File>();
    private List<File> foldersXML = new ArrayList<File>();
    private List<File> templatesXML = new ArrayList<File>();
    private List<File> templateContainersXML = new ArrayList<File>();
    private List<File> filesXML = new ArrayList<File>();
    private List<File> versionInfoFilesXML = new ArrayList<File>();
    private List<File> workFlowTaskXML = new ArrayList<File>();
    private List<File> workFlowCommentXML = new ArrayList<File>();
    private List<File> workFlowHistoryXML = new ArrayList<File>();
    private List<File> workFlowTaskFilesXML = new ArrayList<File>();
    private List<File> tagFiles = new ArrayList<File>();
    private File workflowSchemaFile = null;
    
    public ImportExportUtil() {
        MaintenanceUtil.flushCache();
        // Set the asset paths
        try {
            assetRealPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }
        //classesWithIdentity.add("Inode");
        classesWithIdentity.add("Rating");
        classesWithIdentity.add("dist_journal");
        classesWithIdentity.add("Language");
        classesWithIdentity.add("Permission");
        classesWithIdentity.add("PermissionReference");
        classesWithIdentity.add("UserPreference");
        //classesWithIdentity.add("WebForm");
        classesWithIdentity.add("UsersToDelete");
        //Dashboard Tables
        classesWithIdentity.add("Clickstream404");
        classesWithIdentity.add("DashboardUserPreferences");
        classesWithIdentity.add("DashboardWorkStream");
        classesWithIdentity.add("DashboardSummaryPeriod");
        classesWithIdentity.add("DashboardSummaryReferer");
        classesWithIdentity.add("DashboardSummary404");
        classesWithIdentity.add("DashboardSummaryPage");
        classesWithIdentity.add("DashboardSummary");
        classesWithIdentity.add("DashboardSummaryContent");
        classesWithIdentity.add("DashboardSummaryVisits");
        
        tableNames = new HashMap<String, String>();
        //tableNames.put("Inode", "inode");
        tableNames.put("Rating", "content_rating");
        tableNames.put("dist_journal", "dist_journal");
        tableNames.put("Language", "language");
        tableNames.put("Permission", "permission");
        tableNames.put("PermissionReference", "permission_reference");
        tableNames.put("UserPreference", "user_preferences");
        //tableNames.put("WebForm", "web_form");
        tableNames.put("UsersToDelete", "users_to_delete");
        //Dashboard Tables
        tableNames.put("Clickstream404","clickstream_404");
        tableNames.put("DashboardUserPreferences", "dashboard_user_preferences");
        tableNames.put("DashboardWorkStream", "analytic_summary_workstream");
        tableNames.put("DashboardSummaryPeriod", "analytic_summary_period");
        tableNames.put("DashboardSummaryReferer", "analytic_summary_referer");
        tableNames.put("DashboardSummary404", "analytic_summary_404");
        tableNames.put("DashboardSummaryPage", "analytic_summary_pages");
        tableNames.put("DashboardSummary", "analytic_summary");
        tableNames.put("DashboardSummaryContent", "analytic_summary_content");
        tableNames.put("DashboardSummaryVisits", "analytic_summary_visits");
        if(dbType.equals(DbConnectionFactory.POSTGRESQL) || dbType.equals(DbConnectionFactory.ORACLE)){
            sequences = new HashMap<String, String>();
            //sequences.put("inode", "inode_seq");
            sequences.put("content_rating", "content_rating_sequence");
            sequences.put("dist_journal", "dist_journal_id_seq");
            sequences.put("language", "language_seq");
            sequences.put("permission", "permission_seq");
            sequences.put("permission_reference", "permission_reference_seq");
            sequences.put("user_preferences", "user_preferences_seq");
            //sequences.put("web_form", "web_form_seq");
            sequences.put("users_to_delete", "user_to_delete_seq");
            //Dashboard Tables
            sequences.put("clickstream_404","clickstream_404_seq");
            sequences.put("dashboard_user_preferences", "dashboard_usrpref_seq");
            sequences.put("analytic_summary_workstream", "workstream_seq");
            sequences.put("analytic_summary_period", "summary_period_seq");
            sequences.put("analytic_summary_referer", "summary_referer_seq");
            sequences.put("analytic_summary_404", "summary_404_seq");
            sequences.put("analytic_summary_pages", "summary_pages_seq");
            sequences.put("analytic_summary", "summary_seq");
            sequences.put("analytic_summary_content", "summary_content_seq");
            sequences.put("analytic_summary_visits", "summary_visits_seq");
            tableIDColumns = new HashMap<String, String>();
            //tableIDColumns.put("inode", "inode");
            tableIDColumns.put("content_rating", "id");
            tableIDColumns.put("dist_journal", "id");
            tableIDColumns.put("language", "id");
            tableIDColumns.put("permission", "id");
            tableIDColumns.put("permission_reference", "id");
            tableIDColumns.put("user_preferences", "id");
            //tableIDColumns.put("web_form", "web_form_id");
            tableIDColumns.put("users_to_delete", "id");
            //Dashboard Tables
            tableIDColumns.put("clickstream_404","clickstream_404_id");
            tableIDColumns.put("dashboard_user_preferences", "id");
            tableIDColumns.put("analytic_summary_workstream", "id");
            tableIDColumns.put("analytic_summary_period", "id");
            tableIDColumns.put("analytic_summary_referer", "id");
            tableIDColumns.put("analytic_summary_404", "id");
            tableIDColumns.put("analytic_summary_pages", "id");
            tableIDColumns.put("analytic_summary", "id");
            tableIDColumns.put("analytic_summary_content", "id");
            tableIDColumns.put("analytic_summary_visits", "id");
        }
    }
    
    /**
     * Takes a zip file from the temp directory to restore dotCMS data. Currently it will blow away all current data
     * This method cannot currently be run in a transaction. For performance reasons with db drivers and connections it closes the
     * session every so often.
     * @param out A print writer for output
     * @throws IOException
     */
    public void doImport(PrintWriter out) throws IOException {
        File f = new File(getBackupTempFilePath());
        //		String[] _tempFiles = f.list(new XMLFileNameFilter());
        String[] _tempFiles = f.list();
        out.println("<pre>Found " + _tempFiles.length + " files to import");
        Logger.info(this, "Found " + _tempFiles.length + " files to import");
        deleteDotCMS();
        
        File assetDir = null;
        boolean hasAssetDir = false;
        for (int i = 0; i < _tempFiles.length; i++) {
            try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            File _importFile = new File(getBackupTempFilePath() + "/" + _tempFiles[i]);
            
            if(_importFile.isDirectory()){
                if(_importFile.getName().equals("asset")){
                    hasAssetDir = true;
                    assetDir = new File(_importFile.getPath());
                }
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.contentlet.business.Contentlet_")){
                contentletsXML.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.templates.model.Template_")){
                templatesXML.add(new File(_importFile.getPath()));	
            }else if(_importFile.getName().contains("com.dotmarketing.beans.TemplateContainers_")) {
                templateContainersXML.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.containers.model.Container_")){
                containersXML.add(new File(_importFile.getPath()));	
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.files.model.File_")){
                filesXML.add(new File(_importFile.getPath()));	
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.htmlpages.model.HTMLPage_")){
                pagesXML.add(new File(_importFile.getPath()));	
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.links.model.Link_")){
                menuLinksXML.add(new File(_importFile.getPath()));	
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.structure.model.Structure_")){
                structuresXML.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.business.LayoutsRoles_")){
                rolesLayoutsXML = new File(_importFile.getPath());
            }else if(_importFile.getName().contains("com.dotmarketing.business.UsersRoles_")){
                usersRolesXML.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.business.PortletsLayouts_")){
                layoutsPortletsXML = new File(_importFile.getPath());
            }else if(_importFile.getName().contains("com.dotmarketing.plugin.model.PluginProperty_")){
                pluginPropertyXML = new File(_importFile.getPath());
            }else if(_importFile.getName().endsWith( "LogsMappers.xml" )){
                logMapperRowXML = new File(_importFile.getPath());
            }else if(_importFile.getName().contains("com.dotmarketing.beans.Tree_")){
                treeXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.business.Role_")){
                roleXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardUserPreferences_")){
                dashboardUserPreferencesXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardSummary404_")){
                analyticSummary404XMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer_")){
                analyticSummaryRefererXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent_")){
                analyticSummaryContentXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardSummary_")){
                analyticSummaryXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage_")){
                analyticSummaryPagesXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.dashboard.model.DashboardSummaryVisits_")){
                analyticSummaryVisitsXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().contains("com.dotmarketing.beans.Permission_")){
                permissionXMLs.add(new File(_importFile.getPath()));
            }else if(_importFile.getName().endsWith("User.xml")){
                userXML = new File(_importFile.getPath());
            }else if(_importFile.getName().endsWith("Company.xml")){
                companyXML = new File(_importFile.getPath());
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.folders.model.Folder_")){
                foldersXML.add(_importFile);
            }else if(_importFile.getName().contains("VersionInfo_")){
                versionInfoFilesXML.add(_importFile);
            }else if(_importFile.getName().contains("com.dotmarketing.beans.Identifier_")){
                identifiersXML.add(_importFile);
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.workflows.model.WorkflowTask_")){
            	workFlowTaskXML.add(_importFile);
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.workflows.model.WorkflowComment_")){
            	workFlowCommentXML.add(_importFile);
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.workflows.model.WorkflowHistory_")){
            	workFlowHistoryXML.add(_importFile);
            }else if(_importFile.getName().contains("com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles_")){
            	workFlowTaskFilesXML.add(_importFile);
            }else if(_importFile.getName().contains("com.dotmarketing.tag.model.Tag_")){
            	tagFiles.add(0,_importFile);
	        }else if(_importFile.getName().contains("com.dotmarketing.tag.model.TagInode_")){
	        	tagFiles.add(tagFiles.size(),_importFile);
            }else if(_importFile.getName().contains("WorkflowSchemeImportExportObject.json")){
            	workflowSchemaFile = _importFile;
            }else if(_importFile.getName().endsWith(".xml")){
                try {
                    doXMLFileImport(_importFile, out);
                } catch (Exception e) {
                    Logger.error(this, "Unable to load " + _importFile.getName() + " : " + e.getMessage(), e);
                }
            }
            out.flush();
        }
        
        try {
            doXMLFileImport(companyXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + companyXML.getName() + " : " + e.getMessage(), e);
        }
        List<Role> roles = new ArrayList<Role>();
        String _className = null;
        for (File file : roleXMLs) {
            Reader charStream = null;
            XStream _xstream = null;
            Class _importClass = null;
            
            Pattern p = Pattern.compile("_[0-9]{8}");
            Matcher m  = p.matcher(file.getName());
            if(m.find()){
                _className = file.getName().substring(0, file.getName().lastIndexOf("_"));
            }
            else{
                _className = file.getName().substring(0, file.getName().lastIndexOf("."));
            }
            
            try{
                _importClass = Class.forName(_className);
            }catch (Exception e) {
                Logger.error(this, "Class not found " + _className, e);
                return;
            }
            
            _xstream = new XStream();
            
            try{
                charStream = new InputStreamReader(new FileInputStream(file), "UTF-8");
            }catch (UnsupportedEncodingException uet) {
                Logger.error(this, "Reader doesn't not recoginize Encoding type: ", uet);
            }
            try{
                roles.addAll((List<Role>) _xstream.fromXML(charStream));
            }catch(Exception e){
                Logger.error(this, "Unable to import " + _className, e);
            }
        }
        
        Collections.sort(roles);
        try{
            HibernateUtil.closeSession();
            for (Role role : roles) {
                HibernateUtil _dh = new HibernateUtil(Role.class);
                String id = HibernateUtil.getSession().getSessionFactory().getClassMetadata(Role.class).getIdentifierPropertyName();
                HibernateUtil.getSession().close();
                
                if (UtilMethods.isSet(id)) {
                    String prop = BeanUtils.getProperty(role, id);
                    
                    try {
                        if(id.equalsIgnoreCase("id")){
                            Long myId = new Long(Long.parseLong(prop));
                            HibernateUtil.saveWithPrimaryKey(role, myId);
                        }else{
                            HibernateUtil.saveWithPrimaryKey(role, prop);
                        }
                    } catch (Exception e) {
                        try {
                            HibernateUtil.saveWithPrimaryKey(role, prop);
                        } catch (DotHibernateException ex) {
                            Logger.error(this, "Unable to save role " + role.getId(), ex);
                        }
                    }
                    
                } else {
                    HibernateUtil.save(role);
                }
                HibernateUtil.getSession().flush();
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    Logger.error(this,e.getMessage(),e);
                }
            }
        } catch (Exception e) {
            Logger.error(this, "Unable to load role : " + e.getMessage(), e);
        }
        try{
            doXMLFileImport(userXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + userXML.getName() + " : " + e.getMessage(), e);
        }
        
        for (File file : usersRolesXML) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        try{
            doXMLFileImport(layoutsPortletsXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + layoutsPortletsXML.getName() + " : " + e.getMessage(), e);
        }
        try{
            doXMLFileImport(pluginPropertyXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + pluginPropertyXML.getName() + " : " + e.getMessage(), e);
        }
        try {
            doXMLFileImport( logMapperRowXML, out );
        } catch ( Exception e ) {
            Logger.error( this, "Unable to load " + logMapperRowXML.getName() + " : " + e.getMessage(), e );
        }
        try{
            doXMLFileImport(rolesLayoutsXML, out);
        } catch (Exception e) {
            Logger.error(this, "Unable to load " + rolesLayoutsXML.getName() + " : " + e.getMessage(), e);
        }
        
        try{
            /* Because of the parent check we do at db we need to import 
             * folder identifiers first but for the same reason we need to sort them first
             * by parent_path
             */
            
            final List<Identifier> folderIdents=new ArrayList<Identifier>();
            final XStream xstream = new XStream();
            
            // collecting all folder identifiers
            for(File ff : identifiersXML) {
                List<Identifier> idents=(List<Identifier>)xstream.fromXML(new FileInputStream(ff));
                for(Identifier ident : idents) {
                    if(ident.getAssetType().equals("folder"))
                        folderIdents.add(ident);
                }
            }
            
            // sorting folder identifiers by parent path in order to pass parent check
            Collections.sort(folderIdents, new Comparator<Identifier>() {
                public int compare(Identifier o1, Identifier o2) {
                    return o1.getParentPath().compareTo(o2.getParentPath());
                }
            });
            
            // saving folder identifiers
            for(Identifier ident : folderIdents) {
                Logger.info(this, "Importing folder path "+ident.getParentPath()+ident.getAssetName());
                HibernateUtil.saveWithPrimaryKey(ident, ident.getId());
            }
            HibernateUtil.flush();
            HibernateUtil.closeSession();
            
            // now we need to save all remaining identifiers (folders already added)
            for(File ff : identifiersXML) {
                try {
                    doXMLFileImport(ff, out, new ObjectFilter() {
                        public boolean includeIt(Object obj) {
                            return !((Identifier)obj).getAssetType().equals("folder");
                        }
                    });
                } catch (Exception e) {
                    Logger.error(this, "Unable to load " + ff.getName() + " : " + e.getMessage(), e);
                }
            }
            
            HibernateUtil.closeSession();
            
            // we store here defaultFileType for every folder
            // because of mutual folder <--> structure dependency 
            final Map<String,String> fileTypesInodes=new HashMap<String,String>();
            
            // now we can import folders
            for(File ff : foldersXML) {
                try {
                    doXMLFileImport(ff, out, new ObjectFilter() {
                        public boolean includeIt(Object obj) {
                            Folder f=(Folder)obj;
                            fileTypesInodes.put(f.getInode(), f.getDefaultFileType());
                            f.setDefaultFileType(null);
                            return true;
                        }
                    });
                } catch (Exception e) {
                    Logger.error(this, "Unable to load " + ff.getName() + " : " + e.getMessage(), e);
                }
            }
            
            HibernateUtil.closeSession();
            
            // we need structures before contentlets
            // but structures have references to folders and hosts identifiers
            // so, here is the place to do it
            for (File file : structuresXML) {
                try {
                    doXMLFileImport(file, out);
                } catch (Exception e) {
                    Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
                }
            }
            
            // updating file_type on folder now that structures were added
            DotConnect dc = new DotConnect();
            for(Entry<String,String> entry : fileTypesInodes.entrySet()) {
                dc.setSQL("update folder set default_file_type=? where inode=?");
                dc.addParam(entry.getValue());
                dc.addParam(entry.getKey());
                dc.loadResult();
            }
            
            HibernateUtil.closeSession();
            
            // We have all identifiers, structures and users. Ready to import contentlets! 
            for (File file : contentletsXML) {
                try{
                    doXMLFileImport(file, out);
                } catch (Exception e) {
                    Logger.error(this, "Unable to load hosts from " + file.getName() + " : " + e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            Logger.error(this, "Unable to load contentlet, structures and folders " + e.getMessage(), e);
        }
        
        for (File file : templatesXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        
        for (File file : templateContainersXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : pagesXML) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : menuLinksXML) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : filesXML) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : containersXML) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        
        for (File file : treeXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : analyticSummaryXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : analyticSummary404XMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : analyticSummaryRefererXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : analyticSummaryContentXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : analyticSummaryPagesXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : analyticSummaryVisitsXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : dashboardUserPreferencesXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        
        
        // workflow schemas need to come before permissions
        if(workflowSchemaFile != null){
        	try{
        		WorkflowImportExportUtil.getInstance().importWorkflowExport(workflowSchemaFile);
        		
        	}catch(Exception e){
        		 Logger.error(this, "Unable to import workflowSchemaFile" + e.getMessage(), e);
        	}

        }
        
        
        
        
        for (File file : permissionXMLs) {
            try{
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
			}
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        
        // finally as all assets are loaded we can import versionInfo files
        for (File file : versionInfoFilesXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : workFlowTaskXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : workFlowHistoryXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : workFlowCommentXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        for (File file : workFlowTaskFilesXML) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        
        for (File file : tagFiles) {
            try{
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "Unable to close Session : " + e.getMessage(), e);
            }
            try{
                doXMLFileImport(file, out);
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
            }
        }
        
        
        
        
        
        
        
        
        
        
        
        
        cleanUpDBFromImport();
        if(hasAssetDir && assetDir!= null && assetDir.exists())
            copyAssetDir(assetDir);
        
        
        out.println("Done Importing");
        
        deleteTempFiles();
        
        try {
            HibernateUtil.commitTransaction();
        } catch (DotHibernateException e) {
            Logger.error(this, e.getMessage(),e);
        }
        
        MaintenanceUtil.flushCache();
        
        ReindexThread.startThread(Config.getIntProperty("REINDEX_THREAD_SLEEP", 500), Config.getIntProperty("REINDEX_THREAD_INIT_DELAY", 5000));
        
        ContentletAPI conAPI = APILocator.getContentletAPI();
        Logger.info(this, "Building Initial Index");
        try {
            APILocator.getContentletIndexAPI().getRidOfOldIndex();
        } catch (DotDataException e1) {
            Logger.warn(this, "Exception trying to delete old indexes",e1);
        }
        conAPI.refreshAllContent();
        long recordsToIndex = 0;
        try {
            recordsToIndex = APILocator.getDistributedJournalAPI().recordsLeftToIndexForServer();
            Logger.info(this, "Records left to index : " + recordsToIndex);
        } catch (DotDataException e) {
            Logger.error(ImportExportUtil.class,e.getMessage() + " while trying to get the number of records left to index",e);
        }
        int counter = 0;
        
        while(recordsToIndex > 0){
            if(counter > 600){
                try {
                    Logger.info(this, "Records left to index : " + APILocator.getDistributedJournalAPI().recordsLeftToIndexForServer());
                } catch (DotDataException e) {
                    Logger.error(ImportExportUtil.class,e.getMessage() + " while trying to get the number of records left to index",e);
                }
                counter = 0;
            }
            if(counter % 100 == 0){
                try{
                    recordsToIndex = APILocator.getDistributedJournalAPI().recordsLeftToIndexForServer();
                } catch (DotDataException e) {
                    Logger.error(ImportExportUtil.class,e.getMessage() + " while trying to get the number of records left to index",e);
                }
            }
            try{
                Thread.sleep(100);
            }catch (Exception e) {
                Logger.debug(this, "Cannot sleep : ", e);
            }
            counter++;
        }
        Logger.info(this, "Finished Building Initial Index");
        ReindexThread.stopThread();
        
        CacheLocator.getCacheAdministrator().flushAll();
        MaintenanceUtil.deleteStaticFileStore();
        MaintenanceUtil.deleteMenuCache();
        
    }
    
    private void copyAssetDir(File fromAssetDir){
        File ad;
        if(!UtilMethods.isSet(assetRealPath)){
            ad = new File(Config.CONTEXT.getRealPath(assetPath));
        }else{
            ad = new File(assetRealPath);
        }
        ad.mkdirs();
        String[] fileNames = fromAssetDir.list();
        for (int i = 0; i < fileNames.length; i++) {
            File f = new File(fromAssetDir.getPath() + File.separator + fileNames[i]);
            if(f.getName().equals(".svn")){
                continue;
            }
            if(f.getName().equals("license.dat")){
                continue;
            }
            if(f.isDirectory()){
                FileUtil.copyDirectory(f.getPath(), ad.getPath() + File.separator + f.getName());
            }else{
                FileUtil.copyFile(f.getPath(), ad.getPath() + File.separator + f.getName());
            }
        }
    }
    /**
     * Does what it says - deletes all files from the backupTempFilePath
     * @author Will
     */
    private void deleteTempFiles() {
        File f = new File(backupTempFilePath);
        String[] _tempFiles = f.list();
        if(_tempFiles != null){
            for (int i = 0; i < _tempFiles.length; i++) {
                f = new File(backupTempFilePath + "/" + _tempFiles[i]);
                f.delete();
            }
        }
    }
    
    /**
     * This is not completed should delete all the dotcms data from an install
     * @author Will
     */
    private void deleteDotCMS() {
        try {
            /* get a list of all our tables */
            //			Set<String> _tablesToDelete = new HashSet<String>();
            ArrayList<String> _tablesToDelete = new ArrayList<String>();
            Map map =null;
            
            try {
                map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();
            } catch (DotHibernateException e) {
                Logger.error(this,e.getMessage(),e);
            }
            
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                AbstractEntityPersister cmd = (AbstractEntityPersister) pairs.getValue();
                String tableName = cmd.getTableName();
                //				Class x = (Class) pairs.getKey();
                
                if(!tableName.equalsIgnoreCase("inode")
                        && !tableName.equalsIgnoreCase("plugin")
                        && !tableName.equalsIgnoreCase("plugin_property")
                        && !tableName.equalsIgnoreCase("cms_layouts_portlets")
                        && !tableName.equalsIgnoreCase("layouts_cms_roles")
                        && !tableName.equalsIgnoreCase("users_cms_roles")
                        && !tableName.equalsIgnoreCase("cms_role")
                        && !tableName.equalsIgnoreCase("cms_layout")
                        && !tableName.equalsIgnoreCase("analytic_summary")
                        && !tableName.equalsIgnoreCase("analytic_summary_content")
                        && !tableName.equalsIgnoreCase("analytic_summary_period")
                        && !tableName.equalsIgnoreCase("structure")
                        && !tableName.equalsIgnoreCase("folder")
                        && !tableName.equalsIgnoreCase("identifier")
                        && !tableName.equalsIgnoreCase("inode")){
                    _tablesToDelete.add(tableName);
                }
            }
            //these tables should be deleted in this order to avoid conflicts with foreign keys
            _tablesToDelete.add("cms_layouts_portlets");
            _tablesToDelete.add("layouts_cms_roles");
            _tablesToDelete.add("users_cms_roles");
            _tablesToDelete.add("cms_role");
            _tablesToDelete.add("cms_layout");
            _tablesToDelete.add("analytic_summary_content");
            _tablesToDelete.add("analytic_summary");
            _tablesToDelete.add("analytic_summary_period");
            _tablesToDelete.add("structure");
            _tablesToDelete.add("folder");
            _tablesToDelete.add("identifier");
            _tablesToDelete.add("inode");;
            _tablesToDelete.add("user_");
            _tablesToDelete.add("company");
            _tablesToDelete.add("counter");
            _tablesToDelete.add("image");
            _tablesToDelete.add("portlet");
            _tablesToDelete.add("portletpreferences");
            _tablesToDelete.add("address");
            _tablesToDelete.add("address");
            _tablesToDelete.add("plugin_property");
            _tablesToDelete.add("plugin");
            _tablesToDelete.add("pollschoice");
            _tablesToDelete.add("pollsdisplay");
            _tablesToDelete.add("pollsquestion");
            _tablesToDelete.add("pollsvote");
            
            
            DotConnect _dc = null;
            for (String table : _tablesToDelete) {
                Logger.info(this, "About to delete all records from " + table);
                _dc = new DotConnect();
                _dc.setSQL("delete from " + table);
                _dc.getResult();
                Logger.info(this, "Deleted all records from " + table);
            }
        } catch (HibernateException e) {
            Logger.error(this,e.getMessage(),e);
        }
        File ad;
        if(!UtilMethods.isSet(assetRealPath)){
            ad = new File(Config.CONTEXT.getRealPath(assetPath));
        }else{
            ad = new File(assetRealPath);
        }
        ad.mkdirs();
        String[] fl = ad.list();
        for (String fileName : fl) {
            File f = new File(ad.getPath() + File.separator + fileName);
            if(f.isDirectory()){
                FileUtil.deltree(f);
            }else{
                f.delete();
            }
        }
    }
    
    interface ObjectFilter {
        boolean includeIt(Object obj);
    }
    
    /**
     * This method takes an xml file and will try to import it via XStream and
     * Hibernate
     *
     * @param f
     *            File to be parsed and imported
     * @param out
     *            Printwriter to write responses to Reponse Printwriter so this
     *            method can write to screen.
     *
     * @author Will
     */
    private void doXMLFileImport(File f, PrintWriter out)throws DotDataException, HibernateException {
        doXMLFileImport(f, out, null);
    }
    
    private void doXMLFileImport(File f, PrintWriter out, ObjectFilter filter)throws DotDataException, HibernateException {
        if( f ==null){
            return;
        }
        
        BufferedInputStream _bin = null;
        Reader charStream = null;
        try {
            XStream _xstream = null;
            String _className = null;
            Class _importClass = null;
            HibernateUtil _dh = null;
            
            boolean counter = false;
            boolean image = false;
            boolean portlet = false;
            boolean logsMappers = false;
            boolean portletpreferences = false;
            boolean address = false;
            boolean pollschoice = false;
            boolean pollsdisplay = false;
            boolean pollsquestion = false;
            boolean pollsvote = false;
            
            /* if we have a multipart import file */
            Pattern p = Pattern.compile("_[0-9]{8}");
            Matcher m  = p.matcher(f.getName());
            if(m.find()){
                _className = f.getName().substring(0, f.getName().lastIndexOf("_"));
            }
            else{
                _className = f.getName().substring(0, f.getName().lastIndexOf("."));
            }
            
            if(_className.equals("Counter")){
                counter = true;
            }else if(_className.equals("Image")){
                image = true;
            }else if(_className.equals("Portlet")){
                portlet = true;
            }else if(_className.equals("LogsMappers")){
                logsMappers = true;
            }else if(_className.equals("Portletpreferences")){
                portletpreferences = true;
            }else if(_className.equals("Pollschoice")){
                pollschoice = true;
            }else if(_className.equals("Address")){
                address = true;
            }else if(_className.equals("Pollsdisplay")){
                pollsdisplay = true;
            }else if(_className.equals("Pollsquestion")){
                pollsquestion = true;
            }else if(_className.equals("Pollsvote")){
                pollsvote = true;
            }else{
                try{
                    _importClass = Class.forName(_className);
                }catch (Exception e) {
                    Logger.error(this, "Class not found " + _className, e);
                    return;
                }
            }
            _xstream = new XStream();
            out.println("Importing:\t" + _className);
            Logger.info(this, "Importing:\t" + _className);
            
            try{
                charStream = new InputStreamReader(new FileInputStream(f), "UTF-8");
            }catch (UnsupportedEncodingException uet) {
                Logger.error(this, "Reader doesn't not recoginize Encoding type: ", uet);
            }
            List l = new ArrayList();
            try{
                List all = (List) _xstream.fromXML(charStream);
                if(filter!=null) {
                    for(Object obj : all)
                        if(filter.includeIt(obj))
                            l.add(obj);
                }
                else {
                    l = all;
                }   
            }catch(Exception e){
                Logger.error(this, "Unable to import " + _className, e);
            }
            out.println("Found :\t" + l.size() + " " + _className + "(s)");
            Logger.info(this, "Found :\t" + l.size() + " " + _className + "(s)");
            if(address){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into address values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                    dc.addParam(dcResults.get("addressid"));
                    dc.addParam(dcResults.get("companyid"));
                    dc.addParam(dcResults.get("userid"));
                    dc.addParam(dcResults.get("username"));
                    dc.addParam(dcResults.get("createDate"));
                    dc.addParam(dcResults.get("ModifiedDate"));
                    dc.addParam(dcResults.get("classname"));
                    dc.addParam(dcResults.get("classpk"));
                    dc.addParam(dcResults.get("description"));
                    dc.addParam(dcResults.get("street1"));
                    dc.addParam(dcResults.get("street2"));
                    dc.addParam(dcResults.get("city"));
                    dc.addParam(dcResults.get("state"));
                    dc.addParam(dcResults.get("zip"));
                    dc.addParam(dcResults.get("country"));
                    dc.addParam(dcResults.get("phone"));
                    dc.addParam(dcResults.get("fax"));
                    dc.addParam(dcResults.get("cell"));
                    dc.addParam(UtilMethods.isSet(dcResults.get("priority")) ? Integer.parseInt(dcResults.get("priority")) : null);
                    dc.getResults();
                }
            }else if(counter){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into counter values (?,?)");
                    dc.addParam(dcResults.get("name"));
                    dc.addParam(Integer.valueOf(dcResults.get("currentid")));
                    dc.getResults();
                }
            }else if(pollschoice){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into pollschoice values (?,?,?)");
                    dc.addParam(dcResults.get("choiceid"));
                    dc.addParam(dcResults.get("questionid"));
                    dc.addParam(dcResults.get("description"));
                    dc.getResults();
                }
            }else if(pollsdisplay){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into pollsdisplay values (?,?,?,?)");
                    dc.addParam(dcResults.get("layoutid"));
                    dc.addParam(dcResults.get("userid"));
                    dc.addParam(dcResults.get("portletid"));
                    dc.addParam(dcResults.get("questionid"));
                    dc.getResults();
                }
            }else if(pollsquestion){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into pollsquestion values (?,?,?,?,?,?,?,?,?,?,?,?)");
                    dc.addParam(dcResults.get("questionid"));
                    dc.addParam(dcResults.get("portletid"));
                    if(UtilMethods.isSet(dcResults.get("groupid"))){
                        dc.addParam(dcResults.get("groupid"));
                    }else{
                        dc.addParam(-1);
                    }
                    dc.addParam(dcResults.get("companyid"));
                    dc.addParam(dcResults.get("userid"));
                    dc.addParam(dcResults.get("username"));
                    if(UtilMethods.isSet(dcResults.get("createdate"))){
                        dc.addParam(java.sql.Timestamp.valueOf(dcResults.get("createdate")));
                    }else{
                        dc.addParam(new java.sql.Timestamp(0));
                    }
                    if(UtilMethods.isSet(dcResults.get("modifieddate"))){
                        dc.addParam(java.sql.Timestamp.valueOf(dcResults.get("modifieddate")));
                    }else{
                        dc.addParam(new java.sql.Timestamp(0));
                    }
                    dc.addParam(dcResults.get("title"));
                    dc.addParam(dcResults.get("description"));
                    if(UtilMethods.isSet(dcResults.get("expirationdate"))){
                        dc.addParam(java.sql.Timestamp.valueOf(dcResults.get("expirationdate")));
                    }else{
                        dc.addParam(new java.sql.Timestamp(0));
                    }
                    if(UtilMethods.isSet(dcResults.get("lastvotedate"))){
                        dc.addParam(java.sql.Timestamp.valueOf(dcResults.get("lastvotedate")));
                    }else{
                        dc.addParam(new java.sql.Timestamp(0));
                    }
                    dc.getResults();
                }
            }else if(pollsvote){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into pollsvote values (?,?,?,?)");
                    dc.addParam(dcResults.get("questionid"));
                    dc.addParam(dcResults.get("userid"));
                    dc.addParam(dcResults.get("choiceid"));
                    if(UtilMethods.isSet(dcResults.get("lastvotedate"))){
                        dc.addParam(java.sql.Timestamp.valueOf(dcResults.get("lastvotedate")));
                    }else{
                        dc.addParam(new java.sql.Timestamp(0));
                    }
                    dc.getResults();
                }
            }else if(image){
                /*
                 * The changes in this part were made for Oracle databases. Oracle has problems when
                 * getString() method is called on a LONG field on an Oracle database. Because of this,
                 * the object is loaded from liferay and DotConnect is not used
                 * http://jira.dotmarketing.net/browse/DOTCMS-1911
                 */
                for (int j = 0; j < l.size(); j++) {
                    Image im = (Image)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into image values (?,?)");
                    if(!UtilMethods.isSet(im.getImageId()) && com.dotmarketing.db.DbConnectionFactory.getDBType().equals(com.dotmarketing.db.DbConnectionFactory.ORACLE)){
                        continue;
                    }
                    dc.addParam(im.getImageId());
                    dc.addParam(im.getText());
                    dc.getResults();
                }
            }else if(portlet){
                for (int j = 0; j < l.size(); j++) {
                    HashMap<String, String> dcResults = (HashMap<String,String>)l.get(j);
                    DotConnect dc = new DotConnect();
                    StringBuffer sb = new StringBuffer("insert into portlet values (?,?,?,?,");
                    if(dcResults.get("narrow").equalsIgnoreCase("f") || dcResults.get("narrow").equalsIgnoreCase("false") || dcResults.get("narrow").equalsIgnoreCase("0") || dcResults.get("narrow").equals(DbConnectionFactory.getDBFalse()))
                        sb.append(DbConnectionFactory.getDBFalse() + ",?,");
                    else
                        sb.append(DbConnectionFactory.getDBTrue() + ",?,");
                    if(dcResults.get("active_").equalsIgnoreCase("f") || dcResults.get("active_").equalsIgnoreCase("false") || dcResults.get("active_").equalsIgnoreCase("0") || dcResults.get("active_").equals(DbConnectionFactory.getDBFalse()))
                        sb.append(DbConnectionFactory.getDBFalse() + ")");
                    else
                        sb.append(DbConnectionFactory.getDBTrue() + ")");
                    dc.setSQL(sb.toString());
                    dc.addParam(dcResults.get("portletid"));
                    dc.addParam(dcResults.get("groupid"));
                    dc.addParam(dcResults.get("companyid"));
                    dc.addParam(dcResults.get("defaultpreferences"));
                    dc.addParam(dcResults.get("roles"));
                    dc.getResults();
                }
            }else if(logsMappers){
                for ( int j = 0; j < l.size(); j++ ) {
                    LogMapperRow logMapperRow = ( LogMapperRow ) l.get( j );
                    DotConnect dc = new DotConnect();
                    dc.setSQL( "insert into log_mapper values (?,?,?)" );
                    dc.addParam( logMapperRow.getEnabled() ? 1 : 0 );
                    dc.addParam( logMapperRow.getLog_name() );
                    dc.addParam( logMapperRow.getDescription() );
                    dc.getResults();
                }
            }else if(portletpreferences){
                /*
                 * The changes in this part were made for Oracle databases. Oracle has problems when
                 * getString() method is called on a LONG field on an Oracle database. Because of this,
                 * the object is loaded from liferay and DotConnect is not used
                 * http://jira.dotmarketing.net/browse/DOTCMS-1911
                 */
                for (int j = 0; j < l.size(); j++) {
                    PortletPreferences portletPreferences = (PortletPreferences)l.get(j);
                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into portletpreferences values (?,?,?,?)");
                    dc.addParam(portletPreferences.getPortletId());
                    dc.addParam(portletPreferences.getUserId());
                    dc.addParam(portletPreferences.getLayoutId());
                    dc.addParam(portletPreferences.getPreferences());
                    dc.getResults();
                }
            }else if (_importClass.equals(User.class)) {
                for (int j = 0; j < l.size(); j++) {
                    User u = (User)l.get(j);
                    u.setModified(true);
                    if(!u.isDefaultUser() && !u.getUserId().equals("system")){
                        try{
                            User u1 = APILocator.getUserAPI().createUser(u.getUserId(), u.getEmailAddress());
                            u.setUserId(u1.getUserId());
                            u.setEmailAddress(u.getEmailAddress());
                        }catch (DuplicateUserException e) {
                            Logger.info(this, "user already exists going to update");
                            try{
                                u = APILocator.getUserAPI().loadUserById(u.getUserId(), APILocator.getUserAPI().getSystemUser(),false);
                            }catch (Exception e1) {
                                Logger.info(this, "couldn't find user by ID going to lookup by email address");
                                u = APILocator.getUserAPI().loadByUserByEmail(u.getEmailAddress(), APILocator.getUserAPI().getSystemUser(),false);
                            }
                        }
                        APILocator.getUserAPI().save(u,APILocator.getUserAPI().getSystemUser(),false);
                    }else{
                        Logger.info(this, "");
                    }
                }
            } else if (_importClass.equals(Company.class)) {
                for (int j = 0; j < l.size(); j++) {
                    Company c = (Company)l.get(j);
                    try {
                        c.setModified(true);
                        CompanyManagerUtil.updateCompany(c);
                    } catch (SystemException e) {
                        throw new DotDataException("Unable to load company",e);
                    }
                }
            }else {
                _dh = new HibernateUtil(_importClass);
                String id = HibernateUtil.getSession().getSessionFactory().getClassMetadata(_importClass).getIdentifierPropertyName();
                HibernateUtil.getSession().close();
                boolean identityOn = false;
                String cName = _className.substring(_className.lastIndexOf(".") + 1);
                
                
                
                String tableName = "";
                if(classesWithIdentity.contains(cName) && dbType.equals(DbConnectionFactory.MSSQL) && !cName.equalsIgnoreCase("inode")){
                    tableName = tableNames.get(cName);
                    turnIdentityOnMSSQL(tableName);
                    identityOn = true;
                }/*else if(dbType.equals(DbConnectionFactory.MSSQL)){
					DotConnect dc = new DotConnect();
					dc.executeStatement("set IDENTITY_INSERT inode on;");
				}*/
                for (int j = 0; j < l.size(); j++) {
                    Object obj = l.get(j);
                    if(l.get(j) instanceof com.dotmarketing.portlets.contentlet.business.Contentlet && dbType.equals(DbConnectionFactory.MSSQL)){
                        com.dotmarketing.portlets.contentlet.business.Contentlet contentlet = (com.dotmarketing.portlets.contentlet.business.Contentlet)l.get(j);
                        changeDateForSQLServer(contentlet, out);
                    }
                    
                    if (UtilMethods.isSet(id)) {
                        String prop = BeanUtils.getProperty(obj, id);
                        
                        try {
                            HibernateUtil.startTransaction();
                            if(id.substring(id.length()-2,id.length()).equalsIgnoreCase("id")){
                                if(obj instanceof Identifier){
                                    HibernateUtil.saveWithPrimaryKey(obj, prop);
                                }else{
                                    Long myId = new Long(Long.parseLong(prop));
                                    HibernateUtil.saveWithPrimaryKey(obj, myId);
                                }
                                HibernateUtil.commitTransaction();
                            }else{
                                HibernateUtil.saveWithPrimaryKey(obj, prop);
                                HibernateUtil.commitTransaction();
                            }
                        } catch (Exception e) {
                            try{
                                HibernateUtil.saveWithPrimaryKey(obj, prop);
                                HibernateUtil.commitTransaction();
                            }catch (Exception ex) {
                                Logger.debug(this, "Usually not a problem can be that duplicate data or many times a row of data that is created by the system and is trying to be imported again : " + ex.getMessage(), ex);
                                Logger.warn(this, "Usually not a problem can be that duplicate data or many times a row of data that is created by the system and is trying to be imported again : " + ex.getMessage());
                                Logger.info(this, "Problematic object: "+obj+" prop:"+prop);
                                Logger.info(this, _xstream.toXML(obj));
                                
                                try{
                                    HibernateUtil.rollbackTransaction();
                                    HibernateUtil.closeSession();
                                }catch (Exception e1) {}
                                continue;
                            }
                        }
                        
                    } else {
                        if(obj instanceof Tree){
                            Tree t = (Tree) obj;
                            DotConnect dc = new DotConnect();
                            List<String> inodeList = new ArrayList<String>();
                            dc.setSQL("select inode from inode where inode = ? or inode = ?");
                            dc.addParam(t.getParent());
                            dc.addParam(t.getChild());
                            inodeList = dc.getResults();
                            dc.setSQL("select id from identifier where id = ? or id = ?");
                            dc.addParam(t.getParent());
                            dc.addParam(t.getChild());
                            inodeList.addAll(dc.getResults());
                            if(inodeList.size() > 1){
                                HibernateUtil.save(obj);
                            }
                            else{
                                Logger.warn(this.getClass(), "Can't import tree- no matching inodes: {parent=" + t.getParent() + ", child=" + t.getChild() +"}");
                                
                            }
                        }
                        else{
                            try {
                                HibernateUtil.save(obj);
                            } catch (DotHibernateException e) {
                                Logger.error(this,e.getMessage(),e);
                            }
                        }
                    }
                    HibernateUtil.getSession().flush();
                    HibernateUtil.closeSession();
                    try {
                        
                        
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        Logger.error(this,e.getMessage(),e);
                    }
                }
                if(identityOn){
                    turnIdentityOffMSSQL(tableName);
                }/*else if(dbType.equals(DbConnectionFactory.MSSQL)){
					turnIdentityOffMSSQL("inode");
				}*/
            }
        } catch (FileNotFoundException e) {
            Logger.error(this,e.getMessage(),e);
        } catch (IllegalAccessException e) {
            Logger.error(this,e.getMessage(),e);
        } catch (InvocationTargetException e) {
            Logger.error(this,e.getMessage(),e);
        } catch (NoSuchMethodException e) {
            Logger.error(this,e.getMessage(),e);
        } catch (SQLException e) {
            Logger.error(this,e.getMessage(),e);
        } catch (DotSecurityException e) {
            Logger.error(this,e.getMessage(),e);
        } finally {
            
            try {
                
                if (charStream != null) {
                    charStream.close();
                }
            } catch (IOException e) {
                Logger.error(this,e.getMessage(),e);
            }
        }
    }
    
    /**
     * Simple FileNameFilter for XML files
     *
     * @author will
     *
     */
    private class XMLFileNameFilter implements FilenameFilter {
        
        public boolean accept(File f, String s) {
            if (s.toLowerCase().endsWith(".xml")) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    private void turnIdentityOnMSSQL(String tableName) throws SQLException{
        DotConnect dc = new DotConnect();
        dc.executeStatement("set identity_insert " + tableName + " on");
    }
    
    private void turnIdentityOffMSSQL(String tableName) throws SQLException{
        DotConnect dc = new DotConnect();
        dc.executeStatement("set identity_insert " + tableName + " off");
    }
    
    private void cleanUpDBFromImport(){
        String dbType = DbConnectionFactory.getDBType();
        DotConnect dc = new DotConnect();
        try {
            if(dbType.equals(DbConnectionFactory.MSSQL)){
            }else if(dbType.equals(DbConnectionFactory.ORACLE)){
                for (String clazz : classesWithIdentity) {
                    String tableName = tableNames.get(clazz);
                    dc.setSQL("drop sequence " + sequences.get(tableName));
                    dc.getResults();
                    dc.setSQL("select max(" + tableIDColumns.get(tableName) + ") as maxID from " + tableName);
                    ArrayList<HashMap<String, String>> results = dc.getResults();
                    int max = dc.getResults().size() == 0 ? 0 : Parameter.getInt(dc.getString("maxID"),1);
                    dc.setSQL("CREATE SEQUENCE " + sequences.get(tableName) + " MINVALUE 1 START WITH " + (max + 100) + " INCREMENT BY 1");
                    dc.getResults();
                }
            }else if(dbType.equals(DbConnectionFactory.POSTGRESQL)){
                for (String clazz : classesWithIdentity) {
                    String tableName = tableNames.get(clazz);
                    dc.setSQL("select max(" + tableIDColumns.get(tableName) + ") as maxID from " + tableName);
                    ArrayList<HashMap<String, String>> results = dc.getResults();
                    int max = dc.getResults().size() == 0 ? 0 : Parameter.getInt(dc.getString("maxID"),1);
                    dc.setSQL("alter sequence " + sequences.get(tableName) + " restart with " + (max + 1));
                    dc.getResults();
                }
            }
        } catch (DotDataException e) {
            Logger.error(this,"cleanUpDBFromImport failed:"+ e, e);
        }
    }
    
    public String getBackupTempFilePath() {
        return backupTempFilePath;
    }
    
    public void setBackupTempFilePath(String backupTempFilePath) {
        this.backupTempFilePath = backupTempFilePath;
    }
    
    
    /**
     *
     * @param zipFile
     * @return
     */
    public boolean validateZipFile(File zipFile){
        
        String tempdir = getBackupTempFilePath();
        try {
            deleteTempFiles();
            
            File ftempDir = new File(tempdir);
            ftempDir.mkdirs();
            File tempZip = new File(tempdir + File.separator + zipFile.getName());
            tempZip.createNewFile();
            FileChannel ic = new FileInputStream(zipFile).getChannel();
            FileChannel oc = new FileOutputStream(tempZip).getChannel();
            
            // to handle huge zipfiles
            ic.transferTo(0, ic.size(), oc);
            
            ic.close();
            oc.close();
            
            
            /*
             * Unzip zipped backups
             */
            if (zipFile != null && zipFile.getName().toLowerCase().endsWith(".zip")) {
                ZipFile z = new ZipFile(zipFile);
                ZipUtil.extract(z, new File(backupTempFilePath));
            }
            return true;
            
        } catch (Exception e) {
            Logger.error(this,"Error with file",e);
            return false;
        }
    }
    
    private boolean validateDate(Date date){
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(1753, 01, 01);
        boolean validated = true;
        if(date != null && date.before(calendar.getTime()) ){
            validated = false;
        }
        return validated;
        
    }
    
    private void changeDateForSQLServer(com.dotmarketing.portlets.contentlet.business.Contentlet contentlet, PrintWriter out){
        if(!validateDate(contentlet.getDate1())){
            contentlet.setDate1(new Date());
            out.println("Unsupported data in SQL Server, so changed date to current date for contentlet with inode ");
        }
        if(!validateDate(contentlet.getDate2())){
            contentlet.setDate2(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate3())){
            contentlet.setDate3(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate4())){
            contentlet.setDate4(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate5())){
            contentlet.setDate5(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate6())){
            contentlet.setDate6(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate7())){
            contentlet.setDate7(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate8())){
            contentlet.setDate8(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate9())){
            contentlet.setDate9(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate10())){
            contentlet.setDate10(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate11())){
            contentlet.setDate11(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate12())){
            contentlet.setDate12(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate13())){
            contentlet.setDate13(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate14())){
            contentlet.setDate14(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate15())){
            contentlet.setDate15(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate16())){
            contentlet.setDate16(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate17())){
            contentlet.setDate17(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate18())){
            contentlet.setDate18(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate19())){
            contentlet.setDate19(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate20())){
            contentlet.setDate20(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate21())){
            contentlet.setDate21(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate22())){
            contentlet.setDate22(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate23())){
            contentlet.setDate23(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate24())){
            contentlet.setDate24(new Date());
            out.println("Date changed to current date");
        }
        if(!validateDate(contentlet.getDate25())){
            contentlet.setDate25(new Date());
            out.println("Date changed to current date");
        }
    }
}
