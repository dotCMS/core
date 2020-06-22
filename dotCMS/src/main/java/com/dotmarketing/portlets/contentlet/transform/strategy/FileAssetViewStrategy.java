package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LOAD_META;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.DESCRIPTION;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.FILE_NAME_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.META_DATA_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.MIMETYPE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.TITLE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.UNDERLYING_FILENAME;
import static com.dotmarketing.util.UtilHTML.getIconClass;
import static com.dotmarketing.util.UtilHTML.getStatusIcons;
import static com.dotmarketing.util.UtilMethods.getFileExtension;
import static com.dotmarketing.util.UtilMethods.isImage;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.api.APIProvider;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

public class FileAssetViewStrategy extends WebAssetStrategy<FileAsset> {

    private final ContentletCache contentletCache;

    /**
     * Main Constructor
     * @param toolBox
     */
    public FileAssetViewStrategy(final APIProvider toolBox) {
        super(toolBox);
        contentletCache = CacheLocator.getContentletCache();
    }

    /**
     * fromContentlet retrieves the concrete specific type of contentlet
     * @param contentlet
     * @return
     */
    @Override
    public FileAsset fromContentlet(final Contentlet contentlet) {
        final Contentlet cachedContent = contentletCache.get(contentlet.getInode());
        if(cachedContent instanceof FileAsset){
           Logger.debug(FileAssetViewStrategy.class, ()->String.format(" FileAsset cache hit `%s`",contentlet.getInode()));
           return (FileAsset)cachedContent;
        }
        Logger.debug(FileAssetViewStrategy.class, ()->String.format(" FileAsset cache miss `%s`",contentlet.getInode()));
        return toolBox.fileAssetAPI.fromContentlet(contentlet);
    }

    /**
     * Transform entry point
     * @param fileAsset
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     */
    @Override
    protected Map<String, Object> transform(final FileAsset fileAsset,
            final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) throws DotDataException {

        if(isNotSet(fileAsset.getTitle())) {
            final Identifier identifier = toolBox.identifierAPI.find(fileAsset.getIdentifier());
            map.put(TITLE_FIELD, identifier.getAssetName());
        }

        final String fileName = fileAsset.getFileName();
        final String underlyingFileName = fileAsset.getUnderlyingFileName();
        final long fileSize = fileAsset.getFileSize();
        map.put(MIMETYPE_FIELD, fileAsset.getMimeType());
        map.put("name", fileName);
        map.put("size", fileSize);
        final String description = fileAsset.getStringProperty(DESCRIPTION);
        map.put(DESCRIPTION, isSet(description) ? description : BLANK );
        map.put(UNDERLYING_FILENAME, underlyingFileName);
        if(options.contains(LOAD_META)) {
            map.put(META_DATA_FIELD, fileAsset.getMetaData());
        }
        map.put("path", fileAsset.getPath());
        final String parent = fileAsset.getParent();
        map.put("parent",  isSet(parent) ? parent : BLANK );

        if(isImage(underlyingFileName)) {
            map.put("width", fileAsset.getWidth());
            map.put("height", fileAsset.getHeight());
        }

        map.put("extension", getFileExtension(underlyingFileName));
        try {
            map.put("__icon__", getIconClass(fileAsset));
        }catch (Exception e){
            Logger.warn(FileAssetViewStrategy.class,"Failed to get icon.",e);
        }
        try {
            map.put("statusIcons", getStatusIcons(fileAsset));
        }catch (Exception e){
            Logger.warn(FileAssetViewStrategy.class," Failed to get status icon.",e);
        }

        if(options.contains(USE_ALIAS)) {
            map.put(FILE_NAME_FIELD, fileName);
            map.put("fileSize", fileSize);
        }
        map.put("type", "file_asset");
        map.put("isContentlet", true);
        return map;
    }
}
