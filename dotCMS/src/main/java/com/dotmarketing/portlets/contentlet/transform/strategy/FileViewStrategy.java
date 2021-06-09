package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LOAD_META;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.AVOID_MAP_SUFFIX_FOR_VIEWS;
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
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * TransformStrategy that includes a number of additional properties for each {@link FileField}
 * and {@link ImageField}.
 */

public class FileViewStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Main Constructor
     * @param toolBox
     */
    public FileViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Main Transform function
     * @param contentlet
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet contentlet,
            final Map<String, Object> map, final Set<TransformOptions> options, final User user) {

        final List<Field> fileAndImageFields = contentlet.getContentType().fields(FileField.class);
        fileAndImageFields.addAll(contentlet.getContentType().fields(ImageField.class));

        if (!fileAndImageFields.isEmpty()) {
            for (final Field field : fileAndImageFields) {
                try {
                    final String sufix = options.contains(AVOID_MAP_SUFFIX_FOR_VIEWS)
                            ? "" : "Map";
                    map.put(field.variable() + sufix, transform(field, contentlet, options));
                } catch (DotDataException e) {
                    Logger.warn(this,
                            "Unable to get Binary from field with var " + field.variable());
                }
            }
        }
        return map;
    }

    public Map<String, Object> transform(final Field field, final Contentlet contentlet,
            final Set<TransformOptions> options) throws DotDataException {
        final String fileAssetIdentifier = (String) contentlet.get(field.variable());

        if (!UtilMethods.isSet(fileAssetIdentifier)) {
            return null;
        }

        Optional<Contentlet> fileAsContentOptional = APILocator.getContentletAPI()
                .findContentletByIdentifierOrFallback(fileAssetIdentifier, Try.of(
                        contentlet::isLive).getOrElseThrow(()->new DotDataException("can't determine if content is live"))
                        , contentlet.getLanguageId(),
                        APILocator.systemUser(), true);

        FileAsset fileAsset = null;

        if(fileAsContentOptional.isPresent()) {
            fileAsset = APILocator.getFileAssetAPI().fromContentlet(fileAsContentOptional.get());
        }

        final Map<String, Object> map = new HashMap<>();

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

        if(isImage(underlyingFileName)) {
            map.put("width", fileAsset.getWidth());
            map.put("height", fileAsset.getHeight());
        }

        map.put("extension", getFileExtension(underlyingFileName));
        try {
            map.put("__icon__", getIconClass(fileAsset));
        }catch (Exception e){
            Logger.warn(FileViewStrategy.class,"Failed to get icon.",e);
        }
        try {
            map.put("statusIcons", getStatusIcons(fileAsset));
        }catch (Exception e){
            Logger.warn(FileViewStrategy.class," Failed to get status icon.",e);
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
