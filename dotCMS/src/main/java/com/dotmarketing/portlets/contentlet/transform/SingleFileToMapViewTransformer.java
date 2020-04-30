package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a single file to a map view
 * @author jsanca
 */
public class SingleFileToMapViewTransformer implements FieldsToMapTransformer {

    private final FileAsset fileAsset;

    public SingleFileToMapViewTransformer(final FileAsset fileAsset) {
        this.fileAsset = fileAsset;
    }

    @Override
    public Map<String, Object> asMap() {

        return Sneaky.sneaked(()->toMap()).get();
    }

    private Map<String, Object> toMap() throws DotSecurityException, DotDataException {

        final Map<String, Object> fileMap = new HashMap<>(fileAsset.getMap());
        final Identifier identifier       = APILocator.getIdentifierAPI().find(fileAsset.getVersionId());

        fileMap.put("mimeType",  APILocator.getFileAssetAPI().getMimeType(fileAsset.getUnderlyingFileName()));
        fileMap.put("name",     identifier.getAssetName());
        fileMap.put("title",    identifier.getAssetName());
        fileMap.put("fileName", identifier.getAssetName());
        fileMap.put("title",    fileAsset.getFriendlyName());
        fileMap.put("description", fileAsset instanceof Contentlet ?
                ((Contentlet)fileAsset).getStringProperty(FileAssetAPI.DESCRIPTION)
                : StringPool.BLANK);
        fileMap.put("extension", UtilMethods.getFileExtension(fileAsset.getUnderlyingFileName()));
        fileMap.put("path",      fileAsset.getPath());
        fileMap.put("type",      "file_asset");

        final Host hoster = APILocator.getHostAPI().find(identifier.getHostId(), APILocator.systemUser(), false);
        fileMap.put("hostName", hoster.getHostname());

        fileMap.put("size",        fileAsset.getFileSize());
        fileMap.put("publishDate", fileAsset.getIDate());

        // BEGIN GRAZIANO issue-12-dnd-template
        fileMap.put(
                "parent",
                fileAsset.getParent() != null ? fileAsset
                        .getParent() : StringPool.BLANK);

        fileMap.put("identifier", fileAsset.getIdentifier());
        fileMap.put("inode",      fileAsset.getInode());
        fileMap.put("isLocked",   fileAsset.isLocked());
        fileMap.put("isContentlet", true);

        final Language language = APILocator.getLanguageAPI().getLanguage(fileAsset.getLanguageId());

        fileMap.put("languageId",   language.getId());
        fileMap.put("languageCode", language.getLanguageCode());
        fileMap.put("countryCode",  language.getCountryCode());
        fileMap.put("languageFlag", LanguageUtil.getLiteralLocale(language.getLanguageCode(), language.getCountryCode()));

        fileMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(fileAsset));
        fileMap.put("statusIcons",    UtilHTML.getStatusIcons(fileAsset));
        fileMap.put("hasTitleImage",  String.valueOf(fileAsset.getTitleImage().isPresent()));
        if(fileAsset.getTitleImage().isPresent()) {
            fileMap.put("titleImage", fileAsset.getTitleImage().get().variable());
        }

        fileMap.put("__icon__",       UtilHTML.getIconClass(fileAsset ));

        return fileMap;
    }
}
