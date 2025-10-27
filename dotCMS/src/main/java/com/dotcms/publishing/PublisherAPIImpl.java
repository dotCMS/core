package com.dotcms.publishing;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.config.DotInitializer;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.pushpublish.PushPublishEndEvent;
import com.dotcms.system.event.local.type.pushpublish.PushPublishStartEvent;
import com.dotcms.system.event.local.type.staticpublish.StaticPublishEndEvent;
import com.dotcms.system.event.local.type.staticpublish.StaticPublishStartEvent;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;

/**
 * Implementation class for the {@link PublisherAPI}.
 * <p>Every specific Bundle {@link Publisher} has its own specific list of data Bundlers -- {@link IBundler} -- which
 * determine what and how specific pieces of information are added to a given Bundle. A MANIFEST file is added to all
 * bundles in order to indicate why specific data was included or excluded in them.</p>
 *
 * @author Jason Tesser
 * @since Mar 23rd, 2012
 */
public class PublisherAPIImpl implements PublisherAPI, DotInitializer {

    private final PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();
    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
    private List<FilterDescriptor> filterList = new CopyOnWriteArrayList<>();
    /** Path where the YAML files are stored */
    private final Lazy<Path> PUBLISHING_FILTERS_FOLDER = Lazy.of(() -> Path.of(APILocator.getFileAssetAPI().getRealAssetsRootPath(), "server" , "publishing-filters" ));

    @Override
    public final PublishStatus publish ( PublisherConfig config) throws DotPublishingException {

        return publish( config, new PublishStatus() );
    }

    @CloseDBIfOpened
    @Override
    public final PublishStatus publish ( PublisherConfig config, PublishStatus status) throws DotPublishingException {

        PushPublishLogger.log( this.getClass(), "Started Publishing Task", config.getId() );

        //Triggering event listener when the publishing process starts
        localSystemEventsAPI.asyncNotify(new PushPublishStartEvent(config.getAssets()));
        localSystemEventsAPI.asyncNotify(new StaticPublishStartEvent(config.getAssets()));

        try {

            List<IBundler> confBundlers = new ArrayList<>();

            // init publishers
            for ( Class<Publisher> c : config.getPublishers() ) {
                // Process config
                Publisher publisher = c.getDeclaredConstructor().newInstance();
                config = publisher.init(config);

                if (config.isIncremental() && config.getEndDate() == null
                        && config.getStartDate() == null) {
                    // if its incremental and start/end dates aren't se we take it from latest bundle
                    if (BundlerUtil.bundleExists(config)) {
                        PublisherConfig pc = BundlerUtil.readBundleMeta(config);
                        if (pc.getEndDate() != null) {
                            config.setStartDate(pc.getEndDate());
                            config.setEndDate(new Date());
                        } else {
                            config.setStartDate(null);
                            config.setEndDate(new Date());
                        }
                    } else {
                        config.setStartDate(null);
                        config.setEndDate(new Date());
                    }
                }

                try (BundleOutput output = publisher.createBundleOutput();
                        ManifestBuilder manifestBuilder = new CSVManifestBuilder()){

                    manifestBuilder.addMetadata(CSVManifestBuilder.BUNDLE_ID_METADATA_NAME,
                            config.getId());

                    if (UtilMethods.isSet(config.getOperation())) {
                        manifestBuilder.addMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME,
                                config.getOperation().name());
                    }

                    if (!output.bundleFileExists() || !publishAuditAPI.isPublishRetry(config.getId())) {
                        output.create();
                        config.setManifestBuilder(manifestBuilder);
                        status.addOutput(output);

                        PublishAuditStatus currentStatus = publishAuditAPI
                                .getPublishAuditStatus(config.getId());
                        PublishAuditHistory currentStatusHistory = null;
                        if (currentStatus != null) {
                            currentStatusHistory = currentStatus.getStatusPojo();
                            if (currentStatusHistory != null) {
                                currentStatusHistory.setBundleStart(new Date());
                            }
                        }

                        for (Class<IBundler> clazz : publisher.getBundlers()) {
                            IBundler bundler = clazz.newInstance();
                            confBundlers.add(bundler);
                            bundler.setConfig(config);
                            bundler.setPublisher(publisher);
                            BundlerStatus bs = new BundlerStatus(bundler.getClass().getName());
                            status.addToBs(bs);
                            //Generate the bundler
                            Logger.info(this, "Start of Bundler: " + clazz.getSimpleName());
                            bundler.generate(output, bs);
                            Logger.info(this, "End of Bundler: " + clazz.getSimpleName());
                        }

                        if (currentStatusHistory != null) {
                            currentStatusHistory.setBundleEnd(new Date());
                            publishAuditAPI
                                    .updatePublishAuditStatus(config.getId(),
                                            PublishAuditStatus.Status.BUNDLING,
                                            currentStatusHistory);
                        }

                        if (config.getManifestFile().isPresent()) {
                            addManifestIntoBundleOutput(output, manifestBuilder,
                                    config.getManifestFile().get());
                        } else {
                            addBundleXMLIntoBundle(config, output);
                        }
                    } else {
                        Logger.info(this, "Retrying bundle: " + config.getId()
                                + ", we don't need to run bundlers again");
                    }
                }

                publisher.process(status);

                config.setBundlers(confBundlers);

                //Triggering event listener when the publishing process ends
                localSystemEventsAPI.asyncNotify(new PushPublishEndEvent(config.getAssets()));
                localSystemEventsAPI.asyncNotify(new StaticPublishEndEvent(config.getAssets()));

                PushPublishLogger.log(this.getClass(), "Completed Publishing Task", config.getId());
            }
        } catch (final Exception e) {
            final String errorMsg =
                    String.format("Error generating bundle ID '%s': %s", config.getId(), e.getMessage());
            Logger.error( PublisherAPIImpl.class, errorMsg, e );
            throw new DotPublishingException(errorMsg, e);
        }

        return status;
    }

    private void addBundleXMLIntoBundle(final PublisherConfig config, final BundleOutput output) {
        if (config.isStatic()) {
            //If static we just want to save the things that we need,
            // at this point only the id, static and operation.
            final PublisherConfig pcClone = new PublisherConfig();
            pcClone.setId(config.getId());
            pcClone.setStatic(true);
            pcClone.setOperation(config.getOperation());

            BundlerUtil.writeBundleMetaInfo(pcClone, output);
        } else {
            BundlerUtil.writeBundleMetaInfo(config, output);
        }
    }

    private void addManifestIntoBundleOutput(final BundleOutput output,
            final ManifestBuilder manifestBuilder,
            final File manifestFile) {
        try {
            manifestBuilder.close();
            output.copyFile(manifestFile, File.separator + ManifestBuilder.MANIFEST_NAME);
        } catch (final IOException e) {
            Logger.error(PublisherAPIImpl.class, "Error trying to copy the manifest file: " +
                    e.getMessage());
        }
    }

    /**
     * Initializes the data structures containing the Push Publishing Filter Descriptors. This method will access the
     * location that Filter Descriptors live in, loads them and validates them so that they can be accessed by dotCMS
     * or any User with the appropriate permissions.
     */
    @Override
    public void init() {
        try {

            final File basePath = PUBLISHING_FILTERS_FOLDER.get().toFile();
            if (!basePath.exists()) {
                Logger.info(this, ()->"Push Publishing Filters directory does not exist. Creating it under: " + PUBLISHING_FILTERS_FOLDER.get());
                final boolean mkDirOk = basePath.mkdirs();
                if(!mkDirOk){
                    Logger.error(PublisherAPIImpl.class,String.format("Failure creating basePath dir [%s]", basePath));
                }
                // If the directory does not exist, copy the YAML files that are shipped with dotCMS into the created
                // directory
                final String systemFiltersPathString =
                        Config.CONTEXT.getRealPath(File.separator + "WEB-INF" + File.separator + "publishing-filters" + File.separator);
                final File systemFilters = new File(systemFiltersPathString);
                try (
                        final Stream<Path> list = Try.of(()->Files.list(systemFilters.toPath())).getOrElse(Stream.of())
                ) {
                    list.forEach(filter -> {
                        try {
                            final Path partialPath = filter.getFileName();
                            final Path rootPath = PUBLISHING_FILTERS_FOLDER.get();
                            Files.copy(filter, rootPath.resolve(partialPath));
                        } catch (final IOException e) {
                            Logger.error(this, String.format(
                                    "An error occurred when copying PP filter '%s': %s",
                                    filter.getFileName(), e.getMessage()), e);
                        }
                    });
                    Logger.info(this, () -> "dotcms filters files copied");
                }
            }
            Logger.info(this, ()->"Push Publishing Filters Directory: " + PUBLISHING_FILTERS_FOLDER);
            // Read each YAML file under the directory and re-load the Filter list
            try(
                    final Stream<Path> list = Files.list(basePath.toPath());){
                final List<FilterDescriptor> descriptors = this.loadFiltersFromFolder(list);
                Collections.sort(descriptors);
                this.filterList = descriptors;
            }
        } catch (final IOException e) {
            Logger.error(this, String.format("PP Filters could not be initialized: %s", e.getMessage()), e);
        }
    }

    @Override
    public void loadFilter(final Path path) {
        final FilterDescriptor filterDescriptor = this.createFilterFromFile(path);
        if (null != filterDescriptor) {
            APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
        }
    }

    @Override
    public void addFilterDescriptor(final FilterDescriptor filterDescriptor) {
        this.filterList.remove(filterDescriptor);
        this.filterList.add(filterDescriptor);
        Collections.sort(filterList);
    }

    @Override
    public List<FilterDescriptor> getFiltersDescriptorsByRole(final User user) throws DotDataException {
        List<FilterDescriptor> filters;
        if (user.isAdmin()) {
            return this.filterList;
        } else {
            final List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), true);
            Logger.info(this, "User Roles: " + roles.toString());
            filters = new ArrayList<>();
            for (final FilterDescriptor filterDescriptorMap : this.filterList) {
                final String filterRoles = filterDescriptorMap.getRoles();
                Logger.info(PublisherAPI.class, "File: " + filterDescriptorMap.getKey() + ", Roles: " + filterRoles);
                for (final Role role : roles) {
                    if (UtilMethods.isSet(role.getRoleKey()) && filterRoles.contains(role.getRoleKey())) {
                        filters.add(filterDescriptorMap);
                    }
                }
            }
        }
        return filters;
    }

    @Override
    public boolean existsFilterDescriptor(final String filterKey) {
        return this.filterList.stream().anyMatch(filter -> filter.getKey().equalsIgnoreCase(filterKey));
    }

    @Override
    public FilterDescriptor getFilterDescriptorByKey(final String filterKey) {
        final FilterDescriptor defaultFilter = getDefaultFilter();
        return !UtilMethods.isSet(filterKey) ? defaultFilter :
                this.filterList.stream().filter(filter -> filterKey.equalsIgnoreCase(filter.getKey())).findFirst().orElse(defaultFilter);
    }

    @SuppressWarnings("unchecked")
    @CloseDBIfOpened
    @Override
    public PublisherFilter createPublisherFilter(final String bundleId) throws DotDataException, DotSecurityException {

        final String filterKey = APILocator.getBundleAPI().getBundleById(bundleId).getFilterKey();
        final FilterDescriptor filterDescriptor = this.getFilterDescriptorByKey(filterKey);
        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(filterKey, (Boolean)filterDescriptor.getFilters().getOrDefault(FilterDescriptor.DEPENDENCIES_KEY,true),
                (Boolean)filterDescriptor.getFilters().getOrDefault(FilterDescriptor.RELATIONSHIPS_KEY,true));

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_CLASSES_KEY)){
            List.class.cast(filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_CLASSES_KEY)).forEach(type -> publisherFilter.addTypeToExcludeClassesSet(type.toString()));
        }

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_DEPENDENCY_CLASSES_KEY)){
            List.class.cast(filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_DEPENDENCY_CLASSES_KEY)).forEach(type -> publisherFilter.addTypeToExcludeDependencyClassesSet(type.toString()));
        }

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_QUERY_KEY)){
            final String query = filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_QUERY_KEY).toString();
            APILocator.getContentletAPI().search(query, 0, 0, MOD_DATE, APILocator.systemUser(), false)
                    .forEach(contentlet -> publisherFilter.addContentletIdToExcludeQueryAssetIdSet(contentlet.getIdentifier()));
        }

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_DEPENDENCY_QUERY_KEY)){
            final String query = filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_DEPENDENCY_QUERY_KEY).toString();
            APILocator.getContentletAPI().search(query, 0, 0, MOD_DATE, APILocator.systemUser(), false)
                    .forEach(contentlet -> publisherFilter.addContentletIdToExcludeDependencyQueryAssetIdSet(contentlet.getIdentifier()));
        }

        return publisherFilter;
    }

    @Override
    public void clearFilterDescriptorList() {
        this.filterList.clear();
    }

    @Override
    public boolean deleteFilterDescriptor(final String filterKey) {
        if (!this.existsFilterDescriptor(filterKey)) {
            Logger.warn(this, ()-> String.format("Filter '%s' does not exist", filterKey));
            return Boolean.FALSE;
        }
        final String parentFolder = PUBLISHING_FILTERS_FOLDER.get().toString();
        final File filterPathFile = Path.of(parentFolder, filterKey).toFile();
        try {
            if (filterPathFile.getCanonicalPath().startsWith(parentFolder) &&  FileUtils.deleteQuietly(filterPathFile)) {
                this.init();
                return Boolean.TRUE;
            }
        }catch (IOException e){
            Logger.error(PublisherAPIImpl.class, String.format("Exception trying to get canonical path from file [%s]",filterPathFile), e);
        }
        return Boolean.FALSE;
    }

    @Override
    public void upsertFilterDescriptor(FilterDescriptor filterDescriptor) {
        final File filterPathFile = Path.of(PUBLISHING_FILTERS_FOLDER.get().toString(), filterDescriptor.getKey()).toFile();
        YamlUtil.write(filterPathFile, filterDescriptor);
        this.init();
    }

    @Override
    public void saveFilterDescriptors(final List<File> filterFiles) {
        for (final File file : filterFiles) {
            final File filterPathFile =  Path.of(PUBLISHING_FILTERS_FOLDER.get().toString(), file.getName()).toFile();
            try {
                FileUtils.copyFile(file, filterPathFile);
            } catch (final IOException e) {
                Logger.warn(this, String.format("An error occurred when saving Filter Descriptor '%s': %s",
                        filterPathFile.getAbsolutePath(), e.getMessage()));
            }
        }
        this.init();
    }

    /**
     * Returns the default Filter Descriptor. If there isn't any, returns {@code null}.
     *
     * @return The default Filter Descriptor.
     */
    private FilterDescriptor getDefaultFilter(){
        return this.filterList.stream().filter(FilterDescriptor::isDefaultFilter).findFirst().orElse(null);
    }

    /**
     * Loads the Push Publishing Filter Descriptors living in the specified folder. Keep in mind that such files must
     * be YAML files.
     *
     * @param list The folder containing the Filters, represented as a {@link Stream<Path>} object.
     *
     * @return The list of {@link FilterDescriptor} objects in the specified folder.
     */
    private List<FilterDescriptor> loadFiltersFromFolder(final Stream<Path> list) {
        final List<FilterDescriptor> descriptors = new CopyOnWriteArrayList<>();
        list.forEach(path -> {
            final FilterDescriptor filterDescriptor = this.createFilterFromFile(path);
            if (null != filterDescriptor) {
                descriptors.add(filterDescriptor);
            }
        });
        return descriptors;
    }

    /**
     * Creates a Filter Descriptor object based on the specified file. This method performs two tasks:
     * <ol>
     *     <li>A {@link FilterDescriptor} object is created based on data from the specified YAML file.</li>
     *     <li>The filter information is validated and the respective errors are reported, if any.</li>
     * </ol>
     *
     * @param path The Descriptor Filter -- the YAML file -- as a {@link Path}.
     *
     * @return If the Descriptor Filter data is valid, an instance of ht e{@link FilterDescriptor} is returned.
     * Otherwise, a {@code null} is returned.
     */
    private FilterDescriptor createFilterFromFile(final Path path) {
        final String fileName = path.getFileName().toString();
        Logger.info(this, "Loading Push Publish Filter: " + fileName);
        try {
            final FilterDescriptor filterDescriptor = YamlUtil.parse(path, FilterDescriptor.class);
            filterDescriptor.setKey(fileName);
            Logger.info(this, filterDescriptor.toString());
            filterDescriptor.validate();
            return filterDescriptor;
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(), String.format("PP Filter '%s' could not be loaded: %s", path,
                    e.getMessage()), e);
            return null;
        }
    }

}
