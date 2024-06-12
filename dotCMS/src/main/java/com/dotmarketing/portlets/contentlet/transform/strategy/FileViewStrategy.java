package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LOAD_META;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.MAP_SUFFIX_FOR_VIEWS;
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
import com.dotcms.contenttype.business.DotAssetAPI;
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
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
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

        for (final Field field : fileAndImageFields) {
            try {
                final String relatedContentIdentifier = (String) contentlet.get(field.variable());

                Optional<Contentlet> optContent = APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(relatedContentIdentifier, PageMode.get().showLive
                                , contentlet.getLanguageId(),
                                APILocator.systemUser(), true);

                if (!optContent.isPresent()) {
                    map.put(field.variable(), relatedContentIdentifier);
                    return map;
                }
                final String fieldVar = optContent.get().isFileAsset() ? FileAssetAPI.BINARY_FIELD : DotAssetAPI.DOTASSET_FIELD_VAR;

                Map<String,Object> fileMap = BinaryToMapTransformer.transform(optContent.get(), optContent.get().getContentType().fieldMap().get(fieldVar), true);
                map.put(field.variable(), fileMap);

            } catch (Exception e) {
                Logger.warn(this,
                        "Unable to get Binary from field with var " + field.variable(), e);
            }
        }

        return map;
    }

}
