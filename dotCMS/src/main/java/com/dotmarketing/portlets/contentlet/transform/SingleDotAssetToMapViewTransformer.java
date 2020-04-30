package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;

import java.util.Map;

/**
 * Transform a single dotAsset to a map view
 * @author jsanca
 */
public class SingleDotAssetToMapViewTransformer implements FieldsToMapTransformer {

    private final Contentlet dotAsset;

    public SingleDotAssetToMapViewTransformer(final Contentlet dotAsset) {
        this.dotAsset = dotAsset;
    }

    @Override
    public Map<String, Object> asMap() {

        return Sneaky.sneaked(()->toMap()).get();
    }

    private Map<String, Object> toMap() throws DotSecurityException, DotDataException {

        final Map<String, Object> dotAssetMap = new ContentletToMapTransformer(dotAsset).toMaps().get(0);
        final Identifier identifier       = APILocator.getIdentifierAPI().find(dotAsset.getVersionId());
        final String fileName = Try.of(()->dotAsset.getBinary("asset").getName()).getOrElse("unknown");

        dotAssetMap.put("mimeType", APILocator.getFileAssetAPI().getMimeType(fileName));
        dotAssetMap.put("name",     fileName);
        dotAssetMap.put("fileName", fileName);
        dotAssetMap.put("title",    fileName);
        dotAssetMap.put("friendyName", StringPool.BLANK);

        dotAssetMap.put("extension",     UtilMethods.getFileExtension(fileName));
        dotAssetMap.put("path",          "/dA/" + identifier.getId() + StringPool.SLASH);
        dotAssetMap.put("type",          "dotasset");

        final Host hoster = APILocator.getHostAPI().find(identifier.getHostId(), APILocator.systemUser(), false);
        dotAssetMap.put("hostName",      hoster.getHostname());

        dotAssetMap.put("size",          Try.of(()->dotAsset.getBinary("asset").length()).getOrElse(0l));
        dotAssetMap.put("publishDate",   dotAsset.getModDate());

        dotAssetMap.put("isContentlet",  true);

        dotAssetMap.put("identifier",   dotAsset.getIdentifier());
        dotAssetMap.put("inode",        dotAsset.getInode());
        dotAssetMap.put("isLocked",     dotAsset.isLocked());
        dotAssetMap.put("isContentlet", true);

        final Language language = APILocator.getLanguageAPI().getLanguage(dotAsset.getLanguageId());

        dotAssetMap.put("languageId",   language.getId());
        dotAssetMap.put("languageCode", language.getLanguageCode());
        dotAssetMap.put("countryCode",  language.getCountryCode());
        dotAssetMap.put("languageFlag", LanguageUtil.getLiteralLocale(language.getLanguageCode(), language.getCountryCode()));

        dotAssetMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(dotAsset));
        dotAssetMap.put("statusIcons",    UtilHTML.getStatusIcons(dotAsset));
        dotAssetMap.put("hasTitleImage",  String.valueOf(dotAsset.getTitleImage().isPresent()));
        if(dotAsset.getTitleImage().isPresent()) {
            dotAssetMap.put("titleImage", dotAsset.getTitleImage().get().variable());
        }

        dotAssetMap.put("__icon__",       UtilHTML.getIconClass(dotAsset));

        return dotAssetMap;
    }
}
