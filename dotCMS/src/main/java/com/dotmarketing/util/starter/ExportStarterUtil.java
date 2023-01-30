package com.dotmarketing.util.starter;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.cmsmaintenance.util.AssetFileNameFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TrashUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;

import javax.servlet.ServletException;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import static java.io.File.separator;

/**
 * Utility class used to generate a bundle with assets and data (starter file)
 * The bundle will be a compressed file with assets and data modeled in JSON files
 * @author nollymarlonga
 */
public class ExportStarterUtil {

    private static final String STARTER_FILE_NAME_PROP = "starter.download.filename";
    private static final String DEFAULT_STARTER_FILENAME_FORMAT = "backup_%s-%s.zip";
    private static final NumberFormat DEFAULT_JSON_FILE_DATA_FORMAT = new DecimalFormat("0000000000");

    private static final String STARTER_FILE_NAME = Config.getStringProperty(STARTER_FILE_NAME_PROP,
            DEFAULT_STARTER_FILENAME_FORMAT);
    private static final String JSON_FILE_EXT = ".json";

    /** Only 10,000,000 records of any given data type wil be exported. */
    private static final int MAX_EXPORTED_RECORDS = 10000000;
    private static final int EXPORTED_RECORDS_PAGE_SIZE = 1000;

    final File outputDirectory;
    
    public ExportStarterUtil() {
        
        //outputDirectory = new File(ConfigUtils.getBackupPath() + File.separator + "backup_" + System.currentTimeMillis());
        //outputDirectory.mkdirs();
        outputDirectory = null;
        
    }
    
    public File createStarterWithAssets() throws IOException {
        
        moveAssetsToBackupDir();
        return createStarterData() ;
        
    }
    
    public File createStarterData() {
        createJSONFiles() ;
        
        return outputDirectory;
        
        
    }

    /**
     *
     * @return
     * @throws HibernateException
     */
    private Set getTableSet() throws HibernateException {
        final Set<Class<?>> _tablesToDump = new HashSet<>();
        _tablesToDump.add(Identifier.class);
        _tablesToDump.add(Language.class);
        _tablesToDump.add(Relationship.class);
        _tablesToDump.add(ContentletVersionInfo.class);
        _tablesToDump.add(Template.class);
        _tablesToDump.add(Contentlet.class);
        _tablesToDump.add(Category.class);
        _tablesToDump.add(Tree.class);
        _tablesToDump.add(MultiTree.class);
        _tablesToDump.add(Folder.class);
        _tablesToDump.add(Company.class);
        _tablesToDump.add(User.class);

        //end classes no longer mapped with Hibernate
        _tablesToDump.addAll(HibernateUtil.getSession().getSessionFactory().getAllClassMetadata().keySet());

        _tablesToDump.removeIf(c->c.equals(Inode.class));
        _tablesToDump.removeIf(c->c.equals(Clickstream.class));
        _tablesToDump.removeIf(c->c.equals(ClickstreamRequest.class));
        _tablesToDump.removeIf(c->c.equals(Clickstream404.class));
        _tablesToDump.removeIf(c->c.equals(Structure.class));
        _tablesToDump.removeIf(c->c.equals(Field.class));
        _tablesToDump.removeIf(c->c.equals(WorkflowHistory.class));
        _tablesToDump.removeIf(c->c.equals(PermissionReference.class));
        _tablesToDump.removeIf(t->t.getName().startsWith("Dashboard"));
        _tablesToDump.removeIf(t->t.getName().contains("HBM"));

        return _tablesToDump;
    }

    /**
     * This method will pull a list of all tables /classed being managed by
     * hibernate and export them, one class per file to the backupTempFilePath
     * as valid JSON. It uses Jackson to write the json out to the files.
     *
     * @throws ServletException
     * @throws IOException
     * @author Will
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private void createJSONFiles()  {

        Logger.info(this, "Starting createJSONFiles into " + outputDirectory);


        try {

            /* get a list of all our tables */
            //Including classes that are no longer mapped with Hibernate anymore
            final Set<Class> _tablesToDump = getTableSet();

            HibernateUtil _dh = null;
            DotConnect dc = null;
            List _list = null;
            File _writing = null;
            java.text.NumberFormat formatter = new java.text.DecimalFormat("0000000000");

            for (Class clazz : _tablesToDump) {
                int i= 0;
                int step = 1000;
                int total =0;
                
                /* we will only export 10,000,000 items of any given type */
                for(i=0;i < 10000000;i=i+step) {

                    _dh = new HibernateUtil(clazz);
                    _dh.setFirstResult(i);
                    _dh.setMaxResults(step);

                    if (Tree.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM tree order by parent, child, relation_type")
                                .setStartRow(i).setMaxRows(step);
                    } else if (MultiTree.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL(
                                        "SELECT * FROM multi_tree order by parent1, parent2, child, relation_type")
                                .setStartRow(i).setMaxRows(step);
                    } else if (TagInode.class.equals(clazz)) {
                        _dh.setQuery("from " + clazz.getName() + " order by inode, tag_id");
                    } else if (Tag.class.equals(clazz)) {
                        _dh.setQuery("from " + clazz.getName() + " order by tag_id, tagname");
                    } else if (Identifier.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("select * from identifier order by parent_path, id")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Language.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM language order by id")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Relationship.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM relationship order by inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM contentlet_version_info ORDER BY identifier")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Template.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM template ORDER BY inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Contentlet.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL(
                                        "select contentlet.*, contentlet_1_.owner from contentlet join inode contentlet_1_ "
                                                + " on contentlet_1_.inode = contentlet.inode ORDER BY contentlet.inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Category.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM category ORDER BY inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Folder.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM folder ORDER BY inode")
                                .setStartRow(i).setMaxRows(step);
                    } else {
                        _dh.setQuery("from " + clazz.getName() + " order by 1");
                    }

                    if (Identifier.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createIdentifierTransformer(dc.loadObjectResults()).asList();
                    } else if (Language.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createLanguageTransformer(dc.loadObjectResults()).asList();
                    } else if (Relationship.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createRelationshipTransformer(dc.loadObjectResults()).asList();
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createContentletVersionInfoTransformer(dc.loadObjectResults())
                                .asList();
                    } else if (Template.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createTemplateTransformer(dc.loadObjectResults())
                                .asList();
                    } else if (Contentlet.class.equals(clazz)) {

                        final ContentletJsonAPI contentletJsonAPI = APILocator.getContentletJsonAPI();
                        final List<Contentlet> contentlets = TransformerLocator
                                .createContentletTransformer(dc.loadObjectResults())
                                .asList();
                        _list = contentlets.stream().map(contentlet ->
                                contentletJsonAPI.toImmutable(contentlet)).collect(Collectors.toList());
                    } else if (Category.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createCategoryTransformer(dc.loadObjectResults())
                                .asList();
                    } else if(Folder.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createFolderTransformer(dc.loadObjectResults())
                                .asList();
                    } else if (Tree.class.equals(clazz)){
                        _list = TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
                    } else if (MultiTree.class.equals(clazz)){
                        _list = TransformerLocator.createMultiTreeTransformer(dc.loadObjectResults()).asList();
                    } else {
                        _list = _dh.list();
                    }
                    if(_list==null ||_list.isEmpty()) {
                        break;
                    }
                    if(_list.get(0) instanceof Comparable){
                        java.util.Collections.sort(_list);
                    }

                    _writing = new File(outputDirectory,  clazz.getName() + "_" + formatter.format(i) + ".json");

                    //We use a different serializer for ImmutableContentlets
                    if (Contentlet.class.equals(clazz)) {
                        ContentletJsonHelper.INSTANCE.get().writeContentletListToFile(_list, _writing);
                    } else{
                        BundlerUtil.objectToJSON(_list, _writing);
                    }

                    total = total + _list.size();

                    Thread.sleep(50);

                }
                Logger.info(this, "writing : " + total + " records for " + clazz.getName());
            }


            /* Run Liferay's Tables */
            /* Companies */
            _list = ImmutableList.of(APILocator.getCompanyAPI().getDefaultCompany());

            _writing = new File(outputDirectory,  Company.class.getName() + ".json");

            BundlerUtil.objectToJSON(_list, _writing);

            /* Users */
            _list = APILocator.getUserAPI().findAllUsers();
            _list.add(APILocator.getUserAPI().getDefaultUser());

            _writing = new File(outputDirectory,  User.class.getName() + ".json");
            BundlerUtil.objectToJSON(_list, _writing);

            /* users_roles */
            dc = new DotConnect();

            /* counter */
            dc.setSQL("select * from counter");
            _list = dc.getResults();

            _writing = new File(outputDirectory, "/Counter.json");
            BundlerUtil.objectToJSON(_list, _writing);


            /* image */
            _list = ImageLocalManagerUtil.getImages();

            /*
             * The changes in this part were made for Oracle databases. Oracle has problems when
             * getString() method is called on a LONG field on an Oracle database. Because of this,
             * the object is loaded from liferay and DotConnect is not used
             * http://jira.dotmarketing.net/browse/DOTCMS-1911
             */


            _writing = new File(outputDirectory, "/Image.json");
            BundlerUtil.objectToJSON(_list, _writing);

            /* portlet */

            /*
             * The changes in this part were made for Oracle databases. Oracle has problems when
             * getString() method is called on a LONG field on an Oracle database. Because of this,
             * the object is loaded from liferay and DotConnect is not used
             * http://jira.dotmarketing.net/browse/DOTCMS-1911
             */
            dc.setSQL("select * from portlet");
            _list = dc.getResults();
            _writing = new File(outputDirectory,"/Portlet.json");
            BundlerUtil.objectToJSON(_list, _writing);

            
            //backup content types
            File file = new File(outputDirectory,  "ContentTypes-" + ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION);
            new ContentTypeImportExportUtil().exportContentTypes(file);

            file = new File(outputDirectory, "/WorkflowSchemeImportExportObject.json");
            WorkflowImportExportUtil.getInstance().exportWorkflows(file);

            file = new File(outputDirectory, "/RuleImportExportObject.json");
            RulesImportExportUtil.getInstance().export(file);

            Logger.info(this, "Finished createJSONFiles into " + outputDirectory);
        } catch (Exception e) {
            Logger.error(this,e.getMessage(),e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Takes all the data structures in dotCMS that are required for the system to start up, and creates an in-memory
     * JSON representation of them. No assets are included in this process.
     * <p>For Hibernate classes and classes that map to database tables, the {@link DotSubmitter} API is used to
     * generate the required JSON files in parallel in order to speed up this process as much as possible.</p>
     *
     * @return The list of {@link FileEntry} objects containing the required information of all the required data
     * structures in dotCMS.
     */
    public List<FileEntry> getStarterDataAsJSON()  {
        Logger.info(this, "Converting all repository data to JSON data...");
        final List<FileEntry> starterFiles = new ArrayList<>();
        final Set<Class<?>> dotcmsTablesToDump = Try.of(() -> this.getTableSet()).getOrElse(new HashSet<>());
        final List<List<Class<?>>> dotcmsTablesSubset = this.splitTableList(dotcmsTablesToDump, 10);
        try {
            final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter("starter_export_submitter",
                    new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(10).maxPoolSize(40).queueCapacity(1000).build());
            final CompletionService<List<FileEntry>> completionService = new ExecutorCompletionService<>(dotSubmitter);
            final List<Future<List<FileEntry>>> futures = new ArrayList<>();

            for (final List<Class<?>> dotCMSTables : dotcmsTablesSubset) {
                final Future<List<FileEntry>> future = completionService.submit(() -> this.getStarterDataAsJSON(dotCMSTables));
                futures.add(future);
            }

            for (int i = 0; i < futures.size(); i++) {
                Logger.info(this, "Recovering result " + (i + 1) + " of " + futures.size());
                final List<FileEntry> resultList = completionService.take().get();
                starterFiles.addAll(resultList);
            }
            starterFiles.addAll(this.getAdditionalDataAsJSON());
            Logger.info(this, "Exportable JSON files have been generated successfully!");
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
        return starterFiles;
    }

    /**
     * Splits the Set of database tables into lists of a specific size. For instance, if there are 33 tables in the
     * system and a split value of 10 is specified, the resulting sub-sets will be 4 lists, such as: 3 lists of 10
     * items, and 1 list of 3 items.
     *
     * @param dbTables The Set of Hibernate or class-mapped database tables in dotCMS.
     * @param size The max size of every sub-set of the table list.
     * @return The list of sub-sets of database tables.
     */
    private List<List<Class<?>>> splitTableList(final Set<Class<?>> dbTables, final int size) {
        final List<Class<?>> fullList = new ArrayList<>(dbTables);
        final int partitionSize = Math.round((float) fullList.size() / size);
        return Lists.partition(fullList, partitionSize);
    }

    /**
     * Takes a list of database-mapped classes and generates a JSON with all of their records. There are a couple of
     * details to take into consideration:
     * <ul>
     *     <li>The provided table list may include both legacy Hibernate-mapped tables and tables accessed via
     *     {@link DotConnect}. So this must be considered when reading and loading the records.</li>
     *     <li>Every JSON dataset will be composed of 1,000 records by default, as specified by
     *     {@link #EXPORTED_RECORDS_PAGE_SIZE} . A maximum of 10 million records -- {@link #MAX_EXPORTED_RECORDS} -- per
     *     table will be exported.</li>
     *     <li>The JSON data is NEVER written to the File System.</li>
     * </ul>
     *
     * @param dbTablesAsClasses The list of classes that represent a dotCMS database table.
     *
     * @return The list of {@link FileEntry} objects containing the records of the specified dotCMS database tables.
     */
    @CloseDBIfOpened
    private List<FileEntry> getStarterDataAsJSON(final List<Class<?>> dbTablesAsClasses)  {
        final List<FileEntry> starterFiles = new ArrayList<>();
        try {
            HibernateUtil _dh;
            DotConnect dc;
            List _list;
            final ObjectMapper defaultObjectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            for (final Class<?> clazz : dbTablesAsClasses) {
                int i;
                int step = EXPORTED_RECORDS_PAGE_SIZE;
                int total = 0;
                for (i = 0; i < MAX_EXPORTED_RECORDS; i = i + step) {
                    _dh = new HibernateUtil(clazz);
                    _dh.setFirstResult(i);
                    _dh.setMaxResults(step);
                    dc = new DotConnect();
                    if (Tree.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM tree order by parent, child, relation_type").setStartRow(i).setMaxRows(step);
                    } else if (MultiTree.class.equals(clazz)) {
                        dc.setSQL(
                                "SELECT * FROM multi_tree order by parent1, parent2, child, relation_type").setStartRow(i).setMaxRows(step);
                    } else if (TagInode.class.equals(clazz)) {
                        _dh.setQuery("from " + clazz.getName() + " order by inode, tag_id");
                    } else if (Tag.class.equals(clazz)) {
                        _dh.setQuery("from " + clazz.getName() + " order by tag_id, tagname");
                    } else if (Identifier.class.equals(clazz)) {
                        dc.setSQL("select * from identifier order by parent_path, id").setStartRow(i).setMaxRows(step);
                    } else if (Language.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM language order by id").setStartRow(i).setMaxRows(step);
                    } else if (Relationship.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM relationship order by inode").setStartRow(i).setMaxRows(step);
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM contentlet_version_info ORDER BY identifier").setStartRow(i).setMaxRows(step);
                    } else if (Template.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM template ORDER BY inode").setStartRow(i).setMaxRows(step);
                    } else if (Contentlet.class.equals(clazz)) {
                        dc.setSQL("select contentlet.*, contentlet_1_.owner from contentlet join inode contentlet_1_ "
                                          + " on contentlet_1_.inode = contentlet.inode ORDER BY contentlet.inode").setStartRow(i).setMaxRows(step);
                    } else if (Category.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM category ORDER BY inode").setStartRow(i).setMaxRows(step);
                    } else if (Folder.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM folder ORDER BY inode").setStartRow(i).setMaxRows(step);
                    } else {
                        _dh.setQuery("from " + clazz.getName() + " order by 1");
                    }

                    if (Identifier.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createIdentifierTransformer(dc.loadObjectResults()).asList();
                    } else if (Language.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createLanguageTransformer(dc.loadObjectResults()).asList();
                    } else if (Relationship.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createRelationshipTransformer(dc.loadObjectResults()).asList();
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createContentletVersionInfoTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if (Template.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createTemplateTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if (Contentlet.class.equals(clazz)) {
                        final ContentletJsonAPI contentletJsonAPI = APILocator.getContentletJsonAPI();
                        final List<Contentlet> contentlets = TransformerLocator
                                                                     .createContentletTransformer(dc.loadObjectResults())
                                                                     .asList();
                        _list = contentlets.stream().map(contentletJsonAPI::toImmutable).collect(Collectors.toList());
                    } else if (Category.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createCategoryTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if(Folder.class.equals(clazz)) {
                        _list = TransformerLocator
                                        .createFolderTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if (Tree.class.equals(clazz)){
                        _list = TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
                    } else if (MultiTree.class.equals(clazz)){
                        _list = TransformerLocator.createMultiTreeTransformer(dc.loadObjectResults()).asList();
                    } else {
                        _list = _dh.list();
                    }
                    if (!UtilMethods.isSet(_list)) {
                        break;
                    }
                    final String zippedFileName =
                            clazz.getName() + StringPool.UNDERLINE + DEFAULT_JSON_FILE_DATA_FORMAT.format(i) + JSON_FILE_EXT;
                    final String contentAsJson = defaultObjectMapper.writeValueAsString(_list);
                    final FileEntry jsonFileEntry = new FileEntry(zippedFileName, contentAsJson);
                    starterFiles.add(jsonFileEntry);
                    total += _list.size();
                }
                Logger.info(this, total + " records were generated for " + clazz.getName());
            }
            Logger.info(this, "Exportable JSON files have been generated successfully!");
        } catch (final Exception e) {
            Logger.error(this,e.getMessage(),e);
            throw new DotRuntimeException(e);
        }
        return starterFiles;
    }

    /**
     * Certain dotCMS database tables are not mapped to Java classes, but are required for dotCMS to start up
     * correctly. This method handles the generation of JSON data for them.
     *
     * @return The list of {@link FileEntry} objects containing the records of the "special" dotCMS database tables.
     */
    @CloseDBIfOpened
    private List<FileEntry> getAdditionalDataAsJSON()  {
        final List<FileEntry> starterFiles = new ArrayList<>();
        final ObjectMapper defaultObjectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        try {
            List _list = ImmutableList.of(APILocator.getCompanyAPI().getDefaultCompany());
            String contentAsJson = defaultObjectMapper.writeValueAsString(_list);
            starterFiles.add(new FileEntry(Company.class.getName() + JSON_FILE_EXT, contentAsJson));

            // Users
            _list = APILocator.getUserAPI().findAllUsers();
            _list.add(APILocator.getUserAPI().getDefaultUser());
            contentAsJson = defaultObjectMapper.writeValueAsString(_list);
            starterFiles.add(new FileEntry(User.class.getName() + JSON_FILE_EXT, contentAsJson));

            final DotConnect dc = new DotConnect();

            dc.setSQL("select * from counter");
            _list = dc.loadResults();
            contentAsJson = defaultObjectMapper.writeValueAsString(_list);
            starterFiles.add(new FileEntry("Counter" + JSON_FILE_EXT, contentAsJson));

            _list = ImageLocalManagerUtil.getImages();
            contentAsJson = defaultObjectMapper.writeValueAsString(_list);
            starterFiles.add(new FileEntry("Image" + JSON_FILE_EXT, contentAsJson));

            dc.setSQL("select * from portlet");
            _list = dc.loadResults();
            contentAsJson = defaultObjectMapper.writeValueAsString(_list);
            starterFiles.add(new FileEntry("Portlet" + JSON_FILE_EXT, contentAsJson));

            final List<FileEntry> jsonFileEntries = new ContentTypeImportExportUtil().exportContentTypes();
            starterFiles.addAll(jsonFileEntries);

            contentAsJson = WorkflowImportExportUtil.getInstance().exportWorkflowsToJson();
            starterFiles.add(new FileEntry("WorkflowSchemeImportExportObject" + JSON_FILE_EXT, contentAsJson));

            contentAsJson = RulesImportExportUtil.getInstance().exportToJson();
            starterFiles.add(new FileEntry("RuleImportExportObject" + JSON_FILE_EXT, contentAsJson));

            Logger.info(this, "Additional exportable JSON files have been generated successfully!");
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
        return starterFiles;
    }

    private void moveAssetsToBackupDir() throws IOException{
        String assetDir = ConfigUtils.getAbsoluteAssetsRootPath();

        Logger.info(this, "Moving assets to back up directory: " + outputDirectory);

        FileUtil.copyDirectory(assetDir, outputDirectory + File.separator + "assets", new AssetFileNameFilter());
    }

    /**
     * Traverses the complete list of assets in the current dotCMS instance in order to retrieve their respective file
     * reference. This will allow dotCMS to add their contents to the final Starter ZIP file without copying them to
     * any backup or temporary location.
     *
     * @return The file references to all assets in the dotCMS repository.
     */
    private List<FileEntry> getAssets() {
        final String assetsRootPath = ConfigUtils.getAbsoluteAssetsRootPath();
        final List<FileEntry> assetData = new ArrayList<>();
        final File source = new File(assetsRootPath);
        Logger.info(this, "Adding Assets into the starter...");

        FileUtil.listFilesRecursively(source, new AssetFileNameFilter())
                .stream()
                .filter(File::isFile)
                .forEach(file -> {
                    String filePath = file.getPath().replace(assetsRootPath, "/assets/");
                    final FileEntry entry = new FileEntry(filePath, file);
                    assetData.add(entry);
                });

        return assetData;
    }

    /**
     *
     * @param source
     * @param assetData
     */
    private void traverseAssetsFolder(final File source, final List<FileEntry> assetData) {
        final FileFilter filter = new AssetFileNameFilter();
        final String assetsRootPath = ConfigUtils.getAbsoluteAssetsRootPath();
        if (source.exists() && source.isDirectory()) {
            final File[] fileArray = source.listFiles(filter);
            if (null == fileArray) {
                Logger.warn(FileUtil.class,String.format("No files were returned from [%s] applying filter [%s]. ", source, filter));
                return;
            }
            for (final File file : fileArray) {
                if (file.isDirectory()) {
                    this.traverseAssetsFolder(file, assetData);
                } else {
                    String filePath = file.getPath().replace(assetsRootPath, "/assets/");
                    final FileEntry entry = new FileEntry(filePath, file);
                    assetData.add(entry);
                }
            }
        }
    }

    /**
     * Manipulates contents of a directory and put them through a given output stream not without first compressing them
     * to a generated zip file.
     *
     * @param output given output stream
     * @param withAssets flag telling to include assets or not
     * @param download flag telling to download as a file or as a stream
     * @return file zip file
     * @throws IOException
     */
    public Optional<File> zipStarter(final OutputStream output, final boolean withAssets, final boolean download)
            throws IOException {
        final File outputDir = withAssets
                ? new ExportStarterUtil().createStarterWithAssets()
                : new ExportStarterUtil().createStarterData();
        final File zipFile = new File(
                outputDir + UtilMethods
                        .dateToJDBC(new Date())
                        .replace(':', '-')
                        .replace(' ', '_') + ".zip");
        Logger.info(this, "Zipping up to file:" + zipFile.getAbsolutePath());

        try (final OutputStream outStream = download
                ? new TeeOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile.toPath())), output)
                : new BufferedOutputStream(Files.newOutputStream(zipFile.toPath()));
                final ZipOutputStream zipOutput = new ZipOutputStream(outStream)) {
            ZipUtil.zipDirectory(outputDir.getAbsolutePath(), zipOutput);
        }

        Logger.info(this, "Wrote starter to : " + zipFile.toPath());
        new TrashUtils().moveFileToTrash(outputDir, "starter");

        final File newLocation = new File(ConfigUtils.getAbsoluteAssetsRootPath()
                + separator
                + "bundles"
                + separator
                + zipFile.getName());
        FileUtils.moveFile(zipFile, newLocation);

        return Optional.of(newLocation);
    }

    /**
     * Generates all the contents that go into the dotCMS Starter zip file. It's very important to point out that <b>no
     * temporary files are created, nor any piece of data is written to the FS at any point.</b>
     * <p>The resulting ZIP file is fully created in memory and, as soon as the first entry is added, it is directly
     * streamed to the {@link OutputStream} for users to be able to download it even before it is 100% ready.</p>
     *
     * @param output The {@link OutputStream} instance.
     * @param includeAssets If the Starter file must contain all dotCMS assets as well, set this to {@code true}.
     */
    public void streamZipStarter(final OutputStream output, final boolean includeAssets) {
        final List<FileEntry> starterData = this.getStarterDataAsJSON();
        if (includeAssets) {
            starterData.addAll(this.getAssets());
        }
        this.createInMemoryZip(starterData, output);
    }

    /**
     * Creates an in-memory ZIP file with the provided list of contents. The output object provided by the
     * {@link StreamingOutput} is passed down to the ZIP generation process so that the actual file download as soon as
     * the first entry is added to it.
     *
     * @param output      The {@link OutputStream} instance that will be sent back in the response.
     * @param starterData The complete list of dotCMS data structures -- and optionally assets -- that must be added
     *                    to the in-memory ZIP file.
     */
    private void createInMemoryZip(final List<FileEntry> starterData, final OutputStream output) {
        try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output))) {
            for (final FileEntry entry : starterData) {
                Logger.info(this, "Adding file ." + entry.fileName());
                ZipUtil.addZipEntry(zip, entry.fileName(), entry.getInputStream(), true);
                //Thread.sleep(500);
            }
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    /**
     * Generates the name of the dotCMS Starter zip file. By default, the zip file name has the following format:
     * <pre>
     *     backup_%s-%s.zip
     * </pre>
     * It's made up of:
     * <ul>
     *     <li>The {@code "backup_"} prefix.</li>
     *     <li>The current date and time in milliseconds.</li>
     *     <li>A dash.</li>
     *     <li>The current date in JDBC format -- replacing colons with dashes and blank spaces with underlines.</li>
     *     <li>The file extension: {@code ".zip"}</li>
     * </ul>
     * If you need to pseudo-customize the starter's file name by overriding the following configuration property:
     * {@code starter.download.filename}
     *
     * @return The dotCMS Starter's file name.
     */
    public String generateStarterFileName() {
        return String.format(STARTER_FILE_NAME, System.currentTimeMillis(), UtilMethods.dateToJDBC(new Date()))
                                  .replace(StringPool.COLON, StringPool.DASH)
                                  .replace(StringPool.SPACE, StringPool.UNDERLINE);
    }

    /**
     *
     */
    public static class FileEntry implements Serializable {

        private final String fileName;
        private final byte[] content;
        private final File file;

        public FileEntry(final String fileName, final String content) {
            this.fileName = fileName;
            this.content = content.getBytes(StandardCharsets.UTF_8);
            this.file = null;
        }

        public FileEntry(final String fileName, final File file) {
            this.fileName = fileName;
            this.content = null;
            this.file = file;
        }

        public String fileName() {
            return fileName;
        }

        public byte[] content() {
            return content;
        }

        public InputStream getInputStream() throws IOException {
            if (null != content) {
                return new ByteArrayInputStream(this.content);
            }
            return Files.newInputStream(file.toPath());
        }

        @Override
        public String toString() {
            return "JSONFileEntry{" + "fileName='" + fileName + '\'' + '}';
        }

    }

}
