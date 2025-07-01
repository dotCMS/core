package com.dotcms.ai.api;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.client.AIProxyClient;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.db.EmbeddingsDTO.Builder;
import com.dotcms.ai.db.EmbeddingsFactory;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rest.ContentHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotCorruptedDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;

/**
 * Implementation class for the {@link EmbeddingsAPI} interface.
 * <p>Embeddings are used to convert text into a form that can be processed by machine learning
 * algorithms.</p>
 *
 * @author Daniel Silva
 * @since Mar 27th, 2024
 */
class EmbeddingsAPIImpl implements EmbeddingsAPI {

    private static final Cache<String, Tuple2<Integer, List<Float>>> EMBEDDING_CACHE =
            Caffeine.newBuilder()
                    .expireAfterWrite(
                            Duration.ofSeconds(
                                    ConfigService.INSTANCE
                                            .config()
                                            .getConfigInteger(AppKeys.EMBEDDINGS_CACHE_TTL_SECONDS)))
                    .maximumSize(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_CACHE_SIZE))
                    .build();

    final AppConfig config;

    public EmbeddingsAPIImpl(final Host host) {
        this.config = ConfigService.INSTANCE.config(host);
    }

    @WrapInTransaction
    @Override
    public int deleteByQuery(@NotNull final String deleteQuery, final Optional<String> indexName, final User user) {
        int total=0;
        final int limit = 100;
        int newOffset = 0;

        try {
            for (int i = 0; i < 10000; i++) {
                // searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
                final List<ContentletSearch> searchResults = APILocator
                        .getContentletAPI()
                        .searchIndex(
                                deleteQuery,
                                limit,
                                newOffset,
                                AiKeys.MODDATE,
                                user,
                                false);
                if (searchResults.isEmpty()) {
                    break;
                }
                newOffset += limit;

                for(final ContentletSearch row : searchResults) {
                    final String esId = row.getId();
                    final Builder dto = new EmbeddingsDTO.Builder().withIdentifier(row.getIdentifier());

                    final long languageId = Try.of(() -> esId.split("_")[1]).map(Long::parseLong).getOrElse(-1L);
                    if (languageId > 0) {
                        dto.withLanguage((int)languageId);
                    } else {
                        dto.withInode(row.getInode());
                    }

                    indexName.ifPresent(dto::withIndexName);
    
                    total += (deleteEmbedding(dto.build()) > 0) ? 1 : 0;
                }
            }

            return total;
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void shutdown() {

        Try.run(()->DotConcurrentFactory.getInstance().shutdown(OPEN_AI_THREAD_POOL_KEY));
    }

    @Override
    @WrapInTransaction
    public int deleteEmbedding(@NotNull final EmbeddingsDTO dto) {
        return EmbeddingsFactory.impl.get().deleteEmbeddings(dto);
    }

    @WrapInTransaction
    @Override
    public boolean generateEmbeddingsForContent(@NotNull final Contentlet contentlet,
                                                final List<Field> tryFields,
                                                final String indexName) {

        final List<Field> fields = tryFields.isEmpty()
                ? ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet)
                : tryFields;

        if (fields.isEmpty()) {
            Logger.warn(this, String.format("No valid fields to embed for Contentlet ID '%s' of type " +
                            "'%s' with title '%s'", contentlet.getIdentifier(),
                    contentlet.getContentType().variable(), contentlet.getTitle()));
            return false;
        }

        final Optional<String> content = ContentToStringUtil.impl.get().parseFields(contentlet, fields);
        if (content.isEmpty() || UtilMethods.isEmpty(content.get())) {
            final String message = "Something is wrong with the content, and the Contentlet cannot be embedded:\n" +
                    (tryFields.isEmpty() ? "Found fields from guessing:" + fields.stream().map(Field::variable) :
                            "Found fields from configuration:" + tryFields.stream().map(Field::variable));

            Logger.warn(this, message);
            return false;
        }

        EmbeddingsCallStrategy.resolveStrategy().embed(this, contentlet, content.get(), indexName);

        return true;
    }

    @Override
    public boolean generateEmbeddingsForContent(@NotNull final Contentlet contentlet,
                                                final String velocityTemplate,
                                                final String indexName) {

        if (UtilMethods.isEmpty(velocityTemplate)) {
            return false;
        }

        final Context ctx = VelocityContextFactory.getMockContext(contentlet);
        final String textToEmbed = Try.of(() -> VelocityUtil.eval(velocityTemplate, ctx)).getOrNull();

        if (UtilMethods.isEmpty(textToEmbed)) {
            return false;
        }

        final Optional<String> parsed = ContentToStringUtil.impl.get().isHtml(textToEmbed)
                ? ContentToStringUtil.impl.get().parseHTML(textToEmbed)
                : Optional.of(textToEmbed);
        if (parsed.isEmpty()) {
            return false;
        }

        DotConcurrentFactory.getInstance().getSubmitter(OPEN_AI_THREAD_POOL_KEY).submit(new EmbeddingsRunner(this, contentlet, parsed.get(), indexName));

        return true;
    }

    @WrapInTransaction
    @Override
    public Map<String, List<Field>> parseTypesAndFields(final String typeAndFieldParam) {
        if (UtilMethods.isEmpty(typeAndFieldParam)) {
            return Map.of();
        }

        final Map<String, List<Field>> typesAndFields = new HashMap<>();
        final String[] typeFieldArr = typeAndFieldParam.trim().split("[\\r?\\n,]");

        for (final String typeField : typeFieldArr) {
            final String[] typeOptField = typeField.trim().split("\\.");

            final Optional<ContentType> type = Try
                    .of(() -> APILocator
                            .getContentTypeAPI(APILocator.systemUser())
                            .find(typeOptField[0]))
                    .toJavaOptional();
            if (type.isEmpty()) {
                continue;
            }

            final List<Field> fields = typesAndFields.getOrDefault(type.get().variable(), new ArrayList<>());
            final Optional<Field> field = Try
                    .of(() -> type.get()
                            .fields()
                            .stream()
                            .filter(f -> typeOptField.length > 1 && Objects.requireNonNull(f.variable()).equalsIgnoreCase(typeOptField[1]))
                            .findFirst())
                    .getOrElse(Optional.empty());
            field.ifPresent(fields::add);

            typesAndFields.put(type.get().variable(), fields);
        }

        return typesAndFields;
    }

    @Override
    public JSONObject reduceChunksToContent(final EmbeddingsDTO searcher, final List<EmbeddingsDTO> searchResults) {
        final long startTime = System.currentTimeMillis();
        final Map<String, JSONObject> reducedResults = new LinkedHashMap<>();

        for (final EmbeddingsDTO result : searchResults) {
            final JSONObject contentObject = reducedResults.getOrDefault(
                    result.inode,
                    dtoToContentJson(result, searcher.user));

            contentObject.getAsMap().computeIfAbsent(AiKeys.TITLE, k -> result.title);

            final JSONArray matches = contentObject.optJSONArray(AiKeys.MATCHES) == null
                    ? new JSONArray()
                    : contentObject.optJSONArray(AiKeys.MATCHES);
            final JSONObject match = new JSONObject();
            match.put(AiKeys.DISTANCE, result.threshold);
            match.put(AiKeys.EXTRACTED_TEXT, UtilMethods.truncatify(result.extractedText, 255));
            matches.add(match);
            contentObject.put(AiKeys.MATCHES, matches);
            reducedResults.putIfAbsent(result.inode,contentObject);
        }

        final long count = APILocator.getDotAIAPI().getEmbeddingsAPI().countEmbeddings(searcher);
        final JSONObject map = new JSONObject();
        map.put(AiKeys.TIME_TO_EMBEDDINGS, System.currentTimeMillis() - startTime + "ms");
        map.put(AiKeys.TOTAL, searchResults.size());
        map.put(AiKeys.QUERY, searcher.query);
        map.put(AiKeys.THRESHOLD, searcher.threshold);
        map.put(AiKeys.DOT_CMS_RESULTS, reducedResults.values());
        map.put(AiKeys.OPERATOR, searcher.operator);
        map.put(AiKeys.OFFSET, searcher.offset);
        map.put(AiKeys.LIMIT, searcher.limit);
        map.put(AiKeys.COUNT, count);

        return map;
    }

    @Override
    public JSONObject searchForContent(final EmbeddingsDTO searcher) {
        final long startTime = System.currentTimeMillis();

        final List<EmbeddingsDTO> searchResults = getEmbeddingResults(searcher);
        final JSONObject reducedResults = reduceChunksToContent(searcher, searchResults);

        final long totalTime = System.currentTimeMillis() - startTime;
        reducedResults.put(AiKeys.TIME_TO_EMBEDDINGS, totalTime + "ms");

        return reducedResults;
    }

    @Override
    public List<EmbeddingsDTO> getEmbeddingResults(final EmbeddingsDTO searcher) {
        final EmbeddingsDTO newSearcher = getSearcher(searcher);
        return EmbeddingsFactory.impl.get().searchEmbeddings(newSearcher);
    }

    @Override
    public long countEmbeddings(final EmbeddingsDTO searcher) {
        final EmbeddingsDTO newSearcher = getSearcher(searcher);
        return EmbeddingsFactory.impl.get().countEmbeddings(newSearcher);
    }

    @Override
    public Map<String, Map<String, Object>> countEmbeddingsByIndex() {
        return EmbeddingsFactory.impl.get().countEmbeddingsByIndex();
    }

    @Override
    public void dropEmbeddingsTable() {
        EmbeddingsFactory.impl.get().dropVectorDbTable();
    }

    @Override
    @WrapInTransaction
    public void initEmbeddingsTable() {
        EmbeddingsFactory.impl.get().initVector();
    }

    @Override
    public Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(@NotNull final String content, final String userId) {
        return pullOrGenerateEmbeddings("N/A", content, userId);
    }

    @WrapInTransaction
    @Override
    public Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(final String contentId,
                                                                 @NotNull final String content,
                                                                 final String userId) {
        if (UtilMethods.isEmpty(content)) {
            return Tuple.of(0, List.of());
        }

        final String hashed = StringUtils.hashText(content);
        final Tuple2<Integer, List<Float>> cachedEmbeddings = EMBEDDING_CACHE.getIfPresent(hashed);
        if (cachedEmbeddings != null && !cachedEmbeddings._2.isEmpty()) {
            return cachedEmbeddings;
        }

        final List<Integer> tokens = EncodingUtil.get()
                .getEncoding()
                .map(encoding -> encoding.encode(content))
                .orElse(List.of());
        if (tokens.isEmpty()) {
            config.debugLogger(this.getClass(), () -> String.format("No tokens for content ID '%s' were encoded: %s", contentId, content));
            return Tuple.of(0, List.of());
        }

        final Tuple3<String, Integer, List<Float>> dbEmbeddings =
                EmbeddingsFactory.impl.get().searchExistingEmbeddings(content);
        if (dbEmbeddings != null && !dbEmbeddings._3.isEmpty()) {
            if (!AiKeys.CACHE.equalsIgnoreCase(dbEmbeddings._1)) {
                saveEmbeddingsForCache(content, Tuple.of(dbEmbeddings._2, dbEmbeddings._3));
            }
            EMBEDDING_CACHE.put(hashed, Tuple.of(dbEmbeddings._2, dbEmbeddings._3));
            return Tuple.of(dbEmbeddings._2, dbEmbeddings._3);
        }

        final Tuple2<Integer, List<Float>> openAiEmbeddings = Tuple.of(
                tokens.size(),
                sendTokensToOpenAI(contentId, tokens, userId));
        saveEmbeddingsForCache(content, openAiEmbeddings);
        EMBEDDING_CACHE.put(hashed, openAiEmbeddings);

        return openAiEmbeddings;
    }

    @CloseDBIfOpened
    @Override
    public boolean embeddingExists(final String inode, final String indexName, final String extractedText) {
        return EmbeddingsFactory.impl.get().embeddingExists(inode, indexName, extractedText);
    }

    @WrapInTransaction
    @Override
    public void saveEmbeddings(final EmbeddingsDTO embeddings) {
        EmbeddingsFactory.impl.get().saveEmbeddings(embeddings);
    }

    @WrapInTransaction
    @Override
    public int deleteEmbeddings(final EmbeddingsDTO dto) {
        return EmbeddingsFactory.impl.get().deleteEmbeddings(dto);
    }

    private JSONObject dtoToContentJson(final EmbeddingsDTO dto, final User user) {
        return Try.of(() ->
                ContentHelper.getInstance().contentletToJSON(
                        APILocator.getContentletAPI().find(dto.inode, user, true),
                        HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                        "false",
                        user,
                        false)
        ).andThenTry(() ->
                new JSONObject(APILocator.getContentletAPI().find(dto.inode, user, true).getMap())
        ).andThenTry(() ->
                new JSONObject(
                        Map.of(
                                AiKeys.INODE, dto.inode,
                                AiKeys.IDENTIFIER, dto.identifier,
                                AiKeys.TITLE, dto.title,
                                AiKeys.LANGUAGE, dto.language,
                                AiKeys.INDEX, dto.indexName,
                                AiKeys.CONTENT_TYPE, new JSONArray(dto.contentType))))
                .getOrElse(JSONObject::new);
    }

    private void saveEmbeddingsForCache(final String content, final Tuple2<Integer, List<Float>> embeddings) {
        final EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType(AiKeys.CACHE)
                .withTokenCount(embeddings._1)
                .withInode(AiKeys.CACHE)
                .withLanguage(0)
                .withTitle(AiKeys.CACHE)
                .withIdentifier(AiKeys.CACHE)
                .withHost(AiKeys.CACHE)
                .withExtractedText(content)
                .withIndexName(AiKeys.CACHE)
                .withEmbeddings(embeddings._2)
                .build();

        saveEmbeddings(embeddingsDTO);
    }

    /**
     * Posts the specified list of tokens to the OpenAI Embeddings Endpoint and returns the
     * resulting embeddings. Such tokens are the encoded data of a given Contentlet.
     *
     * @param contentId The ID of the Contentlet that will be sent to the OpenAI Endpoint.
     * @param tokens    The encoded tokens representing the indexable data of a Contentlet.
     * @param userId    The ID of the user making the request.
     *
     * @return A {@link List} of {@link Float} values representing the embeddings.
     */
    private List<Float> sendTokensToOpenAI(final String contentId,
                                           @NotNull final List<Integer> tokens,
                                           final String userId) {
        final JSONObject json = new JSONObject();
        json.put(AiKeys.MODEL, config.getEmbeddingsModel().getCurrentModel());
        json.put(AiKeys.INPUT, tokens);
        config.debugLogger(this.getClass(), () -> String.format("Content tokens for content ID '%s': %s", contentId, tokens));
        final String responseString = AIProxyClient.get()
                .callToAI(JSONObjectAIRequest.quickEmbeddings(config, json, userId))
                .getResponse();
        config.debugLogger(this.getClass(), () -> String.format("OpenAI Response for content ID '%s': %s",
                contentId, responseString.replace("\n", BLANK)));
        final JSONObject jsonResponse = Try.of(() -> new JSONObject(responseString)).getOrElseThrow(e -> {
            Logger.error(this, "OpenAI Response String is not a valid JSON", e);
            config.debugLogger(this.getClass(), () -> String.format("Invalid JSON Response: %s", responseString));
            return new DotCorruptedDataException(e);
        });
        if (jsonResponse.containsKey(AiKeys.ERROR)) {
            final String errorMsg = jsonResponse.getJSONObject(AiKeys.ERROR).getString(AiKeys.MESSAGE);
            throw new DotRuntimeException(errorMsg);
        }
        final JSONObject data = this.getDataFromOpenAIResponse(contentId, jsonResponse);
        return this.getEmbeddingsFromJSON(contentId, data);
    }

    /**
     * Retrieves the data attribute from the OpenAI JSON response.
     *
     * @param contentId    The ID of the Contentlet that the JSON response belongs to.
     * @param jsonResponse The JSON response from OpenAI as a {@link JSONObject}.
     *
     * @return The data attribute from the JSON response in the form of a {@link JSONObject}.
     */
    private JSONObject getDataFromOpenAIResponse(final String contentId, final JSONObject jsonResponse) {
        try {
            return (JSONObject) jsonResponse.getJSONArray(AiKeys.DATA).get(0);
        } catch (final JSONException e) {
            Logger.error(this, String.format("Failed to read 'data' attribute from JSON response to content ID '%s'. Received: " +
                    "< %s >. Error cause: %s", contentId, jsonResponse.getString(AiKeys.DATA), ExceptionUtil.getErrorMessage(e)), e);
            throw e;
        }
    }

    /**
     * Extracts the embeddings from the OpenAI JSON response.
     *
     * @param contentId The ID of the Contentlet that the JSON response belongs to.
     * @param data      The {@link JSONObject} containing the embeddings.
     *
     * @return A {@link List} of {@link Float} values representing the embeddings.
     */
    @SuppressWarnings("unchecked")
    private List<Float> getEmbeddingsFromJSON(final String contentId, final JSONObject data) {
        try {
            return (List<Float>) data.getJSONArray(AiKeys.EMBEDDING).stream().map(val -> {

                final Double x = (Double) val;
                return x.floatValue();

            }).collect(Collectors.toList());
        } catch (final JSONException e) {
            Logger.error(this, String.format("Failed to read 'embedding' attribute from JSON response to content ID '%s'. Received: " +
                    "< %s >. Error cause: %s", contentId, data.getString(AiKeys.EMBEDDING), ExceptionUtil.getErrorMessage(e)), e);
            throw e;
        }
    }

    private EmbeddingsDTO getSearcher(final EmbeddingsDTO searcher) {
        final List<Float> queryEmbeddings = pullOrGenerateEmbeddings(
                searcher.query,
                UtilMethods.extractUserIdOrNull(searcher.user))._2;
        return EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings).build();
    }

}
