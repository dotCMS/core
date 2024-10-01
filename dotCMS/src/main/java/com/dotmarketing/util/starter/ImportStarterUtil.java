package com.dotmarketing.util.starter;

import static com.dotcms.util.ConversionUtils.toLong;
import static com.dotmarketing.util.ConfigUtils.getDeclaredDefaultLanguage;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.net.sf.hibernate.persister.AbstractEntityPersister;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UsersRoles;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.util.RulesImportExportObject;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.FileUtil;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import org.apache.commons.beanutils.BeanUtils;


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
    private ArrayList<String> classesWithIdentity = new ArrayList<>();
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
        tempFiles.removeIf(f -> f.getName().endsWith("Counter.json")
                || f.getName().contains(".Dashboard")
                || f.getName().contains(".FixAudit_")
                || f.getName().contains(".UserProxy_")
                || f.getName().contains(".PluginProperty_")
                || f.getName().contains(".Plugin_")
        );

        classesWithIdentity.add("Permission");
        classesWithIdentity.add("UsersToDelete");
        classesWithIdentity.add("UserPreference");
        tableNames = new HashMap<>();
        tableNames.put("Permission", "permission");
        tableNames.put("UsersToDelete", "users_to_delete");
        tableNames.put("UserPreference", "user_preferences");

        if (DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle()) {
            sequences = new HashMap<>();
            sequences.put("permission", "permission_seq");
            sequences.put("users_to_delete", "user_to_delete_seq");
            sequences.put("user_preferences", "user_preferences_seq");
            tableIDColumns = new HashMap<>();
            tableIDColumns.put("permission", "id");
            tableIDColumns.put("users_to_delete", "id");
            tableIDColumns.put("user_preferences", "id");
        }
    }

    /**
     * Takes a ZIP file from the /temp directory to restore dotCMS data. Currently, it will blow away
     * all current data. This method cannot currently be run in a transaction. For performance reasons
     * with DB drivers and connections it closes the session every so often.
     *
     * @throws IOException
     * @throws Exception
     */
    @WrapInTransaction
    public void doImport() throws Exception {

        final Map<String, String> fileTypesInodes = new HashMap<>();

        Logger.info(this, "Found " + tempFiles.size() + " files to import");

        copyAssetDir();
        deleteDotCMS();

        for (StarterEntity entity: StarterEntity.entitiesToImport){
            if (Identifier.class.getCanonicalName().equals(entity.fileName())){
                saveIdentifiers();
                continue;
            }

            if (ContentType.class.equals(entity.type())){
                // we need content types before contentlets
                // but content types have references to folders and hosts identifiers
                // so, here is the place to do it
                for (File file : endsWith(ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION)) {
                    new ContentTypeImportExportUtil().importContentTypes(file);
                }
                updateFolderFileType(fileTypesInodes);
                continue;
            }

            for (File file : contains(entity.fileName())) {
                if (Folder.class.getCanonicalName().equals(entity.fileName())){
                    // we store here defaultFileType for every folder
                    // because of mutual folder <--> structure dependency

                    try {
                        doJSONFileImport(file, entity.type(), new ObjectFilter() {
                            public boolean includeIt(Object obj) {
                                Folder f = (Folder) obj;
                                fileTypesInodes.put(f.getInode(), f.getDefaultFileType());
                                f.setDefaultFileType(null);
                                return true;
                            }
                        });
                    } catch (Exception e) {
                        Logger.error(this, "Unable to load " + file.getName() + " : " + e.getMessage(), e);
                    }
                    continue;
                }

                if (WorkflowSchemeImportExportObject.class.equals(entity.type())){
                    WorkflowImportExportUtil.getInstance().importWorkflowExport(file);
                    continue;
                }

                if (RulesImportExportObject.class.equals(entity.type())){
                    RulesImportExportUtil.getInstance().importRules(file);
                    continue;
                }

                doJSONFileImport(file, entity.type());

                if (entity.fileName().startsWith(Contentlet.class.getCanonicalName())) {
                    // content types and relationships are imported before sites
                    // so we need to clear the host cache to avoid missed hosts references
                    CacheLocator.getHostCache().clearCache();
                }

                if (Contentlet.class.getCanonicalName().equals(entity.fileName())){
                    updateContentletToNewDefaultLang();
                }

                if (ContentletVersionInfo.class.getCanonicalName().equals(entity.fileName())){
                    updateContentletVersionInfoToNewDefaultLang();
                }

                if (WorkflowTask.class.getCanonicalName().equals(entity.fileName())) {
                    updateWorkflowTaskToNewDefaultLang();
                }
            }
        }

        cleanUpDBFromImport();

        Logger.info(ImportStarterUtil.class, "Done Importing");
        deleteTempFiles();

    }

    private void updateFolderFileType(Map<String, String> fileTypesInodes) throws DotDataException {
        // updating file_type on folder now that structures were added
        DotConnect dc = new DotConnect();
        for (Entry<String, String> entry : fileTypesInodes.entrySet()) {
            dc.setSQL("update folder set default_file_type=? where inode=?");
            dc.addParam(entry.getValue());
            dc.addParam(entry.getKey());
            dc.loadResult();
        }
    }

    private void saveIdentifiers()
            throws DotDataException {
        /*
         * Because of the parent check we do at db we need to import folder identifiers first but for the
         * same reason we need to sort them first by parent_path
         */
        final List<Identifier> folderIdents = new ArrayList<>();
        final List<Identifier> otherIdents = new ArrayList<>();
        List<File> identifiers = contains(Identifier.class.getCanonicalName());
        // collecting all folder identifiers
        for (File ff : identifiers) {
            final List<Identifier> idents = BundlerUtil.jsonToObject(ff, new TypeReference<>() {});
            folderIdents.addAll(
                    idents.stream().filter(identifier -> identifier.getAssetType().equals("folder"))
                            .collect(
                                    Collectors.toList()));

            otherIdents.addAll(idents.stream()
                    .filter(identifier -> !identifier.getAssetType().equals("folder")).collect(
                            Collectors.toList()));
        }

        // sorting folder identifiers by parent path in order to pass parent check
        Collections.sort(folderIdents, Comparator.comparing(Identifier::getParentPath));

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
    }

    /**
     * Updates all the data on the Contentlet Table if the default lang was changed.
     */
    private void updateContentletToNewDefaultLang() throws DotDataException {
        final long defaultLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if(defaultLangId!=1) {
            Logger.info(this,"Updating contentlets to the new default language");
            if (DbConnectionFactory.isMySql()) {
                new DotConnect()
                        .setSQL("update contentlet set language_id = ? where language_id = 1 and"
                                + " identifier not in (select * from (select identifier from "
                                + "contentlet where language_id = ?) as id)")
                        .addParam(defaultLangId).addParam(defaultLangId).loadResult();
            } else {
                new DotConnect()
                        .setSQL("update contentlet set language_id = ? where language_id = 1 and"
                                + " identifier not in (select identifier from contentlet "
                                + "where language_id = ?)")
                        .addParam(defaultLangId).addParam(defaultLangId).loadResult();
            }
        }
    }

    /**
     * Updates all the data on the Workflow_Task Table if the default lang was changed.
     */
    private void updateWorkflowTaskToNewDefaultLang() throws DotDataException {
        final long defaultLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defaultLangId != 1) {
            Logger.info(this, "Updating workflow_task to the new default language");
            if (DbConnectionFactory.isMySql()) {
                new DotConnect().setSQL("update workflow_task set language_id = ? where language_id = 1 "
                                + "and webasset not in (select * from (select webasset "
                                + "from workflow_task where language_id = ?) as id)")
                        .addParam(defaultLangId).addParam(defaultLangId).loadResult();
            } else {
                new DotConnect().setSQL("update workflow_task set language_id = ? where language_id = 1 "
                                + "and webasset not in (select webasset from workflow_task where language_id = ?)")
                        .addParam(defaultLangId).addParam(defaultLangId).loadResult();
            }
        }
    }

    /**
     * Updates all the data on the ContentletVersionInfo Table if the default lang was changed.
     */
    private void updateContentletVersionInfoToNewDefaultLang() throws DotDataException {
        final long defaultLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defaultLangId != 1) {
            Logger.info(this, "Updating contentlet_version_info to the new default language");
            if (DbConnectionFactory.isMySql()) {
                new DotConnect()
                        .setSQL("update contentlet_version_info set lang = ? where lang = 1 "
                                + "and identifier not in (select * from (select identifier "
                                + "from contentlet_version_info where lang = ?) as id)")
                        .addParam(defaultLangId).addParam(defaultLangId).loadResult();
            } else {
                new DotConnect()
                        .setSQL("update contentlet_version_info set lang = ? where lang = 1 "
                                + "and identifier not in (select identifier from "
                                + "contentlet_version_info where lang = ?)")
                        .addParam(defaultLangId).addParam(defaultLangId).loadResult();
            }
        }
    }

    /**
     *
     * @throws IOException
     */
    private void copyAssetDir() throws IOException {

        final Optional<File> assetDir = tempFiles.stream()
                .filter(f -> ("asset".equals(f.getName()) || "assets".equals(f.getName())) && f
                        .isDirectory()).findAny();
        if (assetDir.isPresent()) {
            final File fromAssetDir = assetDir.get();
            final File ad = new File(assetPath);

            ad.mkdirs();
            final String[] fileNames = fromAssetDir.list();
            for (int i = 0; i < fileNames.length; i++) {
                final File f = new File(fromAssetDir.getPath() + File.separator + fileNames[i]);
                if (f.getName().equals(".svn")) {
                    continue;
                }
                if (f.getName().equals("license.dat")) {
                    continue;
                }
                if (f.isDirectory()) {
                    FileUtil.copyDirectory(f.getPath(),
                            ad.getPath() + File.separator + f.getName());
                } else {
                    FileUtil.copyFile(f.getPath(), ad.getPath() + File.separator + f.getName());
                }
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
     * This method takes a JSON file and will try to import it via Jackson and Hibernate.
     *
     * @param f - File to be parsed and imported.
     */
    private void doJSONFileImport(final File f, final Object type) throws Exception {
        doJSONFileImport(f, type,null);
    }




    /**
     * This method takes a JSON file and will try to import it via Jackson and Hibernate.
     *
     * @param file - File to be parsed and imported.
     * @param filter
     * @throws DotDataException
     * @throws HibernateException
     * @throws EncryptorException
     * @throws SQLException
     * @throws DotSecurityException
     * @throws DuplicateUserException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void doJSONFileImport(final File file, final Object type, final ObjectFilter filter) throws Exception {
        if (file == null) {
            return;
        }

        Class _importClass = null;
        HibernateUtil _dh = null;

        boolean counter = false;
        boolean image = false;
        boolean portlet = false;


        Pattern classNamePattern = Pattern.compile("_[0-9]{8}");
        Logger.info(this, "**** Importing the file: " + file + " *****");

        /* if we have a multipart import file */

        final String _className = classNamePattern.matcher(file.getName()).find()
                ?  file.getName().substring(0, file.getName().lastIndexOf("_"))
                :   file.getName().substring(0, file.getName().lastIndexOf("."));


        if (_className.equals("Counter")) {
            counter = true;
        } else if (_className.equals("Image")) {
            image = true;
        } else if (_className.equals("Portlet")) {
            portlet = true;
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
        List all;

        if (Contentlet.class.equals(_importClass)){
            l = getContentletList(file);
        } else{
            if (type instanceof TypeReference){
                all = (List) BundlerUtil.jsonToObject(file, (TypeReference)type);
            } else{
                all = (List) BundlerUtil.jsonToObject(file, (Class)type);
            }
            if (filter != null) {
                for (Object obj : all)
                    if (filter.includeIt(obj))
                        l.add(obj);
            } else {
                l = all;
            }
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

            //Once all the languages are loaded We'll attempt updating the company table with the default language.
            long existingLangId = 1;
            final String defaultCompanyId = PublicCompanyFactory.getDefaultCompanyId();
            final Tuple2<String, String> defaultLanguageDeclaration = getDeclaredDefaultLanguage();
            final String langCode = defaultLanguageDeclaration._1;
            final String countryCode = defaultLanguageDeclaration._2;

            if (UtilMethods.isSet(langCode) || UtilMethods.isSet(countryCode)) {
                final Map<String, Object> map = new DotConnect()
                        .setSQL("select id from language where lower(language_code) = ? and lower(country_code) = ?")
                        .addParam(langCode)
                        .addParam(countryCode)
                        .loadObjectResults()
                        .stream()
                        .findFirst()
                        .orElse(null);
                if(null != map) {
                    existingLangId = toLong(map.get("id"), 1L);
                } else {
                    Logger.warn(ImportStarterUtil.class,String.format("Failed to set the declared default language [%s-%s] as the system default language",langCode,countryCode));
                }
            }
            //If no default lang is found fallback to 1
            new DotConnect().setSQL("UPDATE company SET default_language_id = ? WHERE companyid = ? ")
                    .addParam(existingLangId)
                    .addParam(defaultCompanyId)
                    .loadResult();


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

        } else if (Role.class.equals(_importClass)) {
            List<Role> roles = (List<Role>) l;
            Collections.sort(roles);
            for (Role role : roles) {
                _dh = new HibernateUtil(Role.class);
                if(UtilMethods.isSet(role.getRoleKey())) {
                    List<Map<String,Object>>  matches= new DotConnect().setSQL("select * from cms_role where role_key =?").addParam(role.getRoleKey()).loadObjectResults();
                    Logger.info(this.getClass(), "roleKey:" +role.getRoleKey() + " = " + matches.size() );
                }

                _dh.saveWithPrimaryKey(role, role.getId());
                HibernateUtil.getSession().flush();
            }
        } else {
            String id;
            if (Relationship.class.equals(_importClass) || Template.class.equals(_importClass)
                    || Contentlet.class.equals(_importClass) || Category.class.equals(_importClass)) {
                id = "inode";
            } else if(_importClass.equals(ContentletVersionInfo.class) || _importClass.equals(Folder.class)) {
                id = "identifier";
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
                if (l.get(j) instanceof Contentlet
                        && DbConnectionFactory.isMsSql()) {
                    Contentlet contentlet =
                            (Contentlet) l.get(j);
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
                            if(obj instanceof Category){
                                FactoryLocator.getCategoryFactory().save((Category) obj);
                            } else if (obj instanceof Contentlet) {
                                FactoryLocator.getContentletFactory().save((Contentlet) obj);
                            } else if (obj instanceof Relationship) {
                                Relationship rel = (Relationship) obj;
                                if(new DotConnect().setSQL("select count(*) as counter from relationship where relation_type_value=?")
                                        .addParam(rel.getRelationTypeValue())
                                        .getInt("counter")==0) {
                                    APILocator.getRelationshipAPI().save(Relationship.class.cast(obj), rel.getInode());
                                }

                            } else if (obj instanceof ContentletVersionInfo) {
                                ContentletVersionInfo cvi = (ContentletVersionInfo) obj;
                                APILocator.getVersionableAPI().saveContentletVersionInfo(cvi);
                            } else if (obj instanceof Template) {
                                final Template template = Template.class.cast(obj);
                                FactoryLocator.getTemplateFactory().save(template, template.getInode());
                            } else if (obj instanceof Folder) {
                                final Folder folder = Folder.class.cast(obj);
                                FactoryLocator.getFolderFactory().save(folder);
                            } else{
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



                            HibernateUtil.flush();

                            continue;
                        }
                    }
                } else {
                    if (obj instanceof Tree) {
                        final Tree t = (Tree) obj;


                        DotConnect dc = new DotConnect();
                        List<String> inodeList = new ArrayList<>();
                        dc.setSQL("select inode from inode where inode = ? or inode = ?");
                        dc.addParam(t.getParent());
                        dc.addParam(t.getChild());
                        inodeList.addAll(dc.loadResults());
                        dc.setSQL("select id from identifier where id = ? or id = ?");
                        dc.addParam(t.getParent());
                        dc.addParam(t.getChild());
                        inodeList.addAll(dc.loadResults());
                        if (inodeList.size() > 0) {
                            TreeFactory.saveTree(t);
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

    }

    /**
     * Populates a list of {@link Contentlet} given a JSON file
     * @param file
     * @return {@link List<Contentlet}
     * @throws IOException
     */
    private List<Contentlet> getContentletList(final File file)  {
        return ContentletJsonHelper.INSTANCE.get().readContentletListFromJsonFile(file).stream().map(
                cont -> {
                    try {
                        return APILocator.getContentletJsonAPI().toMutableContentlet(cont);
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.warnAndDebug(ImportStarterUtil.class,
                                "Error getting mutable contentlet with inode: " + cont.inode(), e);
                        return new Contentlet();
                    }
                }).collect(Collectors.toList());
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
     */
    private void changeDateForSQLServer(Contentlet contentlet) {

        final List<Field> dateFields = contentlet.getContentType().fields().stream()
                .filter(field -> field.dataType() == DataTypes.DATE).collect(Collectors.toList());

        dateFields.forEach(field -> {
            if (!validateDate(contentlet.getDateProperty(field.variable()))) {
                contentlet.setDateProperty(field.variable(), new Date());
                Logger.warn(ImportStarterUtil.class,
                        "Unsupported data in SQL Server, so changed date to current date for contentlet with inode "
                                + contentlet.getInode());
            }
        });

    }

    List<File> contains(String pattern) {
        List<File> matches = tempFiles.stream().filter(f -> f.getName().contains(pattern)).collect(Collectors.toList());

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
            final List<String> _tablesToDelete = new ArrayList<>();

            //These tables will be added later to preserve order
            final List<String> tablesToIgnore = getTablesToIgnore();

            final Map map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();

            final Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                final Entry pairs = (Entry) it.next();
                final AbstractEntityPersister cmd = (AbstractEntityPersister) pairs.getValue();
                final String tableName = cmd.getTableName();

                if(!tablesToIgnore.contains(tableName.toLowerCase())){
                    _tablesToDelete.add(tableName);
                }
            }

            //these tables should be deleted in this order to avoid conflicts with foreign keys
            _tablesToDelete.addAll(tablesToIgnore);

            for (final String table : _tablesToDelete) {
                Logger.info(this, "About to delete all records from " + table);
                this.deleteTable(table);
                Logger.info(this, "Deleted all records from " + table);
            }
        } catch (HibernateException e) {
            Logger.error(this,e.getMessage(),e);
        }

        new File(ConfigUtils.getAbsoluteAssetsRootPath()).mkdirs();



    }

    private List<String> getTablesToIgnore() {
        final List<String> tablesToIgnore = new ArrayList<>();
        tablesToIgnore.add("cms_layouts_portlets");
        tablesToIgnore.add("layouts_cms_roles");
        tablesToIgnore.add("users_cms_roles");
        tablesToIgnore.add("cms_role");
        tablesToIgnore.add("cms_layout");

        tablesToIgnore.add("structure");
        tablesToIgnore.add("folder");
        tablesToIgnore.add("identifier");
        tablesToIgnore.add("inode");

        tablesToIgnore.add("user_");

        tablesToIgnore.add("company");
        tablesToIgnore.add("counter");
        tablesToIgnore.add("image");
        tablesToIgnore.add("portlet");
        tablesToIgnore.add("portletpreferences");
        tablesToIgnore.add("address");

        tablesToIgnore.add("plugin_property");
        tablesToIgnore.add("plugin");
        return tablesToIgnore;
    }

}
