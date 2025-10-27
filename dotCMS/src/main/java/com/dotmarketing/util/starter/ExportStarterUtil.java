package com.dotmarketing.util.starter;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
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
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
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
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * Utility class used to generate a bundle with assets and data (starter file). The bundle will be a compressed file
 * with assets and data modeled in JSON files.
 *
 * @author nollymarlonga
 * @author Jose Castro
 */
public class ExportStarterUtil {

    private static final String STARTER_FILE_NAME_PROP = "export.starter.filename";
    private static final String ASSETS_FILE_NAME_PROP = "export.assets.filename";
    private static final String STARTER_GENERATION_SPLIT_VALUE_PROP = "export.starter.generation.tables.splitvalue";
    private static final String STARTER_GENERATION_POOL_SIZE_PROP = "export.starter.generation.poolsize";
    private static final String STARTER_GENERATION_MAX_POOL_SIZE_PROP = "export.starter.generation.maxpoolsize";
    private static final String STARTER_GENERATION_QUEUE_CAPACITY_PROP = "export.starter.generation.queuecapacity";
    private static final String STARTER_GENERATION_ASSETS_FOLDER_PROP = "export.starter.generation.assets";

    private static final NumberFormat DEFAULT_JSON_FILE_DATA_FORMAT = new DecimalFormat("0000000000");

    private static final Lazy<String> STARTER_FILE_NAME =
            Lazy.of(() -> Config.getStringProperty(STARTER_FILE_NAME_PROP, "backup_%s-%s.zip"));
    private static final Lazy<String> ASSETS_FILE_NAME =
            Lazy.of(() -> Config.getStringProperty(ASSETS_FILE_NAME_PROP, "%s_assets_%s.zip"));
    private static final Lazy<Integer> SPLIT_VALUE =
            Lazy.of(() -> Config.getIntProperty(STARTER_GENERATION_SPLIT_VALUE_PROP, 10));
    private static final Lazy<Integer> POOL_SIZE =
            Lazy.of(() -> Config.getIntProperty(STARTER_GENERATION_POOL_SIZE_PROP, 10));
    private static final Lazy<Integer> MAX_POOL_SIZE =
            Lazy.of(() -> Config.getIntProperty(STARTER_GENERATION_MAX_POOL_SIZE_PROP, 40));
    private static final Lazy<Integer> QUEUE_CAPACITY =
            Lazy.of(() -> Config.getIntProperty(STARTER_GENERATION_QUEUE_CAPACITY_PROP, 1000));
    private static final Lazy<String> ZIP_FILE_ASSETS_FOLDER =
            Lazy.of(() -> Config.getStringProperty(STARTER_GENERATION_ASSETS_FOLDER_PROP, "assets"));

    private static final String JSON_FILE_EXT = ".json";

    /** Only 10,000,000 records of any given data type wil be exported. */
    private static final int MAX_EXPORTED_RECORDS = 10000000;
    private static final int EXPORTED_RECORDS_PAGE_SIZE = 1000;

    public File createStarterWithAssets() {
        throw new UnsupportedOperationException("This legacy Starter generation process is not supported anymore");
    }

    public File createStarterData() {
        throw new UnsupportedOperationException("This legacy Starter generation process is not supported anymore");
    }

    /**
     * Returns a list with all the dotCMS database tables whose contents must be added to the Starter bundle.
     *
     * @return The list of exportable dotCMS database tables.
     *
     * @throws HibernateException An error occurred when retrieving Hibernate-managed tables.
     */
    private Set<Class<?>> getTableSet() throws HibernateException {
        final Set<Class<?>> dbTables = new HashSet<>();
        dbTables.add(Identifier.class);
        dbTables.add(Language.class);
        dbTables.add(Relationship.class);
        dbTables.add(ContentletVersionInfo.class);
        dbTables.add(Template.class);
        dbTables.add(Contentlet.class);
        dbTables.add(Category.class);
        dbTables.add(Tree.class);
        dbTables.add(MultiTree.class);
        dbTables.add(Folder.class);
        dbTables.add(Company.class);
        dbTables.add(User.class);

        //end classes no longer mapped with Hibernate
        dbTables.addAll(HibernateUtil.getSession().getSessionFactory().getAllClassMetadata().keySet());

        dbTables.removeIf(c->c.equals(Inode.class));
        dbTables.removeIf(c->c.equals(Clickstream.class));
        dbTables.removeIf(c->c.equals(ClickstreamRequest.class));
        dbTables.removeIf(c->c.equals(Clickstream404.class));
        dbTables.removeIf(c->c.equals(Structure.class));
        dbTables.removeIf(c->c.equals(Field.class));
        dbTables.removeIf(c->c.equals(WorkflowHistory.class));
        dbTables.removeIf(c->c.equals(PermissionReference.class));
        dbTables.removeIf(t->t.getName().startsWith("Dashboard"));
        dbTables.removeIf(t->t.getName().contains("HBM"));

        return dbTables;
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
    public void getStarterDataAsJSON(final ZipOutputStream zip)  {
        Logger.info(this, "Converting all repository data to JSON data...");
        final Set<Class<?>> dotcmsTables = Try.of(this::getTableSet).getOrElse(new HashSet<>());
        final List<List<Class<?>>> dotcmsTablesSubset = this.splitTableList(dotcmsTables, SPLIT_VALUE.get());
        try {
            final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter(
                    "starter_export_submitter",
                    new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(POOL_SIZE.get()).maxPoolSize(MAX_POOL_SIZE.get()).queueCapacity(QUEUE_CAPACITY.get()).build());
            final CompletionService<List<FileEntry>> completionService = new ExecutorCompletionService<>(dotSubmitter);
            final List<Future<List<FileEntry>>> futures = new ArrayList<>();
            Logger.debug(this, String.format("Generating Starter Data JSON files for %d database tables", dotcmsTables.size()));
            for (final List<Class<?>> dotCMSTables : dotcmsTablesSubset) {
                final Future<List<FileEntry>> future = completionService.submit(() -> this.getStarterDataAsJSON(dotCMSTables));
                futures.add(future);
            }
            this.streamFilesToZip(completionService, futures, zip);
            Logger.debug(this, "Exportable JSON files have been generated successfully!");
        } catch (final InterruptedException | ExecutionException e) {
            Logger.error(this, e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Streams the generated data files into the resulting Starter ZIP file. This process is improved by invoking such
     * generation processes in parallel in order to get the results as fast as possible.
     *
     * @param completionService The {@link CompletionService} for retrieving the results of the asynchronous tasks.
     * @param futures           The list of {@link Future} objects to keep track of all the created tasks.
     * @param zip               The {@link ZipOutputStream} that will take the resulting files.
     *
     * @throws InterruptedException An error occurred when accessing the results of a specific Future.
     * @throws ExecutionException   An error occurred when accessing the results of a specific Future.
     */
    private void streamFilesToZip(final CompletionService<List<FileEntry>> completionService,
                                  final List<Future<List<FileEntry>>> futures, final ZipOutputStream zip) throws InterruptedException, ExecutionException {
        Logger.info(this, "Streaming all JSON data files to starter file...");
        for (int i = 0; i < futures.size(); i++) {
            Logger.debug(this, "Recovering result " + (i + 1) + " of " + futures.size());
            this.addFilesToZip(completionService.take().get(), zip);
        }
        this.addFilesToZip(this.getAdditionalDataAsJSON(), zip);
    }

    /**
     * Adds the specified file entry to the resulting Starter ZIP file.
     *
     * @param fileEntry The {@link FileEntry} containing the information of the file being added to the ZIP.
     * @param zip       The {@link ZipOutputStream} that will take the entry.
     */
    private void addFileToZip(final FileEntry fileEntry, final ZipOutputStream zip) {
        this.addFilesToZip(List.of(fileEntry), zip);
    }

    /**
     * Adds the specified list of file entries to the resulting Starter ZIP file.
     *
     * @param fileEntries The list of {@link FileEntry} objects containing the information of the files being added
     *                    to the ZIP.
     * @param zip         The {@link ZipOutputStream} that will take the entry.
     */
    private void addFilesToZip(final List<FileEntry> fileEntries, final ZipOutputStream zip) {
        fileEntries.stream().forEach(entry -> {

            try {
                ZipUtil.addZipEntry(zip, entry.fileName(), entry.getInputStream(), true);
            } catch (final IOException e) {
                Logger.error(this, String.format("An error occurred when streaming file '%s' into starter file: " +
                                                         "%s", entry.fileName(), e.getMessage()), e);
                throw new DotRuntimeException(e);
            }

        });
    }

    /**
     * Splits the Set of database tables into lists of a specific size. For instance, if there are
     * 35 tables in the system and a split value of 10 is specified, the max partition size will be
     * 4. That is, there will be 8 lists of 4 items, and 1 list of 3 items to accommodate all 35
     * tables.
     *
     * @param dbTables The Set of Hibernate or class-mapped database tables in dotCMS.
     * @param size     The max size of every sub-set of the table list.
     *
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
     * @param dbTablesAsClasses The list of classes that represent dotCMS database tables.
     *
     * @return The list of {@link FileEntry} objects containing the records of the specified dotCMS database tables.
     */
    @CloseDBIfOpened
    private List<FileEntry> getStarterDataAsJSON(final List<Class<?>> dbTablesAsClasses)  {
        final List<FileEntry> starterFiles = new ArrayList<>();
        HibernateUtil hibernateUtil;
        DotConnect dc;
        List<?> resultList;
        final ObjectMapper defaultObjectMapper = ContentletJsonHelper.INSTANCE.get().objectMapper();
        try {
            Logger.debug(this, String.format("Retrieving data from tables: %s", dbTablesAsClasses));
            for (final Class<?> clazz : dbTablesAsClasses) {
                int i;
                int step = EXPORTED_RECORDS_PAGE_SIZE;
                int total = 0;
                for (i = 0; i < MAX_EXPORTED_RECORDS; i = i + step) {
                    hibernateUtil = new HibernateUtil(clazz);
                    hibernateUtil.setFirstResult(i);
                    hibernateUtil.setMaxResults(step);
                    dc = new DotConnect();
                    if (Tree.class.equals(clazz)) {
                        dc.setSQL("SELECT * FROM tree order by parent, child, relation_type").setStartRow(i).setMaxRows(step);
                    } else if (MultiTree.class.equals(clazz)) {
                        dc.setSQL(
                                "SELECT * FROM multi_tree order by parent1, parent2, child, relation_type").setStartRow(i).setMaxRows(step);
                    } else if (TagInode.class.equals(clazz)) {
                        hibernateUtil.setQuery("FROM " + clazz.getName() + " ORDER BY inode, tag_id");
                    } else if (Tag.class.equals(clazz)) {
                        hibernateUtil.setQuery("FROM " + clazz.getName() + " ORDER BY tag_id, tagname");
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
                        hibernateUtil.setQuery("FROM " + clazz.getName() + " ORDER BY 1");
                    }

                    if (Identifier.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createIdentifierTransformer(dc.loadObjectResults()).asList();
                    } else if (Language.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createLanguageTransformer(dc.loadObjectResults()).asList();
                    } else if (Relationship.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createRelationshipTransformer(dc.loadObjectResults()).asList();
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createContentletVersionInfoTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if (Template.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createTemplateTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if (Contentlet.class.equals(clazz)) {
                        final ContentletJsonAPI contentletJsonAPI = APILocator.getContentletJsonAPI();
                        final List<Contentlet> contentlets = TransformerLocator
                                                                     .createContentletTransformer(dc.loadObjectResults())
                                                                     .asList();
                        resultList = contentlets.stream().map(contentletJsonAPI::toImmutable).collect(Collectors.toList());
                    } else if (Category.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createCategoryTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if(Folder.class.equals(clazz)) {
                        resultList = TransformerLocator
                                        .createFolderTransformer(dc.loadObjectResults())
                                        .asList();
                    } else if (Tree.class.equals(clazz)){
                        resultList = TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
                    } else if (MultiTree.class.equals(clazz)){
                        resultList = TransformerLocator.createMultiTreeTransformer(dc.loadObjectResults()).asList();
                    } else {
                        resultList = hibernateUtil.list();
                    }
                    if (!UtilMethods.isSet(resultList)) {
                        break;
                    }
                    final String jsonFileName =
                            clazz.getName() + StringPool.UNDERLINE + DEFAULT_JSON_FILE_DATA_FORMAT.format(i) + JSON_FILE_EXT;
                    final String contentAsJson = defaultObjectMapper.writeValueAsString(resultList);
                    Logger.debug(this, String.format("-> File: %s", jsonFileName));
                    final FileEntry jsonFileEntry = new FileEntry(jsonFileName, contentAsJson);
                    starterFiles.add(jsonFileEntry);
                    total += resultList.size();
                }
                Logger.debug(this, String.format("-> %d records were generated for class table '%s'", total, clazz.getName()));
            }
            Logger.debug(this, "Exportable JSON files have been generated successfully!");
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
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
            List resultList = List.of(APILocator.getCompanyAPI().getDefaultCompany());
            String contentAsJson = defaultObjectMapper.writeValueAsString(resultList);
            starterFiles.add(new FileEntry(Company.class.getName() + JSON_FILE_EXT, contentAsJson));

            resultList = APILocator.getUserAPI().findAllUsers();
            resultList.add(APILocator.getUserAPI().getDefaultUser());
            contentAsJson = defaultObjectMapper.writeValueAsString(resultList);
            starterFiles.add(new FileEntry(User.class.getName() + JSON_FILE_EXT, contentAsJson));

            final DotConnect dc = new DotConnect();

            dc.setSQL("select * from counter");
            resultList = dc.loadResults();
            contentAsJson = defaultObjectMapper.writeValueAsString(resultList);
            starterFiles.add(new FileEntry("Counter" + JSON_FILE_EXT, contentAsJson));

            resultList = ImageLocalManagerUtil.getImages();
            contentAsJson = defaultObjectMapper.writeValueAsString(resultList);
            starterFiles.add(new FileEntry("Image" + JSON_FILE_EXT, contentAsJson));

            dc.setSQL("select * from portlet");
            resultList = dc.loadResults();
            contentAsJson = defaultObjectMapper.writeValueAsString(resultList);
            starterFiles.add(new FileEntry("Portlet" + JSON_FILE_EXT, contentAsJson));

            final List<FileEntry> jsonFileEntries = new ContentTypeImportExportUtil().exportContentTypes();
            starterFiles.addAll(jsonFileEntries);

            contentAsJson = WorkflowImportExportUtil.getInstance().exportWorkflowsToJson();
            starterFiles.add(new FileEntry("WorkflowSchemeImportExportObject" + JSON_FILE_EXT, contentAsJson));

            contentAsJson = RulesImportExportUtil.getInstance().exportToJson();
            starterFiles.add(new FileEntry("RuleImportExportObject" + JSON_FILE_EXT, contentAsJson));

            Logger.debug(this, String.format("Additional exportable entries added = %d", starterFiles.size()));
            Logger.debug(this, "Additional exportable JSON files have been generated successfully!");
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
        return starterFiles;
    }

    final String[] assetDirs =new String[]{"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
    /**
     * Traverses the complete list of assets in the current dotCMS instance in order to retrieve their respective file
     * reference. This will allow dotCMS to add their contents to the final Starter ZIP file without copying them to
     * any backup or temporary location.
     *
     * @param zip        The {@link ZipOutputStream} which will receive all the streamed data.
     * @param fileFilter The {@link FileFilter} being used to get the appropriate asset data.
     */
    private void getAssets(final ZipOutputStream zip, final FileFilter fileFilter) {

        Logger.info(this, "Adding all Assets into compressed file:");
        // add all our b-tree directories
        for (int i = 0; i < assetDirs.length; i++) {
            for (int j = 0; j < assetDirs.length; j++) {
                final File source = Paths.get(ConfigUtils.getAssetPath() , assetDirs[i] , assetDirs[j]).toFile();
                addFolderToZip(zip, fileFilter, source);
            }
        }
        // finally add the messages folder
        final File source = Paths.get(ConfigUtils.getAssetPath() , "messages").toFile();
        addFolderToZip(zip, fileFilter, source);
    }

    private void addFolderToZip(final ZipOutputStream zip, final FileFilter fileFilter, final File source){
        if(!source.exists() || ! source.isDirectory() || Files.isSymbolicLink(source.toPath())){
            Logger.warn(this, String.format("-> Not a directory: %s", source));
            return;
        }
        FileUtil.listFilesRecursively(source, fileFilter).stream().filter(File::isFile).forEach(file -> {
            try {
                Path sourcePath = Paths.get(ConfigUtils.getAssetPath()).normalize();
                Path currentFilePath = file.toPath().normalize();
                Path targetFolderPath = Paths.get(ZIP_FILE_ASSETS_FOLDER.get()).normalize();

                // Get relative path from source to current file
                Path relativePath = sourcePath.relativize(currentFilePath);

                // Construct the final path by combining target folder with relative path
                Path finalPath = targetFolderPath.resolve(relativePath);

                // Convert to string with forward slashes for ZIP compatibility
                String filePath = finalPath.toString().replace('\\', '/');

            Logger.debug(this, String.format("-> File path: %s", filePath));
            final FileEntry entry = new FileEntry(filePath, file);
            this.addFileToZip(entry, zip);

        } catch (Exception e) {
            Logger.error(this, String.format("Error processing file path for %s: %s",
                    file.getPath(), e.getMessage()));
        }
        });


    }




    /**
     * Generates all the contents that go into the compressed dotCMS Starter file. It's very
     * important to point out that <b>no temporary files are created, and no data is written to the
     * file system at any point.</b>
     * <p>The resulting compressed file is directly streamed to the {@link OutputStream} for users
     * to be able to download it even before it is 100% ready. Users have the option to choose
     * whether they want all versions of the assets to be part of the starter ot not. This is useful
     * in cases where the starter's size must be kept as low as possible, or if older asset versions
     * are not needed at all.</p>
     *
     * @param output           The {@link OutputStream} instance.
     * @param includeAssets    If the Starter file must contain all dotCMS assets as well, set this
     *                         to {@code true}.
     * @param includeOldAssets If absolutely all versions of the assets must be included in the
     *                         compressed file, set this to {@code true}.
     */
    public void streamCompressedStarter(final OutputStream output, final boolean includeAssets, final boolean includeOldAssets) {
        this.streamCompressedData(output, true, includeAssets, false, includeOldAssets);
    }

    /**
     * Generates a compressed file with all the assets living in the current dotCMS repository. It's very important to
     * point out that <b>no temporary files are created, and no data is written to the file system at any point.</b>
     * <p>The resulting compressed file is directly streamed to the {@link OutputStream} for users to be able to
     * download it even before it is 100% ready.</p>
     *
     * @param output           The {@link OutputStream} instance.
     * @param includeOldAssets If absolutely all versions of the assets must be included in the compressed file, set
     *                         this to {@code true}.
     */
    public void streamCompressedAssets(final OutputStream output, boolean includeOldAssets) {
        this.streamCompressedData(output, false, false, true, includeOldAssets);
    }

    /**
     * Generates a compressed file with specific dotCMS data.</p>
     *
     * @param output                      The {@link OutputStream} instance.
     * @param includeStarterData          If the dotCMS data must be added to the compressed file, set this to
     *                                    {@code true}.
     * @param includeStarterDataAndAssets If the Starter file must contain all dotCMS assets as well, set this to
     *                                    {@code true}.
     * @param includeAssetsOnly           If only the assets must be included in the compressed file, set this to
     *                                    {@code true}.
     * @param includeOldAssets            If absolutely all versions of the assets must be included in the compressed
     *                                    file, set this to {@code true}.
     */
    private void streamCompressedData(final OutputStream output, boolean includeStarterData,
                                     final boolean includeStarterDataAndAssets, boolean includeAssetsOnly, boolean includeOldAssets) {




        try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output))) {
            if (includeStarterData) {
                Logger.debug(this, "Including Starter Data");
                this.getStarterDataAsJSON(zip);
            }
            final AssetFileNameFilter fileFilter = includeOldAssets ? new AssetFileNameFilter() : new AssetFileNameFilter(getLiveWorkingBloomFilter());
            if (includeStarterDataAndAssets) {
                Logger.debug(this, "Including Starter Data and Assets");
                this.getAssets(zip, fileFilter);
            }
            if (includeAssetsOnly) {
                Logger.debug(this, "Including Assets only");
                this.getAssets(zip, fileFilter);
            }
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when generating compressed data file with [ " +
                    "includeStarterData = %s, includeStarterDataAndAssets = %s, includeAssetsOnly = %s, " +
                    "includeOldAssets = %s ] : %s", includeStarterData, includeStarterDataAndAssets,
                    includeAssetsOnly, includeOldAssets, e.getMessage()), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Generates the name of the compressed dotCMS Starter file. By default, such a file name has the following format:
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
     * If you need to customize the starter's file name by overriding the following configuration property:
     * {@link #STARTER_FILE_NAME_PROP}
     *
     * @return The dotCMS Starter's file name.
     */
    public String resolveStarterFileName() {
        return String.format(STARTER_FILE_NAME.get(), System.currentTimeMillis(),
                DateUtil.EXPORTING_DATE_FORMAT.format(new Date()));
    }

    /**
     * Generates the name of the compressed dotCMS assets file. By default, such a file name has the following format:
     * <pre>
     *     %s_assets_%s.zip
     * </pre>
     * It's made up of:
     * <ul>
     *     <li>The name of the Default Site as prefix, or "dotcms" if not available.</li>
     *     <li>The current date in JDBC format -- replacing colons with dashes and blank spaces with underlines.</li>
     *     <li>The file extension: {@code ".zip"}</li>
     * </ul>
     * If you need to customize the assets' file name by overriding the following configuration property:
     * {@link #ASSETS_FILE_NAME_PROP}
     *
     * @return The dotCMS Starter's file name.
     */
    public String resolveAssetsFileName() {
        final String siteName =
                Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname()).getOrElse("dotcms");
        return String.format(ASSETS_FILE_NAME.get(), StringUtils.sanitizeFileName(siteName),
                DateUtil.EXPORTING_DATE_FORMAT.format(new Date()));
    }

    /**
     * Represents a file entry in the generated compressed file, which can be in the form of a byte array or a file
     * reference.
     */
    public static class FileEntry implements Serializable {

        private final String fileName;
        private final byte[] content;
        private final File file;

        /**
         * Creates a new ZIP File Entry for the specified file in the form of a String.
         *
         * @param fileName The name of the file.
         * @param content  The contents of such a file.
         */
        public FileEntry(final String fileName, final String content) {
            this.fileName = fileName;
            this.content = content.getBytes(StandardCharsets.UTF_8);
            this.file = null;
        }

        /**
         * Creates a new ZIP File Entry for the specified file in the form of a File Reference.
         *
         * @param fileName The name of the file.
         * @param file     The contents of such a file.
         */
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

        /**
         * Returns the contents of this file as an {@link InputStream} object.
         *
         * @return The contents of the file as an Input Stream.
         *
         * @throws IOException An error occurred when the file's Input Stream -- if applicable -- was retrieved.
         */
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

    private static final String SELECT_ALL_LIVE_WORKING_CONTENTLETS=
            "select " +
            "  live_inode as inode " +
            "from " +
            "  contentlet_version_info " +
            "where " +
            "  live_inode is not null " +
            "UNION " +
            "select " +
            "  working_inode as inode " +
            "from " +
            "  contentlet_version_info " +
            "where " +
            "  working_inode <> live_inode " +
            "and " +
            "  working_inode is not null " +
            "and " +
            "  deleted = false";

    private static final String SELECT_ALL_LIVE_WORKING_CONTENTLETS_COUNT=
            "select " +
            "count(*) as my_count " +
            "from (" + SELECT_ALL_LIVE_WORKING_CONTENTLETS + ") testing ";

    @CloseDBIfOpened
    BloomFilter<String> getLiveWorkingBloomFilter()  {
        long contentCount = new DotConnect()
                .setSQL(SELECT_ALL_LIVE_WORKING_CONTENTLETS_COUNT)
                .getInt("my_count");

        Logger.info(this.getClass(), "Creating BloomFilter with " + contentCount + " expected size");

        final BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                contentCount,
                0.01);

        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            final PreparedStatement statement = conn.prepareStatement(SELECT_ALL_LIVE_WORKING_CONTENTLETS)){
            statement.setFetchSize(5000);
            try (final ResultSet rs = statement.executeQuery()) {
                int i = 0;
                while (rs.next()) {
                    bloomFilter.put(rs.getString("inode"));
                    i++;
                }
                Logger.info(this.getClass(), "Added " + i + " contentlets to the BloomFilter");
            }
        } catch (final SQLException e) {
            throw new DotRuntimeException(e);
        }

        return bloomFilter;
    }

}
