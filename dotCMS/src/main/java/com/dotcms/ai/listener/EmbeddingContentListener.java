package com.dotcms.ai.listener;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.dotcms.ai.db.EmbeddingsDTO.ALL_INDICES;

/**
 * This class listens to various contentlet events and performs actions based on those events.
 * It implements the ContentletListener interface and overrides its methods to provide custom functionality.
 * The class uses a LoadingCache to store and retrieve configuration JSON objects for different hosts.
 * It also provides methods to add contentlets to indexes and delete them from indexes based on certain conditions.
 */
public class EmbeddingContentListener implements ContentletListener<Contentlet> {

    @Override
    public String getId() {
        return getClass().getCanonicalName();
    }

    private final LoadingCache<Host, JSONObject> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build(this::getConfigJson);

    @Override
    public void onModified(ContentletPublishEvent<Contentlet> contentletPublishEvent) {
        final Contentlet contentlet = contentletPublishEvent.getContentlet();
        if (contentletPublishEvent.isPublish()) {
            logEvent("onModified - PublishEvent:true ", contentlet);
        } else {
            logEvent("onModified - PublishEvent:false ", contentlet);
            deleteFromIndexes(contentlet);
        }
    }

    @Subscriber
    public void onPublish(final ContentletPublishEvent<Contentlet> contentletPublishEvent) {
        final Contentlet contentlet = contentletPublishEvent.getContentlet();
        if (contentletPublishEvent.isPublish()) {
            logEvent("onPublish - PublishEvent:true", contentlet);
            addToIndexesIfNeeded(contentlet);
        } else {
            logEvent("onPublish - PublishEvent:false", contentlet);
            deleteFromIndexes(contentlet);
        }
    }

    @Subscriber
    @Override
    public void onArchive(final ContentletArchiveEvent<Contentlet> contentletArchiveEvent) {
        final Contentlet contentlet = contentletArchiveEvent.getContentlet();
        logEvent("onArchive", contentlet);
        deleteFromIndexes(contentlet);
    }

    @Subscriber
    @Override
    public void onDeleted(final ContentletDeletedEvent<Contentlet> contentletDeletedEvent) {
        final Contentlet contentlet = contentletDeletedEvent.getContentlet();
        logEvent("onDeleted", contentlet);
        deleteFromIndexes(contentlet);
    }

    /**
     * JSONObject that has a list of indexes and the content types that should be indexed in them.
     *
     * @param host
     * @return
     */
    private JSONObject getConfigJson(final Host host) {
        return Try
                .of(() -> new JSONObject(ConfigService.INSTANCE.config(host).getConfig(AppKeys.LISTENER_INDEXER)))
                .onFailure(e -> Logger.warn(
                        getClass(),
                        "error in json config from app:" + e.getMessage()))
                .getOrElse(new JSONObject());
    }

    /**
     * Adds the content to the embeddings index based on the JSON configuration in the app.  The JSON key is the
     * indexName and the property is a comma or br delimited string of contentType.field to index
     *
     * @param contentlet
     */
    private void addToIndexesIfNeeded(final Contentlet contentlet) {
        final String contentType = contentlet.getContentType().variable();
        if (contentType == null) {
            return;
        }

        final Host host = Try
                .of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false))
                .getOrElse(APILocator.systemHost());
        final JSONObject config = cache.get(host);

        for(final Entry<String, Object> entry : (Set<Entry<String, Object>>) config.entrySet()) {
            final String indexName = entry.getKey();
            final Map<String, List<Field>> typesAndFields =
                    EmbeddingsAPI.impl().parseTypesAndFields((String) entry.getValue());
            typesAndFields.entrySet()
                    .stream()
                    .filter(e -> contentType.equalsIgnoreCase(e.getKey()))
                    .forEach(e ->
                            EmbeddingsAPI.impl().generateEmbeddingsForContent(contentlet, e.getValue(), indexName));
        }
    }

    /**
     * If a contentlet is unpublished, we delete it from the dot_embeddings no matter what index it is part of
     * @param contentlet
     */
    private void deleteFromIndexes(final Contentlet contentlet) {
        final EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withIdentifier(contentlet.getIdentifier())
                .withLanguage(contentlet.getLanguageId())
                .withIndexName(ALL_INDICES)
                .build();
        EmbeddingsAPI.impl().deleteEmbedding(dto);
    }

    private void logEvent(final String eventType, final Contentlet contentlet) {
        Logger.info(
               getClass().getCanonicalName(),
                "GOT " + eventType + " for content: " + contentlet.getTitle() + " id:" + contentlet.getIdentifier());
    }

}
