package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.liferay.util.StringPool;

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

    public FilesCollector() {
        this(APILocator.getFileAssetAPI(), APILocator.getContentletAPI());
    }

    public FilesCollector(final FileAssetAPI fileAssetAPI,
                          final ContentletAPI contentletAPI) {

        this.fileAssetAPI = fileAssetAPI;
        this.contentletAPI = contentletAPI;
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
            });
        }

        collectorPayloadBean.put(OBJECT,  fileObject);
        collectorPayloadBean.put(URL, uri);
        collectorPayloadBean.put(HOST, host);
        collectorPayloadBean.put(LANGUAGE, language);
        collectorPayloadBean.put(SITE, null != site?site.getIdentifier():StringPool.UNKNOWN);
        collectorPayloadBean.put(EVENT_TYPE, EventType.FILE_REQUEST.getType());

        return collectorPayloadBean;
    }

    private Optional<Contentlet> getFileAsset(String uri, Host host, Long languageId) {
        try {
            if (uri.endsWith(".dotsass")) {
                final String actualUri = uri.substring(0, uri.lastIndexOf('.')) + ".scss";
                return Optional.ofNullable(this.fileAssetAPI.getFileByPath(actualUri, host, languageId, true));
            } else if (uri.startsWith("/dA") || uri.startsWith("/contentAsset") || uri.startsWith("/dotAsset")) {
                final String[] split = uri.split(StringPool.FORWARD_SLASH);
                final String id = uri.startsWith("/contentAsset") ? split[3] : split[2];
                return getFileAsset(languageId, id);
            } else {
                return Optional.ofNullable(this.fileAssetAPI.getFileByPath(uri, host, languageId, true));
            }
        } catch (DotDataException | DotSecurityException e) {
            return Optional.empty();
        }
    }

    private Optional<Contentlet> getFileAsset(final Long languageId, final String id) throws DotDataException, DotSecurityException {

        return Optional.ofNullable(contentletAPI.findContentletByIdentifier(id, true, languageId,
                APILocator.systemUser(), false));
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
