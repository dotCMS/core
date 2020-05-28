package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.FILE_NAME_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.MIMETYPE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.TITLE_FIELD;
import static com.dotmarketing.util.UtilHTML.getIconClass;
import static com.dotmarketing.util.UtilHTML.getStatusIcons;
import static com.dotmarketing.util.UtilMethods.getFileExtension;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Map;
import java.util.Set;

public class DotAssetTransformStrategy extends WebAssetStrategy<Contentlet> {

    private static final String ASSET = "asset";

    DotAssetTransformStrategy(final TransformToolbox toolBox) {
        super(toolBox);
    }

    @Override
    public Contentlet fromContentlet(final Contentlet contentlet) {
        return contentlet;
    }

    @Override
    public Map<String, Object> transform(final Contentlet dotAsset, final Map<String, Object> map,
            final Set<TransformOptions> options)
            throws DotDataException, DotSecurityException {

        String fileName = "unknown";
        long fileSize = 0L;
        try {
            final File asset = dotAsset.getBinary(ASSET);
            fileName = asset.getName();
            fileSize = asset.length();
        }catch (Exception e){
            Logger.warn(DotAssetTransformStrategy.class, "dotAsset does not have a binary ", e);
        }
        map.put(MIMETYPE_FIELD, toolBox.fileAssetAPI.getMimeType(fileName));

        map.put(TITLE_FIELD, fileName);
        map.put("type", "dotasset");
        map.put("path", ResourceLink.getPath(dotAsset));
        map.put("name", fileName);
        map.put("size", fileSize);

        map.put("__icon__", getIconClass(dotAsset));
        map.put("statusIcons", getStatusIcons(dotAsset));
        map.put("extension",  getFileExtension(fileName));

        if(options.contains(USE_ALIAS)) {
            map.put(FILE_NAME_FIELD, fileName);
            map.put("fileSize", fileSize);
        }

        return map;
    }
}
