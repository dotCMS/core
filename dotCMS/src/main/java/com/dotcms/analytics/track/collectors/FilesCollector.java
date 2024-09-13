package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.csspreproc.DotLibSassCompiler;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This collector collects the file information
 * @author jsanca
 */
public class FilesCollector implements Collector {

    private final FileAssetAPI fileAssetAPI;
    private final HostAPI hostAPI;
    private final ContentletAPI contentletAPI;

    public FilesCollector() {
        this(APILocator.getFileAssetAPI(),
                APILocator.getHostAPI(), APILocator.getContentletAPI());
    }

    public FilesCollector(final FileAssetAPI fileAssetAPI,
                          final HostAPI hostAPI, final ContentletAPI contentletAPI) {

        this.fileAssetAPI = fileAssetAPI;
        this.hostAPI = hostAPI;
        this.contentletAPI = contentletAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return FilesRequestMatcher.FILES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectionCollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectionCollectorPayloadBean collectionCollectorPayloadBean) {

        // we use the same event just collect more information async
        final CollectorPayloadBean collectorPayloadBean = collectionCollectorPayloadBean.first();
        final String uri = (String)collectorContextMap.get("uri");
        final String host = (String)collectorContextMap.get("host");
        final String siteId = (String)collectorContextMap.get("siteId");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final Map<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            getFileAsset(uri, site, languageId).ifPresent(fileAsset -> {
                pageObject.put("id", fileAsset.getIdentifier());
                pageObject.put("title", fileAsset.getTitle());
                pageObject.put("url", uri);
            });
        }

        final StringWriter writer = new StringWriter();
        Try.run(()-> DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValue(writer, pageObject));
        collectorPayloadBean.put("object",  writer.toString());
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("host", host);
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", siteId);
        collectorPayloadBean.put("event_type", EventType.FILE_REQUEST.getType());

        return collectionCollectorPayloadBean;
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
