package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.beans.IconType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilHTML;
import com.liferay.portal.language.LanguageUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a single page to a map view
 * @author jsanca
 */
public class SinglePageToMapViewTransformer implements FieldsToMapTransformer {

    private final HTMLPageAsset page;

    public SinglePageToMapViewTransformer(final HTMLPageAsset page) {
        this.page = page;
    }

    @Override
    public Map<String, Object> asMap() {

        return Sneaky.sneaked(()->toMap()).get();
    }

    private Map<String, Object> toMap() throws DotSecurityException, DotDataException {

        final Map<String, Object> pageMap = new HashMap<>(page.getMap());

        pageMap.put("mimeType",     "application/dotpage");
        pageMap.put("name",         page.getPageUrl());
        pageMap.put("description",  page.getFriendlyName());
        pageMap.put("extension",    "page");
        pageMap.put("isContentlet", true);
        pageMap.put("title",        page.getPageUrl());

        pageMap.put("identifier",   page.getIdentifier());
        pageMap.put("inode",        page.getInode());
        pageMap.put("languageId",   page.getLanguageId());

        final Language lang = APILocator.getLanguageAPI().getLanguage(page.getLanguageId());

        pageMap.put("languageCode", lang.getLanguageCode());
        pageMap.put("countryCode",  lang.getCountryCode());
        pageMap.put("isLocked",     page.isLocked());
        pageMap.put("languageFlag", LanguageUtil.getLiteralLocale(lang.getLanguageCode(), lang.getCountryCode()));

        pageMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(page));
        pageMap.put("statusIcons",   UtilHTML.getStatusIcons(page));
        pageMap.put("hasTitleImage", String.valueOf(page.getTitleImage().isPresent()));

        if(page.getTitleImage().isPresent()) {
            pageMap.put("titleImage", page.getTitleImage().get().variable());
        }

        pageMap.put("__icon__",      IconType.HTMLPAGE.iconName());

        return pageMap;
    }
}
