package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.MIMETYPE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.TITLE_FIELD;
import static com.dotmarketing.util.UtilMethods.isNotSet;

import com.dotmarketing.beans.IconType;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

/**
 * Strategy to handle Contentlets of type Page
 */
public class PageViewStrategy extends WebAssetStrategy<HTMLPageAsset> {

    private final HTMLPageCache htmlPageCache;

    /**
     * Main constructor
     * @param toolBox
     */
    PageViewStrategy(final TransformToolbox toolBox) {
        super(toolBox);
        htmlPageCache = CacheLocator.getHTMLPageCache();
    }

    /**
     * Instance Conversion method
     * @param contentlet
     * @return
     */
    @Override
    public HTMLPageAsset fromContentlet(final Contentlet contentlet) {
        final IHTMLPage page = htmlPageCache.get(contentlet.getInode());
        if(null != page){
          Logger.debug(PageViewStrategy.class, ()->String.format(" Page cache Hit `%s`",contentlet.getInode()));
          return (HTMLPageAsset)page;
        }
        Logger.debug(PageViewStrategy.class, ()->String.format(" Page cache Miss `%s`",contentlet.getInode()));
        return toolBox.htmlPageAssetAPI.fromContentlet(contentlet);
    }

    /**
     * Transform main method - entry point
     * @param page
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    protected Map<String, Object> transform(final HTMLPageAsset page, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotSecurityException, DotDataException {

        final String title = (String)map.get(TITLE_FIELD);
        if(isNotSet(title)){
           map.put(TITLE_FIELD, page.getPageUrl());
        }

        map.put(MIMETYPE_FIELD, "application/dotpage");
        map.put("name", page.getPageUrl());
        map.put("description", page.getFriendlyName());
        map.put("extension", "page");
        map.put("isContentlet", true);
        map.put("statusIcons", UtilHTML.getStatusIcons(page));
        map.put("__icon__", IconType.HTMLPAGE.iconName());

        return map;
    }
}
