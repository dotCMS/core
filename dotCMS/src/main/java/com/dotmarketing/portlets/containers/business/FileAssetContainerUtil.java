package com.dotmarketing.portlets.containers.business;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dotmarketing.util.StringUtils.builder;
import static com.liferay.util.StringPool.FORWARD_SLASH;

/**
 * This util is in charge of handling the creation of the FileAsset containers based on the folder and their contains.
 */
public class FileAssetContainerUtil {

    private static final int DEFAULT_MAX_CONTENTLETS = 10;
    private static final String TITLE                = "title";
    private static final String DESCRIPTION          = "description";
    private static final String MAX_CONTENTLETS      = "max_contentlets";
    private static final String NOTES                = "notes";
    private static final String HOST_INDICATOR       = "//";

    private static String [] DEFAULT_META_DATA_NAMES_ARRAY
            = new String[] { TITLE, DESCRIPTION, MAX_CONTENTLETS, NOTES};

    static final String PRE_LOOP             = "preloop.vtl";
    static final String POST_LOOP            = "postloop.vtl";
    static final String CONTAINER_META_INFO  = "container.vtl";



    private static class SingletonHolder {
        private static final FileAssetContainerUtil INSTANCE = new FileAssetContainerUtil();
    }
    /**
     * Get the instance.
     * @return ContainerByFolderAssetsUtil
     */
    public static FileAssetContainerUtil getInstance() {

        return FileAssetContainerUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Determines if the container id is a path or uudi, if not any match will return UNKNOWN.
     * @param containerIdOrPath String path or uddi
     * @return Source (File if it is a fs container, DB if it is a db container, otherwise UNKNOWN)
     */
    public Source getContainerSourceFromContainerIdOrPath(final String containerIdOrPath) {

        Source source = Source.UNKNOWN;

        if (this.isFolderAssetContainerId(containerIdOrPath)) {
            source = Source.FILE;
        } else if (this.isDataBaseContainerId(containerIdOrPath)) {
            source = Source.DB;
        }

        return source;
    }

    public boolean isDataBaseContainerId(final String containerIdentifier) {

        boolean isIdentifier = false;
        isIdentifier |= UUIDUtil.isUUID(containerIdentifier);

        if (!isIdentifier) {
            try {
                APILocator.getShortyAPI().validShorty(containerIdentifier);
                isIdentifier = true;
            } catch (Exception e) {
                isIdentifier = false;
            }
        }

        return isIdentifier;
    }

    public boolean isFolderAssetContainerId(final String containerPath) {

        return UtilMethods.isSet(containerPath) && containerPath.contains(FORWARD_SLASH);
    }

    /**
     * Based on a host, folder and collection of asset, creates a fs container by convention.
     * @param host {@link Host} the host of the container
     * @param containerFolder {@link Folder} this folder represents the container
     * @param assets {@link List} of {@link FileAsset} has all meta info such as container.vtl, preloop.vtl, postloop.vtl and all content types.
     * @param showLive {@link Boolean} true if only want published assets
     * @param includeHostOnPath {@link Boolean} true if want to include the host on the {@link Container}.path,
     *                                         this is usually needed when a resource of host A, wants to use a container from host B, the path inside the {@link FileAssetContainer}
     *                                         must include the host, otherwise will be relative
     * @return Container
     * @throws DotDataException
     */
    public Container fromAssets(final Host host, final Folder containerFolder,
                                final List<FileAsset> assets, final boolean showLive,
                                final boolean includeHostOnPath) throws DotDataException {

        final ImmutableList.Builder<FileAsset> containerStructures =
                new ImmutableList.Builder<>();
        final FileAssetContainer container =
                new FileAssetContainer();
        Optional<String> preLoop           = Optional.empty();
        Optional<String> postLoop          = Optional.empty();
        Optional<String> containerMetaInfo = Optional.empty();
        FileAsset metaInfoFileAsset        =  null;


        for (final FileAsset fileAsset : assets) {

            if (this.isPreLoop(fileAsset, showLive)) {

                preLoop = Optional.of(this.wrapIntoDotParseDirective(fileAsset)); continue;
            }

            if (this.isPostLoop(fileAsset, showLive)) {

                postLoop = Optional.of(this.wrapIntoDotParseDirective(fileAsset)); continue;
            }

            if (this.isContainerMetaInfo(fileAsset, showLive)) {

                metaInfoFileAsset = fileAsset;
                containerMetaInfo = Optional.of(this.toString(fileAsset)); continue;
            }

            if (this.isValidContentType(showLive, fileAsset)) {
                containerStructures.add(fileAsset);
            }
        }

        if (null == metaInfoFileAsset) {

            throw new NotFoundInDbException("On getting the container by folder, the folder: " + containerFolder.getPath() +
                    " is not valid, it must be under: " + Constants.CONTAINER_FOLDER_PATH + " and must have a child file asset called: " +
                    Constants.CONTAINER_META_INFO_FILE_NAME);
        }

        this.setContainerData(host, containerFolder, metaInfoFileAsset, containerStructures.build(), container,
                preLoop, postLoop, containerMetaInfo.get(), includeHostOnPath);

        return container;
    }

    private boolean isValidContentType(final boolean showLive, final FileAsset fileAsset) {
        try {
            return !fileAsset.isArchived() && this.isMode(fileAsset, showLive);
        } catch (DotDataException | DotSecurityException e) {
            return false;
        }
    }

    /**
     * Wraps the file asset into the dotParse directive, this is helpful in order to fetch lazy on runtime the fileasset and also, to add multi lang capabilities
     * @param fileAsset {@link FileAsset}
     * @return String
     */
    public String wrapIntoDotParseDirective (final FileAsset fileAsset) {

        try {

            final Host host = APILocator.getHostAPI().find(fileAsset.getHost(), APILocator.systemUser(), false);

            return "#dotParse(\"//" + host.getHostname()  + fileAsset.getPath() + fileAsset.getFileName() + "\")";

        } catch (DotSecurityException | DotDataException  e) {
            return StringPool.BLANK;
        }
    }

    private void setContainerData(final Host host,
                                  final Folder containerFolder,
                                  final FileAsset metaInfoFileAsset,
                                  final List<FileAsset> containerStructures,
                                  final FileAssetContainer container,
                                  final Optional<String> preLoop,
                                  final Optional<String> postLoop,
                                  final String containerMetaInfo,
                                  final boolean includeHostOnPath) {

        container.setIdentifier (metaInfoFileAsset.getIdentifier());
        container.setInode      (metaInfoFileAsset.getInode());
        container.setOwner      (metaInfoFileAsset.getOwner());
        container.setIDate      (metaInfoFileAsset.getIDate());
        container.setModDate    (metaInfoFileAsset.getModDate());
        container.setModUser    (metaInfoFileAsset.getModUser());
        container.setShowOnMenu (metaInfoFileAsset.isShowOnMenu());
        container.setSortOrder  (containerFolder.getSortOrder());
        container.setTitle      (containerFolder.getTitle());
        container.setMaxContentlets(DEFAULT_MAX_CONTENTLETS);
        container.setLanguage(metaInfoFileAsset.getLanguageId());
        container.setFriendlyName((String)metaInfoFileAsset.getMap().getOrDefault(DESCRIPTION, container.getTitle()));
        container.setPath(this.buildPath(host, containerFolder, includeHostOnPath));

        preLoop.ifPresent (value -> container.setPreLoop (value));
        postLoop.ifPresent(value -> container.setPostLoop(value));
        this.setMetaInfo (containerMetaInfo, container);

        container.setContainerStructuresAssets(containerStructures);
    }

    private String buildPath(final Host host, final Folder containerFolder, final boolean includeHostOnPath) {

        return includeHostOnPath?
                builder(HOST_INDICATOR, host.getHostname(), containerFolder.getPath()).toString():
                containerFolder.getPath();
    }

    private boolean isContainerMetaInfo(final FileAsset fileAsset, final boolean showLive) {

        return isType(fileAsset, showLive, CONTAINER_META_INFO);
    }

    private boolean isPreLoop(final FileAsset fileAsset, final boolean showLive) {

        return isType(fileAsset, showLive, PRE_LOOP);
    }

    private boolean isPostLoop(final FileAsset fileAsset, final boolean showLive) {

        return isType(fileAsset, showLive, POST_LOOP);
    }


    private void setMetaInfo(final String metaInfoVTLContent, final FileAssetContainer container) {

        // todo: check the cache
        final Context context         = new VelocityContext();
        final StringWriter evalResult = new StringWriter();

        try {
            context.put("dotJSON", new DotJSON());
            VelocityUtil.getEngine().evaluate(context, evalResult, StringPool.BLANK, metaInfoVTLContent);
            final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

            // todo: let's add it to cache
            // cache.add(request, user, dotJSON);
            final Map<String, Object> map = dotJSON.getMap();
            if (UtilMethods.isSet(map)) {

                container.setTitle((String) map.getOrDefault(TITLE, container.getTitle()));
                container.setFriendlyName((String) map.getOrDefault(DESCRIPTION, container.getFriendlyName()));
                container.setMaxContentlets(ConversionUtils.toInt(map.get(MAX_CONTENTLETS), 10));
                container.setNotes((String) map.getOrDefault(NOTES, StringPool.BLANK));

                this.removeDefaultMetaDataNames(map);

                for (final Map.Entry<String, Object> entry : map.entrySet()) {

                    container.addMetaData(entry.getKey(), entry.getValue());
                }
            }
        } catch(ParseErrorException e) {
            Logger.error(this, "Parsing error, setting the meta data of the container: " + container.getName() +
                    ", velocity code: " + metaInfoVTLContent, e);
        }
    }

    private void removeDefaultMetaDataNames(Map<String, Object> map) {
        for (final String metaDataName : DEFAULT_META_DATA_NAMES_ARRAY) {

            if (map.containsKey(metaDataName)) {
                map.remove(metaDataName);
            }
        }
    }

    private boolean isType(final FileAsset fileAsset, final boolean showLive, final String type) {

        final String fileName = fileAsset.getFileName();
        final boolean isMode  = this.isMode(fileAsset, showLive);

        return UtilMethods.isSet(fileName) && type.equalsIgnoreCase(fileName) && isMode;
    }

    private boolean isMode (final FileAsset fileAsset, final boolean showLive) {

        try {
            return showLive? fileAsset.isLive():fileAsset.isWorking();
        } catch (DotDataException | DotSecurityException e) {
            return false;
        }
    }

    private String toString (final FileAsset fileAsset) {

        try {
            return IOUtils.toString(fileAsset.getInputStream(),
                    UtilMethods.getCharsetConfiguration());
        } catch (IOException e) {
            return StringPool.BLANK;
        }
    }

    public Set<FileAsset> findContainerAssets(final Folder containerFolder)
            throws DotDataException, DotSecurityException {
        return ImmutableSet.<FileAsset>builder().addAll(APILocator.getFileAssetAPI()
                .findFileAssetsByFolder(containerFolder, null, false, APILocator.systemUser(),
                        false))
                                .build();
    }


}
