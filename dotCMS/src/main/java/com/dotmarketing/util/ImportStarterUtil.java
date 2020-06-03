package com.dotmarketing.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import org.apache.commons.beanutils.BeanUtils;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.net.sf.hibernate.persister.AbstractEntityPersister;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutsRoles;
import com.dotmarketing.business.PortletsLayouts;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UsersRoles;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;


/**
 * This utility is part of the {@link Task00004LoadStarter} task, which fills the empty dotCMS
 * tables on a fresh install with information regarding the Demo Site that the application ships
 * with. This allows users to be able to log into the dotCMS back-end and interact with the system
 * before adding their own custom content.
 *
 * @author Jason Tesser
 * @version 1.6
 *
 */
public class ImportStarterUtil {

    /**
     * The path where tmp files are stored. This gets wiped alot
     */
    private String backupTempFilePath = ConfigUtils.getBackupPath() + File.separator + "temp";
    private ArrayList<String> classesWithIdentity = new ArrayList<String>();
    private Map<String, String> sequences;
    private Map<String, String> tableIDColumns;
    private Map<String, String> tableNames;
    private final String assetPath;


    private final List<File> tempFiles;

    private static final String SYSTEM_FOLDER_PATH = FolderAPI.SYSTEM_FOLDER_PARENT_PATH;

    private final File starterZip;


    public ImportStarterUtil(File starterZipFile) {
        this.starterZip = starterZipFile;

        this.assetPath = ConfigUtils.getAbsoluteAssetsRootPath();
        if (!this.validateZipFile()) {
            throw new DotRuntimeException("starter data invalid");
        }
        final File backTemporalFile = new File(getBackupTempFilePath());
        
        tempFiles = Arrays.asList(backTemporalFile.listFiles()).stream().sorted().collect(Collectors.toList());
        tempFiles.removeIf(f -> f.getName().endsWith("Counter.xml"));
        tempFiles.removeIf(f -> f.getName().contains(".Dashboard"));
        tempFiles.removeIf(f -> f.getName().contains(".FixAudit_"));
        tempFiles.removeIf(f -> f.getName().contains(".UserProxy_"));
        tempFiles.removeIf(f -> f.getName().contains(".PluginProperty_"));
        tempFiles.removeIf(f -> f.getName().contains(".Plugin_"));



        classesWithIdentity.add("Permission");
        classesWithIdentity.add("UsersToDelete");
        tableNames = new HashMap<String, String>();
        tableNames.put("Permission", "permission");
        tableNames.put("UsersToDelete", "users_to_delete");

        if (DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle()) {
            sequences = new HashMap<String, String>();
            sequences.put("permission", "permission_seq");
            sequences.put("users_to_delete", "user_to_delete_seq");
            tableIDColumns = new HashMap<String, String>();
            tableIDColumns.put("permission", "id");
            tableIDColumns.put("users_to_delete", "id");

        }
    }

    /**
     * Takes a ZIP file from the /temp directory to restore dotCMS data. Currently, it will blow away
     * all current data. This method cannot currently be run in a transaction. For performance reasons
     * with DB drivers and connections it closes the session every so often.
     *
     * @param out - A print writer for output.
     * @throws IOException
     * @throws Exception
     */
    @WrapInTransaction
    public void doImport() throws Exception {



        Logger.info(this, "Found " + tempFiles.size() + " files to import");


        deleteDotCMS();
        

        for (File file : endsWith("Company.xml")) {
            doXMLFileImport(file);
        }


        List<Role> roles = new ArrayList<Role>();

        for (File file : startsWith("com.dotmarketing.business.Role_")) {
            roles.addAll((List<Role>) BundlerUtil.xmlToObject(file));
        }

        Collections.sort(roles);


        for (Role role : roles) {
            HibernateUtil _dh = new HibernateUtil(Role.class);
            String id = HibernateUtil.getSession().getSessionFactory().getClassMetadata(Role.class).getIdentifierPropertyName();

            if(UtilMethods.isSet(role.getRoleKey())) {
               List<Map<String,Object>>  matches= new DotConnect().setSQL("select * from cms_role where role_key =?").addParam(role.getRoleKey()).loadObjectResults();
               Logger.info(this.getClass(), "roleKey:" +role.getRoleKey() + " = " + matches.size() );
            }

            _dh.saveWithPrimaryKey(role, role.getId());
            HibernateUtil.getSession().flush();
        }

        for (File file : endsWith(User.class.getCanonicalName() + ".xml")) {
            doXMLFileImport(file);
        }

        for (File file : contains(UsersRoles.class.getCanonicalName() + "_")) {
            doXMLFileImport(file);
        }
        
        for (File file : contains(Layout.class.getCanonicalName() + "_")) {
            doXMLFileImport(file);
        }

        
        
        for (File file : contains(PortletsLayouts.class.getCanonicalName())) {
            doXMLFileImport(file);
        }

        
        for (File file : endsWith("LogsMappers.xml")) {
            doXMLFileImport(file);
        }
        
        for (File file : contains(LayoutsRoles.class.getCanonicalName())) {
            doXMLFileImport(file);
        }
        
        
        
        for (File file : contains(Language.class.getCanonicalName() + "_")) {
            doXMLFileImport(file);
        }
        /*
         * Because of the parent check we do at db we need to import folder identifiers first but for the
         * same reason we need to sort them first by parent_path
         */
        final List<Identifier> folderIdents = new ArrayList<Identifier>();
        final List<Identifier> otherIdents = new ArrayList<Identifier>();

        List<File> identifiers = contains(Identifier.class.getCanonicalName());
        // collecting all folder identifiers
        for (File ff : identifiers) {
            List<Identifier> idents = (List<Identifier>) BundlerUtil.xmlToObject(ff);
            for (Identifier ident : idents) {
                if (ident.getAssetType().equals("folder")) {
                    folderIdents.add(ident);
                }else {
                    otherIdents.add(ident);
                }
            }
        }

        // sorting folder identifiers by parent path in order to pass parent check
        Collections.sort(folderIdents, new Comparator<Identifier>() {
            public int compare(Identifier o1, Identifier o2) {
                return o1.getParentPath().compareTo(o2.getParentPath());
            }
        });

        // saving folder identifiers
        for (Identifier ident : folderIdents) {
            if (!SYSTEM_FOLDER_PATH.equals(ident.getParentPath())) {
                ident.setParentPath(ident.getParentPath());
                ident.setAssetName(ident.getAssetName());
            }
            Logger.info(this, "Importing folder path " + ident.getParentPath() + ident.getAssetName());
            APILocator.getIdentifierAPI().save(ident);
        }

        // now we need to save all remaining identifiers (folders already added)
        for (final Identifier ident : otherIdents) {
            APILocator.getIdentifierAPI().save(ident);
        }



        // we store here defaultFileType for every folder
        // because of mutual folder <--> structure dependency
        final Map<String, String> fileTypesInodes = new HashMap<String, String>();

        // now we can import folders
        for (File ff : contains(Folder.class.getCanonicalName())) {
            try {
                doXMLFileImport(ff, new ObjectFilter() {
                    public boolean includeIt(Object obj) {
                        Folder f = (Folder) obj;
                        fileTypesInodes.put(f.getInode(), f.getDefaultFileType());
                        f.setDefaultFileType(null);
                        return true;
                    }
                });
            } catch (Exception e) {
                Logger.error(this, "Unable to load " + ff.getName() + " : " + e.getMessage(), e);
            }
        }


        // we need content types before contentlets
        // but content types have references to folders and hosts identifiers
        // so, here is the place to do it
        for (File file : endsWith(ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION)) {
            new ContentTypeImportExportUtil().importContentTypes(file);
        }

        // updating file_type on folder now that structures were added
        DotConnect dc = new DotConnect();
        for (Entry<String, String> entry : fileTypesInodes.entrySet()) {
            dc.setSQL("update folder set default_file_type=? where inode=?");
            dc.addParam(entry.getValue());
            dc.addParam(entry.getKey());
            dc.loadResult();
        }


        for (File file : contains(Relationship.class.getCanonicalName() + "_")) {
            doXMLFileImport(file);
        }

        // We have all identifiers, structures and users. Ready to import contentlets!
        for (File file : contains(Contentlet.class.getCanonicalName() + "_")) {
            doXMLFileImport(file);
        }


        for (File file : contains(Template.class.getCanonicalName() + "_")) {
            doXMLFileImport(file);
        }

        for (File file : contains("com.dotmarketing.beans.TemplateContainers_")) {
            doXMLFileImport(file);
        }
        for (File file : contains(Link.class.getCanonicalName() + "_")) {

            doXMLFileImport(file);
        }
        for (File file : contains(Container.class.getCanonicalName()+ "_")) {
            doXMLFileImport(file);
        }
        
        for (File file : contains("com.dotmarketing.beans.ContainerStructure_")) {
            doXMLFileImport(file);
        }
        
        for (File file : contains("com.dotmarketing.beans.Tree_")) {
            doXMLFileImport(file);
        }

        // workflow schemas need to come before permissions
        for (File file : contains("WorkflowSchemeImportExportObject.json")) {
            WorkflowImportExportUtil.getInstance().importWorkflowExport(file);
        }

        for (File file : contains("com.dotmarketing.beans.Permission_")) {
            doXMLFileImport(file);
        }

        // finally as all assets are loaded we can import versionInfo files
        for (File file : contains("VersionInfo_")) {

            doXMLFileImport(file);

        }
        // We install rules after Version info.
        for (File file : contains("RuleImportExportObject.json")) {
            RulesImportExportUtil.getInstance().importRules(file);
        }

        for (File file : contains("com.dotmarketing.portlets.workflows.model.WorkflowTask_")) {
            doXMLFileImport(file);
        }
        for (File file : contains("com.dotmarketing.portlets.workflows.model.WorkflowHistory_")) {
            doXMLFileImport(file);
        }
        for (File file : contains("com.dotmarketing.portlets.workflows.model.WorkflowComment_")) {
            doXMLFileImport(file);
        }
        for (File file : contains("com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles_")) {
            doXMLFileImport(file);
        }

        for (File file : contains("com.dotmarketing.tag.model.Tag_")) {
            doXMLFileImport(file);
        }
        // Image, Portlet, Multitree, category
        for (File file : endsWith(".xml")) {
            doXMLFileImport(file);
        }


        cleanUpDBFromImport();
        Optional<File> assetDir = tempFiles.stream().filter(f -> "asset".equals(f.getName()) && f.isDirectory()).findAny();
        if (assetDir.isPresent()) {
            copyAssetDir(assetDir.get());
        }
        Logger.info(ImportStarterUtil.class, "Done Importing");
        deleteTempFiles();



    }

    /**
     *
     * @param fromAssetDir
     * @throws IOException
     */
    private void copyAssetDir(File fromAssetDir) throws IOException {
        File ad = new File(assetPath);

        ad.mkdirs();
        String[] fileNames = fromAssetDir.list();
        for (int i = 0; i < fileNames.length; i++) {
            File f = new File(fromAssetDir.getPath() + File.separator + fileNames[i]);
            if (f.getName().equals(".svn")) {
                continue;
            }
            if (f.getName().equals("license.dat")) {
                continue;
            }
            if (f.isDirectory()) {
                FileUtil.copyDirectory(f.getPath(), ad.getPath() + File.separator + f.getName());
            } else {
                FileUtil.copyFile(f.getPath(), ad.getPath() + File.separator + f.getName());
            }
        }
    }

    /**
     * Deletes all files from the backupTempFilePath
     */
    private void deleteTempFiles() {
        File f = new File(backupTempFilePath);

        FileUtil.deltree(f, true);
    }



    private void deleteTable(final String table) {

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("delete from " + table);
        dotConnect.getResult();
    }

    interface ObjectFilter {
        boolean includeIt(Object obj);
    }

    /**
     * This method takes an XML file and will try to import it via XStream and Hibernate.
     *
     * @param f - File to be parsed and imported.
     * @param out - Printwriter to write responses to Reponse Printwriter so this method can write to
     *        screen.
     */
    private void doXMLFileImport(File f) throws Exception {
        doXMLFileImport(f, null);
    }

    /**
     * This method takes an XML file and will try to import it via XStream and Hibernate.
     *
     * @param f - File to be parsed and imported.
     * @param out - Printwriter to write responses to Reponse Printwriter so this method can write to
     *        screen.
     * @param filter
     * @throws DotDataException
     * @throws HibernateException
     * @throws EncryptorException
     */
    private void doXMLFileImport(File f, ObjectFilter filter) throws DotDataException, HibernateException, EncryptorException {
        if (f == null) {
            return;
        }


        Reader charStream = null;
        try {
            String _className = null;
            Class _importClass = null;
            HibernateUtil _dh = null;

            boolean counter = false;
            boolean image = false;
            boolean portlet = false;
            boolean logsMappers = false;



            Logger.info(this, "**** Importing the file: " + f + " *****");

            /* if we have a multipart import file */
            Pattern p = Pattern.compile("_[0-9]{8}");
            Matcher m = p.matcher(f.getName());
            if (m.find()) {
                _className = f.getName().substring(0, f.getName().lastIndexOf("_"));
            } else {
                _className = f.getName().substring(0, f.getName().lastIndexOf("."));
            }

            if (_className.equals("Counter")) {
                counter = true;
            } else if (_className.equals("Image")) {
                image = true;
            } else if (_className.equals("Portlet")) {
                portlet = true;
            } else if (_className.equals("LogsMappers")) {
                logsMappers = true;
            } else {
                try {
                    _importClass = Class.forName(_className);
                } catch (Exception e) {
                    Logger.error(this, "Class not found " + _className);
                    return;
                }
            }

            Logger.info(this, "Importing:\t" + _className);


            List l = new ArrayList();

            List all = (List) BundlerUtil.xmlToObject(f);
            if (filter != null) {
                for (Object obj : all)
                    if (filter.includeIt(obj))
                        l.add(obj);
            } else {
                l = all;
            }


            Logger.info(this, "Found :\t" + l.size() + " " + _className + "(s)");
            if (counter) {
                for (int j = 0; j < l.size(); j++) {
                    final HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);

                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into counter values (?,?)");
                    dc.addParam(dcResults.get("name"));
                    dc.addParam(Integer.valueOf(dcResults.get("currentid")));
                    dc.loadResults();

                }
            } else if (image) {
                /*
                 * The changes in this part were made for Oracle databases. Oracle has problems when getString()
                 * method is called on a LONG field on an Oracle database. Because of this, the object is loaded
                 * from liferay and DotConnect is not used
                 */
                for (int j = 0; j < l.size(); j++) {
                    final Image im = (Image) l.get(j);

                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into image values (?,?)");
                    if (!UtilMethods.isSet(im.getImageId()) && DbConnectionFactory.isOracle()) {
                        return;
                    }
                    dc.addParam(im.getImageId());
                    dc.addParam(im.getText());
                    dc.loadResults();

                }
            } else if (portlet) {
                for (int j = 0; j < l.size(); j++) {
                    final HashMap<String, String> dcResults = (HashMap<String, String>) l.get(j);

                    DotConnect dc = new DotConnect();
                    StringBuffer sb = new StringBuffer("insert into portlet values (?,?,?,?,");
                    if (dcResults.get("narrow").equalsIgnoreCase("f") || dcResults.get("narrow").equalsIgnoreCase("false")
                                    || dcResults.get("narrow").equalsIgnoreCase("0")
                                    || dcResults.get("narrow").equals(DbConnectionFactory.getDBFalse()))
                        sb.append(DbConnectionFactory.getDBFalse() + ",?,");
                    else
                        sb.append(DbConnectionFactory.getDBTrue() + ",?,");
                    if (dcResults.get("active_").equalsIgnoreCase("f") || dcResults.get("active_").equalsIgnoreCase("false")
                                    || dcResults.get("active_").equalsIgnoreCase("0")
                                    || dcResults.get("active_").equals(DbConnectionFactory.getDBFalse()))
                        sb.append(DbConnectionFactory.getDBFalse() + ")");
                    else
                        sb.append(DbConnectionFactory.getDBTrue() + ")");
                    dc.setSQL(sb.toString());
                    dc.addParam(dcResults.get("portletid"));
                    dc.addParam(dcResults.get("groupid"));
                    dc.addParam(dcResults.get("companyid"));
                    dc.addParam(dcResults.get("defaultpreferences"));
                    dc.addParam(dcResults.get("roles"));
                    dc.loadResults();

                }
            } else if (logsMappers) {
                for (int j = 0; j < l.size(); j++) {
                    final LogMapperRow logMapperRow = (LogMapperRow) l.get(j);

                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into log_mapper values (?,?,?)");
                    dc.addParam(logMapperRow.getEnabled() ? 1 : 0);
                    dc.addParam(logMapperRow.getLog_name());
                    dc.addParam(logMapperRow.getDescription());
                    dc.loadResults();

                }
            } else if (_importClass.equals(User.class)) {
                for (int j = 0; j < l.size(); j++) {
                    User u = (User) l.get(j);
                    u.setModified(true);
                    if (!u.isDefaultUser() && !u.getUserId().equals("system")) {
                        try {
                            User u1 = APILocator.getUserAPI().createUser(u.getUserId(), u.getEmailAddress());
                            u.setUserId(u1.getUserId());
                            u.setEmailAddress(u.getEmailAddress());
                        } catch (DuplicateUserException e) {
                            Logger.info(this, "user already exists going to update");
                            u = loadUserFromIdOrEmail(u);
                        }

                        APILocator.getUserAPI().save(u, APILocator.getUserAPI().getSystemUser(), false);
                    } else {
                        Logger.info(this, "");
                    }
                }
            } else if (_importClass.equals(Company.class)) {
                for (int j = 0; j < l.size(); j++) {
                    Company c = (Company) l.get(j);
                    // github #16470 with support for custom starter.zips that have updated keys
                    if ("liferay.com".equals(c.getCompanyId())) {
                        continue;
                    }
                    c.setKey(Base64.objectToString(Encryptor.generateKey()));

                    try {
                        c.setModified(true);
                        CompanyManagerUtil.updateCompany(c);
                    } catch (SystemException e) {
                        throw new DotDataException("Unable to load company", e);
                    }
                }
            } else if (_importClass.equals(Language.class)) {
                for (Object aL : l) {
                    final Language lang = (Language) aL;


                    DotConnect dc = new DotConnect();
                    dc.setSQL("insert into language (id,language_code,country_code,language,country) values (?,?,?,?,?)");
                    dc.addParam(lang.getId());
                    dc.addParam(lang.getLanguageCode());
                    dc.addParam(lang.getCountryCode());
                    dc.addParam(lang.getLanguage());
                    dc.addParam(lang.getCountry());
                    dc.loadResults();


                }

            } else if (_importClass.equals(UsersRoles.class)) {
                for (Object role : l) {
                    final UsersRoles userRole = (UsersRoles) role;

                    // upsert pattern for the user role
                    new DotConnect().setSQL("delete from users_cms_roles where user_id=? and role_id=?")
                                    .addParam(userRole.getUserId())
                                    .addParam(userRole.getRoleId())
                                    .loadResult();
                    new DotConnect().setSQL("insert into users_cms_roles (id,user_id,role_id) values (?,?,?)")
                                    .addParam(userRole.getId())
                                    .addParam(userRole.getUserId())
                                    .addParam(userRole.getRoleId())
                                    .loadResult();



                }

            }



            else {
                String id;
                if (_importClass.equals(Relationship.class)) {
                    id = "inode";
                } else {
                    _dh = new HibernateUtil(_importClass);
                    id = HibernateUtil.getSession().getSessionFactory().getClassMetadata(_importClass)
                                    .getIdentifierPropertyName();
                    HibernateUtil.getSession().flush();
                }

                boolean identityOn = false;
                String cName = _className.substring(_className.lastIndexOf(".") + 1);
                String tableName = "";
                if (classesWithIdentity.contains(cName) && DbConnectionFactory.isMsSql() && !cName.equalsIgnoreCase("inode")) {
                    tableName = tableNames.get(cName);
                    turnIdentityOnMSSQL(tableName);
                    identityOn = true;
                }
                for (int j = 0; j < l.size(); j++) {
                    Object obj = l.get(j);
                    if (l.get(j) instanceof com.dotmarketing.portlets.contentlet.business.Contentlet
                                    && DbConnectionFactory.isMsSql()) {
                        com.dotmarketing.portlets.contentlet.business.Contentlet contentlet =
                                        (com.dotmarketing.portlets.contentlet.business.Contentlet) l.get(j);
                        changeDateForSQLServer(contentlet);
                    }

                    if (UtilMethods.isSet(id)) {
                        String prop = BeanUtils.getProperty(obj, id);
                        try {
                            if (id.substring(id.length() - 2, id.length()).equalsIgnoreCase("id")) {
  
                                Logger.debug(this, "Saving the object: " + obj.getClass() + ", with the id: " + prop);

                                long myId = Try.of(() -> Long.parseLong(prop)).getOrElse(-1l);
                                if (myId > 0) {
                                    HibernateUtil.saveWithPrimaryKey(obj, myId);
                                } else {
                                    HibernateUtil.saveWithPrimaryKey(obj, prop);
                                }

                            } else {
                                if (obj instanceof Relationship) {
                                    
                                    
                                    Relationship rel = (Relationship) obj;
                                    
                                    new DotConnect().setSQL("delete from relationship where inode=? or relation_type_value=?").addParam(rel.getInode()).addParam(rel.getRelationTypeValue()).loadResult();
                                    new DotConnect().setSQL("delete from inode where inode=?").addParam(rel.getInode()).loadResult();
                                    
                                    
                                    APILocator.getRelationshipAPI().create(Relationship.class.cast(obj));
                                } else {

                                    Logger.debug(this, "Saving the object: " + obj.getClass() + ", with the id: " + prop);
                                    HibernateUtil.saveWithPrimaryKey(obj, prop);

                                    HibernateUtil.flush();
                                }
                            }
                        } catch (Exception e) {
                            try {

                                if (obj != null && !(obj instanceof Identifier)) {

                                    Logger.debug(this,
                                                    "Error on trying to save: " + e.getMessage()
                                                                    + ", trying to Save the object again: " + obj.getClass()
                                                                    + ", with the id: " + prop);

                                    HibernateUtil.saveWithPrimaryKey(obj, prop);
                                    HibernateUtil.flush();
                                }
                            } catch (Exception ex) {
                                Logger.debug(this,
                                                "Usually not a problem can be that duplicate data or many times a row of data that is created by the system and is trying to be imported again : "
                                                                + ex.getMessage(),
                                                ex);
                                Logger.warn(this,
                                                "Usually not a problem can be that duplicate data or many times a row of data that is created by the system and is trying to be imported again : "
                                                                + ex.getMessage());
                                Logger.info(this, "Problematic object: " + obj + " prop:" + prop);


                                try {

                                    HibernateUtil.flush();
                                } catch (Exception e1) {
                                }
                                continue;
                            }
                        }
                    } else {
                        if (obj instanceof Tree) {
                            final Tree t = (Tree) obj;


                            DotConnect dc = new DotConnect();
                            List<String> inodeList = new ArrayList<String>();
                            dc.setSQL("select inode from inode where inode = ? or inode = ?");
                            dc.addParam(t.getParent());
                            dc.addParam(t.getChild());
                            inodeList.addAll(dc.loadResults());
                            dc.setSQL("select id from identifier where id = ? or id = ?");
                            dc.addParam(t.getParent());
                            dc.addParam(t.getChild());
                            inodeList.addAll(dc.loadResults());
                            if (inodeList.size() > 0) {
                                HibernateUtil.save(obj);
                            } else {
                                Logger.warn(this.getClass(), "Skipping tree- no matching inodes: {parent=" + t.getParent()
                                                + ", child=" + t.getChild() + "}");
                            }

                        } else if (obj instanceof MultiTree) {
                            final MultiTree t = (MultiTree) obj;
                            APILocator.getMultiTreeAPI().saveMultiTree(t);

                        } else {

                            Logger.debug(this, "Saving the object: " + obj.getClass() + ", with the values: " + obj);


                            HibernateUtil.save(obj);
                            HibernateUtil.flush();

                        }
                    }

                    HibernateUtil.getSession().flush();


                }
                if (identityOn) {
                    turnIdentityOffMSSQL(tableName);
                }
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        } finally {
            try {
                if (charStream != null) {
                    charStream.close();
                }
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    private User loadUserFromIdOrEmail(final User u) throws DotDataException, DotSecurityException {
        User user = null;

        try {
            user = APILocator.getUserAPI().loadUserById(u.getUserId(), APILocator.getUserAPI().getSystemUser(), false);
        } catch (Exception e1) {
            Logger.info(this, "couldn't find user by ID going to lookup by email address");
            user = APILocator.getUserAPI().loadByUserByEmail(u.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);
        }

        return user;
    }

    /**
     *
     * @param tableName
     * @throws SQLException
     */
    private void turnIdentityOnMSSQL(String tableName) throws SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement("set identity_insert " + tableName + " on");
    }

    /**
     *
     * @param tableName
     * @throws SQLException
     */
    private void turnIdentityOffMSSQL(String tableName) throws SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement("set identity_insert " + tableName + " off");
    }

    /**
     *
     */
    private void cleanUpDBFromImport() {
        DotConnect dc = new DotConnect();
        try {
            if (DbConnectionFactory.isOracle()) {
                for (String clazz : classesWithIdentity) {
                    String tableName = tableNames.get(clazz);
                    dc.setSQL("drop sequence " + sequences.get(tableName));
                    dc.loadResults();
                    dc.setSQL("select max(" + tableIDColumns.get(tableName) + ") as maxID from " + tableName);
                    ArrayList<HashMap<String, String>> results = dc.loadResults();
                    int max = dc.loadResults().size() == 0 ? 0 : Parameter.getInt(dc.getString("maxID"), 1);
                    dc.setSQL("CREATE SEQUENCE " + sequences.get(tableName) + " MINVALUE 1 START WITH " + (max + 100)
                                    + " INCREMENT BY 1");
                    dc.loadResults();
                }
            } else if (DbConnectionFactory.isPostgres()) {
                for (String clazz : classesWithIdentity) {
                    String tableName = tableNames.get(clazz);
                    dc.setSQL("select max(" + tableIDColumns.get(tableName) + ") as maxID from " + tableName);
                    ArrayList<HashMap<String, String>> results = dc.loadResults();
                    int max = dc.loadResults().size() == 0 ? 0 : Parameter.getInt(dc.getString("maxID"), 1);
                    dc.setSQL("alter sequence " + sequences.get(tableName) + " restart with " + (max + 1));
                    dc.loadResults();
                }
            }
        } catch (DotDataException e) {
            Logger.error(this, "cleanUpDBFromImport failed:" + e, e);
        }
    }

    /**
     *
     * @return
     */
    public String getBackupTempFilePath() {
        return backupTempFilePath;
    }

    /**
     *
     * @param backupTempFilePath
     */
    public void setBackupTempFilePath(String backupTempFilePath) {
        this.backupTempFilePath = backupTempFilePath;
    }

    /**
     *
     * @param zipFile
     * @return
     */
    public boolean validateZipFile() {
        String tempdir = getBackupTempFilePath();

        if (starterZip == null || !starterZip.exists()) {
            throw new DotStateException("Starter.zip does not exist:" + starterZip);
        }

        try {
            deleteTempFiles();

            File ftempDir = new File(tempdir);
            ftempDir.mkdirs();
            File tempZip = new File(tempdir + File.separator + starterZip.getName());
            tempZip.createNewFile();

            try (final ReadableByteChannel inputChannel = Channels.newChannel(Files.newInputStream(starterZip.toPath()));
                            final WritableByteChannel outputChannel =
                                            Channels.newChannel(Files.newOutputStream(tempZip.toPath()))) {

                FileUtil.fastCopyUsingNio(inputChannel, outputChannel);
            }

            /*
             * Unzip zipped backups
             */
            if (starterZip != null && starterZip.getName().toLowerCase().endsWith(".zip")) {
                ZipFile z = new ZipFile(starterZip);
                ZipUtil.extract(z, new File(backupTempFilePath));
            }
            return true;
        } catch (Exception e) {
            throw new DotStateException("Starter.zip invalid:" + e.getMessage(), e);
        }
    }

    /**
     *
     * @param date
     * @return
     */
    private boolean validateDate(Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(1753, 01, 01);
        boolean validated = true;
        if (date != null && date.before(calendar.getTime())) {
            validated = false;
        }
        return validated;
    }

    /**
     *
     * @param contentlet
     * @param out
     */
    private void changeDateForSQLServer(com.dotmarketing.portlets.contentlet.business.Contentlet contentlet) {
        if (!validateDate(contentlet.getDate1())) {
            contentlet.setDate1(new Date());
            Logger.warn(ImportStarterUtil.class,
                            "Unsupported data in SQL Server, so changed date to current date for contentlet with inode ");
        }
        if (!validateDate(contentlet.getDate2())) {
            contentlet.setDate2(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate3())) {
            contentlet.setDate3(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate4())) {
            contentlet.setDate4(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate5())) {
            contentlet.setDate5(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate6())) {
            contentlet.setDate6(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate7())) {
            contentlet.setDate7(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate8())) {
            contentlet.setDate8(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate9())) {
            contentlet.setDate9(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate10())) {
            contentlet.setDate10(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate11())) {
            contentlet.setDate11(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate12())) {
            contentlet.setDate12(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate13())) {
            contentlet.setDate13(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate14())) {
            contentlet.setDate14(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate15())) {
            contentlet.setDate15(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate16())) {
            contentlet.setDate16(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate17())) {
            contentlet.setDate17(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate18())) {
            contentlet.setDate18(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate19())) {
            contentlet.setDate19(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate20())) {
            contentlet.setDate20(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate21())) {
            contentlet.setDate21(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate22())) {
            contentlet.setDate22(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate23())) {
            contentlet.setDate23(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate24())) {
            contentlet.setDate24(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
        if (!validateDate(contentlet.getDate25())) {
            contentlet.setDate25(new Date());
            Logger.warn(ImportStarterUtil.class, "Date changed to current date");
        }
    }

    List<File> contains(String pattern) {
        List<File> matches = tempFiles.stream().filter(f -> f.getName().contains(pattern)).collect(Collectors.toList());

        tempFiles.removeAll(matches);
        return matches;



    }

    List<File> startsWith(String pattern) {
        List<File> matches = tempFiles.stream().filter(f -> f.getName().startsWith(pattern)).collect(Collectors.toList());
        tempFiles.removeAll(matches);
        return matches;

    }

    List<File> endsWith(String pattern) {
        List<File> matches = tempFiles.stream().filter(f -> f.getName().endsWith(pattern)).collect(Collectors.toList());
        tempFiles.removeAll(matches);
        return matches;

    }
    private void deleteDotCMS() {
        try {
            /* get a list of all our tables */
            final ArrayList<String> _tablesToDelete = new ArrayList<String>();
            Map map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();
   

            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                AbstractEntityPersister cmd = (AbstractEntityPersister) pairs.getValue();
                String tableName = cmd.getTableName();

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

            _tablesToDelete.add("structure");
            _tablesToDelete.add("folder");
            _tablesToDelete.add("identifier");
            _tablesToDelete.add("inode");;
            _tablesToDelete.add("user_");
            _tablesToDelete.add("user_proxy");
            
            _tablesToDelete.add("company");
            _tablesToDelete.add("counter");
            _tablesToDelete.add("image");
            _tablesToDelete.add("portlet");
            _tablesToDelete.add("portletpreferences");
            _tablesToDelete.add("address");
            _tablesToDelete.add("address");
            
            _tablesToDelete.add("plugin_property");
            _tablesToDelete.add("plugin");


            for (String table : _tablesToDelete) {
                Logger.info(this, "About to delete all records from " + table);
                this.deleteTable(table);
                Logger.info(this, "Deleted all records from " + table);
            }
        } catch (HibernateException e) {
            Logger.error(this,e.getMessage(),e);
        }

        new File(ConfigUtils.getAbsoluteAssetsRootPath()).mkdirs();



    }

}
