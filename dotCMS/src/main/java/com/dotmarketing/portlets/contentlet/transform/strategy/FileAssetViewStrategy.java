package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.exception.ExceptionUtil;
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

import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;
import static com.liferay.util.StringPool.BLANK;

/**
 * This Strategy defines the way File Assets in dotCMS must be transformed into a Map of
 * attributes. Keep in mind that you can specify different {@link TransformOptions} that
 * allow you to add more properties to the resulting Map.
 *
 * @author Fabrizzio Araya
 * @since Jun 11th, 2020
 */
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
           Logger.debug(FileAssetViewStrategy.class, ()->String.format("FileAsset cache hit `%s`",contentlet.getInode()));
           return (FileAsset)cachedContent;
        }
        Logger.debug(FileAssetViewStrategy.class, ()->String.format("FileAsset cache miss `%s`",contentlet.getInode()));
        return toolBox.fileAssetAPI.fromContentlet(contentlet);
    }

    /**
     * Transforms the specified File Asset into a data Map with its different attributes.
     *
     * @param fileAsset The {@link FileAsset} to transform.
     * @param map       A Map containing additional attributes form the File Asset that will be
     *                  included in the transformation.
     * @param options   A Set of {@link TransformOptions} that will be used to determine which
     *                  attributes will be included in the transformation.
     * @param user      The {@link User} performing the action.
     *
     * @return A Map containing the different attributes from the specified File Asset.
     *
     * @throws DotDataException If an error occurs while accessing File Asset data.
     */
    @Override
    protected Map<String, Object> transform(final FileAsset fileAsset,
            final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) throws DotDataException {
        if (null == fileAsset.getMetadata()) {
            Logger.error(this, String.format("File Asset '%s' [ %s ] has no binary file associated" +
                    " with it in the dotCMS Assets folder", fileAsset.getFileName(),
                    fileAsset.getInode()));
        }
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
            map.put(META_DATA_FIELD, fileAsset.getMetaDataMap());
        }
        map.put("path", fileAsset.getPath());
        final String parent = fileAsset.getParent();
        map.put("parent",  isSet(parent) ? parent : BLANK );

        if(fileAsset.isImage()) {
            map.put("width", fileAsset.getWidth());
            map.put("height", fileAsset.getHeight());
        }

        map.put("extension", getFileExtension(underlyingFileName));
        try {
            map.put("__icon__", getIconClass(fileAsset));
        } catch (final Exception e) {
            Logger.warn(FileAssetViewStrategy.class, String.format("Failed to get 'icon' attribute from File Asset " +
                            "'%s' [ %s ]: %s", fileName, fileAsset.getInode(),
                    ExceptionUtil.getErrorMessage(e)), e);
        }
        try {
            map.put("statusIcons", getStatusIcons(fileAsset));
        } catch (final Exception e) {
            Logger.warn(FileAssetViewStrategy.class, String.format("Failed to get 'statusIcons' attribute from File Asset " +
                            "'%s' [ %s ]: %s", fileName, fileAsset.getInode(),
                    ExceptionUtil.getErrorMessage(e)), e);
        }

        if(options.contains(USE_ALIAS)) {
            map.put(FILE_NAME_FIELD, fileName);
            map.put("fileSize", fileSize);
        }
        map.put("type", fileAsset.getType());
        map.put("isContentlet", true);
        return map;
    }

}
