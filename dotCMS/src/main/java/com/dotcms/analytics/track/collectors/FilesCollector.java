package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.PageMode;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;


/**
 * This collector collects the file information
 * @author jsanca
 */
public class FilesCollector implements Collector {

    private final FileAssetAPI fileAssetAPI;
    private final ContentletAPI contentletAPI;
    private final ShortyIdAPI shortyIdAPI;

    public FilesCollector() {
        this(APILocator.getFileAssetAPI(), APILocator.getContentletAPI(), APILocator.getShortyAPI());
    }

    public FilesCollector(final FileAssetAPI fileAssetAPI,
                          final ContentletAPI contentletAPI,
                          final ShortyIdAPI shortyIdAPI) {

        this.fileAssetAPI = fileAssetAPI;
        this.contentletAPI = contentletAPI;
        this.shortyIdAPI = shortyIdAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return FilesRequestMatcher.FILES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        final String host = (String)collectorContextMap.get(CollectorContextMap.HOST);
        final Host site = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);
        final Long languageId = (Long)collectorContextMap.get(CollectorContextMap.LANG_ID);
        final String language = (String)collectorContextMap.get(CollectorContextMap.LANG);

        final HashMap<String, String> fileObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(site) && Objects.nonNull(languageId)) {

            getFileAsset(uri, site, languageId).ifPresent(fileAsset -> {
                fileObject.put(ID, fileAsset.getIdentifier());
                fileObject.put(TITLE, fileAsset.getTitle());
                fileObject.put(URL, uri);
                fileObject.put(CONTENT_TYPE_ID, fileAsset.getContentType().id());
                fileObject.put(CONTENT_TYPE_NAME, fileAsset.getContentType().name());
                fileObject.put(CONTENT_TYPE_VAR_NAME, fileAsset.getContentType().variable());
                fileObject.put(BASE_TYPE, fileAsset.getContentType().baseType().name());
                fileObject.put(LIVE,    String.valueOf(Try.of(()->fileAsset.isLive()).getOrElse(false)));
                fileObject.put(WORKING, String.valueOf(Try.of(()->fileAsset.isWorking()).getOrElse(false)));
            });
        }

        collectorPayloadBean.put(OBJECT,  fileObject);
        collectorPayloadBean.put(URL, uri);
        collectorPayloadBean.put(SITE_NAME, Objects.nonNull(site)?site.getHostname():host);
        collectorPayloadBean.put(LANGUAGE, language);
        collectorPayloadBean.put(LANGUAGE_ID, languageId);
        collectorPayloadBean.put(SITE_ID, null != site?site.getIdentifier():StringPool.UNKNOWN);
        collectorPayloadBean.put(EVENT_TYPE, EventType.FILE_REQUEST.getType());

        return collectorPayloadBean;
    }

    protected Optional<Contentlet> getFileAsset(String uri, Host host, Long languageId) {
        try {
            if (uri.endsWith(".dotsass")) {
                final String actualUri = uri.substring(0, uri.lastIndexOf('.')) + ".scss";
                return Optional.ofNullable(this.fileAssetAPI.getFileByPath(actualUri, host, languageId, true));
            } else if (uri.startsWith("/dA") || uri.startsWith("/contentAsset") || uri.startsWith("/dotAsset")) {
                final FieldNameIdentifier fieldNameIdentifier = getIdentifierAndFieldName(uri);
                return getFileAsset(languageId, fieldNameIdentifier);
            } else {
                return Optional.ofNullable(this.fileAssetAPI.getFileByPath(uri, host, languageId, true));
            }
        } catch (DotDataException | DotSecurityException e) {
            return Optional.empty();
        }
    }

    private static FieldNameIdentifier getIdentifierAndFieldName(String uri) {
        final String[] split = uri.split(StringPool.FORWARD_SLASH);

        final int idIndex =  uri.startsWith("/contentAsset") ? 3 : 2;
        final int fieldNameIndex =  uri.startsWith("/contentAsset") ? 4 : 3;

        if (uri.startsWith("/dotAsset")) {
            return new FieldNameIdentifier(split[idIndex], null);
        }

        //return new FieldNameIdentifier(fieldNameIndex < split.length ? getIdentifier(split[idIndex]) : null,
          //      fieldNameIndex < split.length ? split[fieldNameIndex] : null);
        return new FieldNameIdentifier(fieldNameIndex < split.length ? split[idIndex] : null,
              fieldNameIndex < split.length ? split[fieldNameIndex] : null);
    }

    private static String getIdentifier(final String idOrShortId) {
        return APILocator.getShortyAPI().getShorty(idOrShortId)
                .map(shortyId -> shortyId.shortId)
                .orElse(null);
    }

    private Optional<Contentlet> getFileAsset(final Long languageId, final FieldNameIdentifier fieldNameIdentifier)
            throws DotDataException, DotSecurityException {

        if (Objects.isNull(fieldNameIdentifier.identifier)) {
            return Optional.empty();
        }

        final ShortyId shortId = this.shortyIdAPI.getShorty(fieldNameIdentifier.identifier).orElse(null);

        if (Objects.isNull(shortId)) {
            return Optional.empty();
        }

        final Contentlet contentlet = (shortId.type == ShortType.IDENTIFIER)
                ? contentletAPI.findContentletByIdentifier(shortId.longId,
                    PageMode.get().showLive, languageId,
                    APILocator.systemUser(), false)
                : contentletAPI.find(shortId.longId, APILocator.systemUser(), false);

        if (Objects.nonNull(fieldNameIdentifier.fieldName) && Objects.nonNull(contentlet) && !isBinaryField(contentlet)) {
            final String binaryFileId = contentlet.getStringProperty(fieldNameIdentifier.fieldName);

            return Optional.ofNullable(contentletAPI.findContentletByIdentifier(binaryFileId,
                    PageMode.get().showLive, languageId,
                    APILocator.systemUser(), false));
        }

        return Optional.ofNullable(contentlet);
    }

    private static boolean isBinaryField(Contentlet contentlet) {
        return BaseContentType.FILEASSET == contentlet.getBaseType().get() ||
                BaseContentType.DOTASSET == contentlet.getBaseType().get();
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    private static class FieldNameIdentifier {
        final String fieldName;
        final String identifier;

        FieldNameIdentifier(final String identifier, final String fieldName) {
            this.fieldName = fieldName;
            this.identifier = identifier;
        }
    }

}
