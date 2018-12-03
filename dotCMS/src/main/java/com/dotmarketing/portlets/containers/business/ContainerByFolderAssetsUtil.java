package com.dotmarketing.portlets.containers.business;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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

public class ContainerByFolderAssetsUtil {

    private static final int DEFAULT_MAX_CONTENTLETS = 10;
    private static final String TITLE                = "title";
    private static final String FRIENDLY_NAME        = "friendly_name";
    private static final String MAX_CONTENTLETS      = "max_contentlets";
    private static final String NOTES                = "notes";

    private static String [] DEFAULT_META_DATA_NAMES_ARRAY
            = new String[] { TITLE, FRIENDLY_NAME, MAX_CONTENTLETS, NOTES};

    static final String PRE_LOOP             = "preloop.vtl";
    static final String POST_LOOP            = "postloop.vtl";
    static final String CONTAINER_META_INFO  = "container.vtl";


    private static class SingletonHolder {
        private static final ContainerByFolderAssetsUtil INSTANCE = new ContainerByFolderAssetsUtil();
    }
    /**
     * Get the instance.
     * @return ContainerByFolderAssetsUtil
     */
    public static ContainerByFolderAssetsUtil getInstance() {

        return ContainerByFolderAssetsUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    public Container fromAssets(final Folder containerFolder, final List<FileAsset> assets, final boolean showLive) throws DotDataException {

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

                preLoop = Optional.of(this.toString(fileAsset)); continue;
            }

            if (this.isPostLoop(fileAsset, showLive)) {

                postLoop = Optional.of(this.toString(fileAsset)); continue;
            }

            if (this.isContainerMetaInfo(fileAsset, showLive)) {

                metaInfoFileAsset = fileAsset;
                containerMetaInfo = Optional.of(this.toString(fileAsset)); continue;
            }

            if (this.isMode(fileAsset, showLive)) {
                containerStructures.add(fileAsset);
            }
        }

        if (null == metaInfoFileAsset) {

            throw new NotFoundInDbException("On getting the container by folder, the folder: " + containerFolder.getPath() +
                    " is not valid, it must be under: " + Constants.CONTAINER_FOLDER_PATH + " and must have a child file asset called: " +
                    Constants.CONTAINER_META_INFO_FILE_NAME);
        }

        this.setContainerData(containerFolder, metaInfoFileAsset, containerStructures.build(), container,
                preLoop, postLoop, containerMetaInfo.get());

        return container;
    }

    private void setContainerData(final Folder containerFolder,
                                  final FileAsset metaInfoFileAsset,
                                  final List<FileAsset> containerStructures,
                                  final FileAssetContainer container,
                                  final Optional<String> preLoop,
                                  final Optional<String> postLoop,
                                  final String containerMetaInfo) {

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

        preLoop.ifPresent (value -> container.setPreLoop (value));
        postLoop.ifPresent(value -> container.setPostLoop(value));
        this.setMetaInfo (containerMetaInfo, container);

        container.setContainerStructuresAssets(containerStructures);
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
                container.setFriendlyName((String) map.getOrDefault(FRIENDLY_NAME, StringPool.BLANK));
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
}
