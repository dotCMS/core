package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.MIMETYPE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.TITLE_FIELD;
import static com.dotmarketing.util.UtilMethods.isNotSet;

import com.dotmarketing.beans.IconType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.UtilHTML;
import java.util.Map;
import java.util.Set;

public class HtmlPageTransformStrategy extends WebAssetStrategy<HTMLPageAsset> {

    HtmlPageTransformStrategy(final TransformToolbox toolBox) {
        super(toolBox);
    }

    @Override
    public HTMLPageAsset fromContentlet(final Contentlet contentlet) {
        return toolBox.htmlPageAssetAPI.fromContentlet(contentlet);
    }

    @Override
    public Map<String, Object> transform(final HTMLPageAsset page, final Map<String, Object> map,
            Set<TransformOptions> options)
            throws DotSecurityException, DotDataException {

        final String title = (String)map.get(TITLE_FIELD);
        if(isNotSet(title)){
           map.put(TITLE_FIELD, page.getPageUrl());
        }

        map.put(MIMETYPE_FIELD, "application/dotpage");
        map.put("name", page.getPageUrl());
        map.put("description", page.getFriendlyName());
        map.put("extension", "page");
        map.put("statusIcons", UtilHTML.getStatusIcons(page));
        map.put("__icon__", IconType.HTMLPAGE.iconName());

        return map;
    }
}
