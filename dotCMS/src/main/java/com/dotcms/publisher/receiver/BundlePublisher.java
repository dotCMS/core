package com.dotcms.publisher.receiver;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.handler.BundleXMLascHandler;
import com.dotcms.enterprise.publishing.remote.handler.CategoryFullHandler;
import com.dotcms.enterprise.publishing.remote.handler.CategoryHandler;
import com.dotcms.enterprise.publishing.remote.handler.ContainerHandler;
import com.dotcms.enterprise.publishing.remote.handler.ContentHandler;
import com.dotcms.enterprise.publishing.remote.handler.ContentTypeHandler;
import com.dotcms.enterprise.publishing.remote.handler.ContentWorkflowHandler;
import com.dotcms.enterprise.publishing.remote.handler.ExperimentHandler;
import com.dotcms.enterprise.publishing.remote.handler.FolderHandler;
import com.dotcms.enterprise.publishing.remote.handler.HostHandler;
import com.dotcms.enterprise.publishing.remote.handler.LanguageHandler;
import com.dotcms.enterprise.publishing.remote.handler.LanguageVariablesHandler;
import com.dotcms.enterprise.publishing.remote.handler.LinkHandler;
import com.dotcms.enterprise.publishing.remote.handler.OSGIHandler;
import com.dotcms.enterprise.publishing.remote.handler.RelationshipHandler;
import com.dotcms.enterprise.publishing.remote.handler.RuleHandler;
import com.dotcms.enterprise.publishing.remote.handler.TemplateHandler;
import com.dotcms.enterprise.publishing.remote.handler.UserHandler;
import com.dotcms.enterprise.publishing.remote.handler.VariantHandler;
import com.dotcms.enterprise.publishing.remote.handler.WorkflowHandler;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.manifest.CSVManifestReader;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.publishing.manifest.ManifestReason;
import com.dotcms.rest.BundlePublisherResource;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishEndOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishFailureOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishStartOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishSuccessOnReceiverEvent;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tools.tar.TarBuffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * This publisher will be in charge of retrieving the bundle, un-zipping it, and
 * saving the different contents in it based on a predefined list of content
 * handler classes.
 * <p>
 * An {@link IHandler} class provides the logic to import the new content, based
 * on its specified business rules. These handlers read the respective data
 * files (i.e., a Container handler will only read Container data files),
 * retrieve the Java objects that they represent, and imports their content in
 * the destination server.
 *
 * @author Alberto
 * @version 1.0
 * @since Oct 26, 2012
 */
public class BundlePublisher extends Publisher {

    private PublishAuditAPI auditAPI = null;

    boolean bundleSuccess = true;

    private List<IHandler> handlers = new ArrayList<>();

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
        Logger.debug(BundlePublisher.class, "Initializing bundle publisher");
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            throw new RuntimeException("need an enterprise license to run this");
        }
        handlers = new ArrayList<>();
        handlers.add(new BundleXMLascHandler(config));
        //The order is really important
        handlers.add(new UserHandler(config));
        handlers.add(new CategoryHandler(config));
        handlers.add(new CategoryFullHandler(config));
        handlers.add(new HostHandler(config));
        handlers.add(new FolderHandler(config));
        handlers.add(new WorkflowHandler(config));
        handlers.add(new ContentTypeHandler(config));
        handlers.add(new RelationshipHandler(config));
        handlers.add(new ContainerHandler(config));
        handlers.add(new TemplateHandler(config));
        handlers.add(new LanguageHandler(config));
        handlers.add(new LanguageVariablesHandler(config));
        handlers.add(new VariantHandler(config));
        handlers.add(new ContentHandler(config));
        handlers.add(new ExperimentHandler(config));
        handlers.add(new ContentWorkflowHandler(config));
        handlers.add(new OSGIHandler(config));
        handlers.add(new LinkHandler(config));
        handlers.add(new RuleHandler(config));
        auditAPI = PublishAuditAPI.getInstance();
        this.config = super.init(config);
        return this.config;
    }

    /**
     * Processes the contents of a bundle. The process consists of uncompressing
     * the bundle file, and having each {@link IHandler} class analyze and
     * process the corresponding data files.
     *
     * @param status - Current status of the publishing process.
     * @return This bundle configuration ({@link PublisherConfig}).
     * @throws DotPublishingException An error occurred when handling the contents of this bundle.
     */
    @Override
    public PublisherConfig process(final PublishStatus dassdasda) throws DotPublishingException {
        Logger.debug(BundlePublisher.class, "Processing bundle");
        if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
            throw new RuntimeException( "need an enterprise license to run this" );
        }

        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        boolean hasWarnings = false;
        String bundleName = config.getId();
        String bundleID = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        String bundlePath =
                ConfigUtils.getBundlePath() + File.separator + BundlePublisherResource.MY_TEMP;

        //Publish the bundle extracted
        PublishAuditHistory currentStatusHistory = null;
        EndpointDetail detail = new EndpointDetail();

        try {
            //Update audit
            Logger.debug(BundlePublisher.class, "Updating audit table for bundle with ID '" + bundleName + "'");
            currentStatusHistory = config.getPublishAuditStatus().getStatusPojo();
            currentStatusHistory.setPublishStart(new Date());

            detail.setStatus(PublishAuditStatus.Status.PUBLISHING_BUNDLE.getCode());
            detail.setInfo("Publishing bundle");
            String endPointId = (String) currentStatusHistory.getEndpointsMap().keySet().toArray()[0];
            currentStatusHistory.addOrUpdateEndpoint(endPointId, endPointId, detail);

            auditAPI.updatePublishAuditStatus(bundleID, PublishAuditStatus.Status.PUBLISHING_BUNDLE,
                currentStatusHistory);
            // Notify to anyone subscribed the PP is about to start
            Logger.debug(this, "Notify PushPublishStartOnReceiverEvent");
            localSystemEventsAPI.asyncNotify(new PushPublishStartOnReceiverEvent(config.getAssets()));
        } catch (Exception e) {
            Logger.error(BundlePublisher.class, "Unable to update audit table for bundle with ID '" + bundleName + "': " + e.getMessage(), e);
        }

        File folderOut = new File(bundlePath + bundleID);
        if(folderOut.exists()){
          FileUtil.deltree(folderOut);
        }
        folderOut.mkdir();

        // Extract file to a directory
        InputStream bundleIS = null;
        try {
            bundleIS = Files.newInputStream(Paths.get(bundlePath + bundleName));
            untar(bundleIS, folderOut.getAbsolutePath() + File.separator + bundleName, bundleName);
        } catch (IOException e) {

            // Notify to anyone subscribed the PP is failed
            Logger.debug(this, "Notify PushPublishFailureOnReceiverEvent");
            localSystemEventsAPI.asyncNotify(new PushPublishFailureOnReceiverEvent(config.getAssets(), e));
            throw new DotPublishingException("Cannot extract the selected archive", e);
        } finally {
            CloseUtils.closeQuietly(bundleIS);
        }

        Map<String, String> assetsDetails = null;

        String finalBundlePath = ConfigUtils.getBundlePath() + File.separator + bundleID;
        BundleMetaDataFile bundleMetaDataFile = null;

        try {
            Logger.debug(BundlePublisher.class, "Getting assets list from received bundle with ID '" + bundleName + "'");
            bundleMetaDataFile = new BundleMetaDataFile(finalBundlePath);
            assetsDetails = bundleMetaDataFile.getAssetsDetails();
        } catch (Exception e) {
            Logger.error(BundlePublisher.class, "Unable to get assets list from received bundle with ID '" + bundleName + "': " + e.getMessage(), e);
        }

        try {
            HibernateUtil.startTransaction();
            // Execute the handlers
            for (IHandler handler : handlers) {
                Logger.debug(BundlePublisher.class, "Start of Handler: " + handler.getName());
                handler.handle(folderOut);

                if (!handler.getWarnings().isEmpty()){
                    detail.setStatus(Status.SUCCESS_WITH_WARNINGS.getCode());
                    if (!hasWarnings) {
                        detail.setInfo(StringUtils.join(handler.getWarnings(), "\n"));
                    } else {
                        detail.setInfo(detail.getInfo() + "\n" + StringUtils.join(handler.getWarnings(), "\n"));
                    }
                    hasWarnings = true;
                }
                Logger.debug(BundlePublisher.class, "End of Handler: " + handler.getName());
            }
            HibernateUtil.commitTransaction();
        } catch (Exception e) {
            bundleSuccess = false;
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(BundlePublisher.class, e.getMessage(), e1);
            }
            Logger.error(BundlePublisher.class, "Error publishing bundle with ID '" + bundleName + "': " + e.getMessage(), e);

            //Update audit
            try {
                detail.setStatus(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode());
                detail.setInfo("Failed to publish because an error occurred: " + e.getMessage());
                detail.setStackTrace(ExceptionUtils.getStackTrace(e));
                String endPointId = (String) currentStatusHistory.getEndpointsMap().keySet().toArray()[0];
                currentStatusHistory.addOrUpdateEndpoint(endPointId, endPointId, detail);
                currentStatusHistory.setPublishEnd(new Date());
                currentStatusHistory.setAssets(assetsDetails);

                auditAPI.updatePublishAuditStatus(bundleID, PublishAuditStatus.Status.FAILED_TO_PUBLISH,
                        currentStatusHistory);

                Logger.debug(this, "Notify PushPublishFailureOnReceiverEvent");
                localSystemEventsAPI.asyncNotify(new PushPublishFailureOnReceiverEvent(config.getAssets(), e));
            } catch (DotPublisherException e1) {
                throw new DotPublishingException("Cannot update audit of bundle with ID '" + bundleName + "': ", e);
            }
            throw new DotPublishingException("Error publishing bundle with ID '" + bundleName + "': " + e, e);
        } finally {
            DbConnectionFactory.closeSilently();
        }

        try {
            //Update audit
            if (!hasWarnings) {
                detail.setStatus(PublishAuditStatus.Status.SUCCESS.getCode());
                detail.setInfo("Everything ok");
            }
            String endPointId = (String) currentStatusHistory.getEndpointsMap().keySet().toArray()[0];
            currentStatusHistory.addOrUpdateEndpoint(endPointId, endPointId, detail);
            currentStatusHistory.setPublishEnd(new Date());
            currentStatusHistory.setAssets(assetsDetails);
            auditAPI.updatePublishAuditStatus(bundleID,
                    hasWarnings ? Status.SUCCESS_WITH_WARNINGS : PublishAuditStatus.Status.SUCCESS,
                    currentStatusHistory);
            config.setPublishAuditStatus(auditAPI.getPublishAuditStatus(bundleID));

            // Everything success and the process ends
            Logger.debug(this, "Notify PushPublishSuccessOnReceiverEvent and PushPublishEndOnReceiverEvent");
            localSystemEventsAPI.asyncNotify(new PushPublishSuccessOnReceiverEvent(config));

            localSystemEventsAPI.asyncNotify(new PushPublishEndOnReceiverEvent(
                    UtilMethods.isSet(bundleMetaDataFile) ? bundleMetaDataFile.getBundlerAssets() : Collections.EMPTY_LIST));
        } catch (Exception e) {

            localSystemEventsAPI.asyncNotify(new PushPublishFailureOnReceiverEvent(config.getAssets(), e));
            Logger.error(BundlePublisher.class, "Unable to update audit table for bundle with ID '" + bundleName + "': " + e.getMessage(), e);
        }

        return config;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Class> getBundlers() {
        List<Class> list = new ArrayList<>();

        return list;
    }

    /**
     * Untars the given bundle file in order process its contents.
     *
     * @param bundle   - The {@link InputStream} containing the bundle.
     * @param path     - The location where the bundle will be uncompressed.
     * @param fileName - The file name of the bundle.
     * @throws DotPublisherException 
     */
    private void untar(InputStream bundle, String path, String fileName) throws DotPublishingException {
        Logger.debug(BundlePublisher.class, "Untaring bundle: " + fileName);
        TarArchiveEntry entry;
        TarArchiveInputStream inputStream = null;
        OutputStream outputStream = null;
        File baseBundlePath = new File(ConfigUtils.getBundlePath());
        try {
            //Clean the bundler folder if exist to clean dirty data
            String previousFolderPath = path.replace(fileName, "");
            File previousFolder = new File(previousFolderPath);
            if (previousFolder.exists()) {
                FileUtils.cleanDirectory(previousFolder);
            }
            // get a stream to tar file
            InputStream gstream = new GZIPInputStream(bundle);
            inputStream =
                new TarArchiveInputStream(gstream, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE,
                    UtilMethods.getCharsetConfiguration());

            // For each entry in the tar, extract and save the entry to the file
            // system
            while (null != (entry = inputStream.getNextTarEntry())) {
                // for each entry to be extracted
                int bytesRead;

                String pathWithoutName = path.substring(0, path.indexOf(fileName));
                File fileOrDir = new File(pathWithoutName + entry.getName());
                
                // if the logFile is outside of of the logFolder, die
                if ( !fileOrDir.getCanonicalPath().startsWith(baseBundlePath.getCanonicalPath())) {

                    SecurityLogger.logInfo(this.getClass(),  "Invalid Bundle writing file outside of bundlePath"  );
                    SecurityLogger.logInfo(this.getClass(),  " Bundle path "  + baseBundlePath );
                    SecurityLogger.logInfo(this.getClass(),  " Evil File"  + fileOrDir );
                    throw new DotPublishingException("Bundle trying to write outside of proper path:" + fileOrDir);
                }
                
                
                // if the entry is a directory, create the directory
                if (entry.isDirectory()) {
                    fileOrDir.mkdirs();
                    continue;
                }


                
                // We will ignore symlinks
                if(entry.isLink() || entry.isSymbolicLink()){
                  SecurityLogger.logInfo(this.getClass(),  "Invalid Bundle writing symlink (or some non-file) inside a bundle"  );
                  SecurityLogger.logInfo(this.getClass(),  " Bundle path "  + baseBundlePath );
                  SecurityLogger.logInfo(this.getClass(),  " Evil entry"  + entry );
                  throw new DotPublishingException("Bundle contains a symlink:" + fileOrDir);
                }

                fileOrDir.getParentFile().mkdirs();

                // write to file
                byte[] buf = new byte[1024];
                outputStream = Files.newOutputStream(fileOrDir.toPath());
                while ((bytesRead = inputStream.read(buf, 0, 1024)) > -1) {
                    outputStream.write(buf, 0, bytesRead);
                }
                try {
                    if (null != outputStream) {
                        outputStream.close();
                    }
                } catch (Exception e) {
                    Logger.warn(this.getClass(), "Error Closing Stream.", e);
                }
            }// while
            Logger.debug(BundlePublisher.class, "Untaring bundle finished");
        } catch (Exception e) {
            throw new DotPublishingException(e.getMessage(),e);

        } finally { // close your streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Logger.warn(this.getClass(), "Error Closing Stream.", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Logger.warn(this.getClass(), "Error Closing Stream.", e);
                }
            }
        }
    }

    private static class BundleMetaDataFile {
        private static String ASSET_DETAILS_KEY = "assetsDetails";
        private static String BUNDLER_ASSETS_KEY = "bundlerAssets";

        private final Map<String, Object> metaData;

        BundleMetaDataFile(final String finalBundlePath) {
            final String manifestFilePath = finalBundlePath + File.separator + ManifestBuilder.MANIFEST_NAME;
            final File manifestFile = new File(manifestFilePath);

            metaData = manifestFile.exists() ? getAssetsDetailsFromManifest(manifestFile) :
                    getAssetsDetailsFromBundleXML(finalBundlePath);
        }

        private Map<String, Object> getAssetsDetailsFromBundleXML(final String finalBundlePath) {
            Logger.debug(BundlePublisher.class, "Getting assets details from bundle.xml for bundle: " + finalBundlePath);
            File xml = new File(finalBundlePath + File.separator + "bundle.xml");

            PushPublisherConfig readConfig = (PushPublisherConfig) BundlerUtil.readBundleMeta(xml);
            final List<PublishQueueElement> bundlerAssets = UtilMethods.isSet(readConfig.getAssets()) ?
                    readConfig.getAssets() : Collections.EMPTY_LIST;

            final Map<String, String> assetsDetails = bundlerAssets.stream()
                    .collect(Collectors
                            .toMap(PublishQueueElement::getAsset, PublishQueueElement::getType));
            return Map.of(ASSET_DETAILS_KEY, assetsDetails, BUNDLER_ASSETS_KEY, bundlerAssets);
        }

        private Map<String, Object> getAssetsDetailsFromManifest(final File manifestFile) {
            Logger.debug(BundlePublisher.class, "Getting assets details from manifest for bundle: " + manifestFile.getName());
            final Map<String, String> assetsDetails = new HashMap<>();

            Collection<ManifestInfo> bundlerAssets;
            final CSVManifestReader csvManifestReader = new CSVManifestReader(manifestFile);
            bundlerAssets = csvManifestReader.getAssets(ManifestReason.INCLUDE_BY_USER);

            if (bundlerAssets != null && !bundlerAssets.isEmpty()) {
                for (ManifestInfo manifestInfo : bundlerAssets) {
                    assetsDetails.put(manifestInfo.id(), manifestInfo.objectType().toLowerCase());
                }
            }

            return Map.of(ASSET_DETAILS_KEY, assetsDetails,
                    BUNDLER_ASSETS_KEY, csvManifestReader.getPublishQueueElement());
        }

        public Map<String, String> getAssetsDetails() {
            return (Map<String, String>) metaData.get(ASSET_DETAILS_KEY);
        }

        public List<PublishQueueElement> getBundlerAssets() {
            return (List<PublishQueueElement>) metaData.get(BUNDLER_ASSETS_KEY);
        }
    }
}
