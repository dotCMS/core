package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.FILE_NAME_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.MIMETYPE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.TITLE_FIELD;
import static com.dotmarketing.util.UtilHTML.getIconClass;
import static com.dotmarketing.util.UtilHTML.getStatusIcons;
import static com.dotmarketing.util.UtilMethods.getFileExtension;

import com.dotcms.api.APIProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * This class should take care of the transformation of asset of type dotAsset
 */
public class DotAssetViewStrategy extends WebAssetStrategy<Contentlet> {

    private static final String ASSET = "asset";
    private static final String UNKNOWN = "unknown";

    /**
     * Main constructor
     * @param toolBox
     */
    DotAssetViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Concrete type transform method
     * @param dotAsset
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet dotAsset,
            final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotDataException, DotSecurityException {

        String fileName;
        long fileSize;
            //TODO: in a near future this must be read from a pre-cached metadata.
            final Object asset = dotAsset.get(ASSET);
            if(asset instanceof File ){
                final File file = (File)asset;
                fileName = file.getName();
                fileSize = file.length();
            } else {
                //There are scenarios on which a contentlet is pushed back into the transformers pipeline to add extra stuff etc..
                //We're probably looking at contantlet that has already been transformed
                //so we still have an "asset" but it's not java.io.File it's a string
                //So lets try to get these attributes from the already transformed contentlet

                //unknown is a fallback.
                //if we're seeing it something has gone wrong.
                fileName = Try.of(() -> (String)dotAsset.get("name")).getOrElse(UNKNOWN);
                fileSize = Try.of(() -> (long)dotAsset.get("size")).getOrElse(0L);
            }

        map.put(MIMETYPE_FIELD, toolBox.fileAssetAPI.getMimeType(fileName));
        map.put("isContentlet", true);
        map.computeIfAbsent(TITLE_FIELD, k -> fileName);
        map.put("type", "dotasset");
        map.put("path", ResourceLink.getPath(dotAsset));
        map.put("name", fileName);
        map.put("size", fileSize);
        try {
            map.put("__icon__", getIconClass(dotAsset));
        } catch (Exception e) {
            Logger.warn(DotAssetViewStrategy.class, "Failed to get icon.", e);
        }
        try {
            map.put("statusIcons", getStatusIcons(dotAsset));
        } catch (Exception e) {
            Logger.warn(DotAssetViewStrategy.class, "Failed to get icon.", e);
        }
        map.put("extension",  getFileExtension(fileName));

        if(options.contains(USE_ALIAS)) {
            map.put(FILE_NAME_FIELD, fileName);
            map.put("fileSize", fileSize);
        }

        return map;
    }
}
