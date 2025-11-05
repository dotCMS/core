package com.dotcms.ai.api;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.embeddings.DotPgVectorEmbeddingStore;
import com.dotcms.ai.api.embeddings.EmbeddingIndexRequest;
import com.dotcms.ai.api.embeddings.FixedSizeChunker;
import com.dotcms.ai.api.embeddings.SearchMatch;
import com.dotcms.ai.api.embeddings.TextChunker;
import com.dotcms.ai.api.embeddings.extractor.ContentExtractor;
import com.dotcms.ai.api.embeddings.extractor.ExtractedContent;
import com.dotcms.ai.api.embeddings.retrieval.EmbeddingStoreRetriever;
import com.dotcms.ai.api.embeddings.retrieval.RetrievalQuery;
import com.dotcms.ai.api.embeddings.retrieval.RetrievedChunk;
import com.dotcms.ai.api.embeddings.retrieval.Retriever;
import com.dotcms.ai.api.provider.VendorModelProviderFactory;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.client.AIProxyClient;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.db.EmbeddingsDTO.Builder;
import com.dotcms.ai.db.EmbeddingsFactory;
import com.dotcms.ai.util.AIUtil;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cdi.CDIUtils;
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
import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import com.liferay.util.StringPool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
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
    private final VendorModelProviderFactory modelProviderFactory;
    private final ContentExtractor contentExtractor;
    private final TextChunker textChunker;

    public EmbeddingsAPIImpl(final Host host) {
        this.config = ConfigService.INSTANCE.config(host);
        this.modelProviderFactory = CDIUtils.getBeanThrows(VendorModelProviderFactory.class);
        contentExtractor = CDIUtils.getBeanThrows(ContentExtractor.class);
        textChunker = new FixedSizeChunker(1200, 200); // todo see this should be configurable);
    }

    public static EmbeddingModel defaultOnnxModel() {

        Logger.info(EmbeddingsAPIImpl.class, "Using the default default embedding model Onnx");
        return CDIUtils.getBeanThrows(EmbeddingModel.class, "onnx");
    }

    public int indexOne(final EmbeddingIndexRequest embeddingIndexRequest) throws Exception {

        final String identifier = embeddingIndexRequest.getIdentifier();
        final long languageId   = embeddingIndexRequest.getLanguageId();
        final String vendorModelPath = embeddingIndexRequest.getVendorModelPath();
        final String indexName = Objects.nonNull(embeddingIndexRequest.getIndexName())?embeddingIndexRequest.getIndexName():"default";
        final AiModelConfig modelConfig = embeddingIndexRequest.getModelConfig();
        final EmbeddingModel embeddingModel = Objects.nonNull(vendorModelPath)?
                this.modelProviderFactory.getEmbedding(vendorModelPath, modelConfig):defaultOnnxModel();
        // todo: this could be eventually cached by factory by index and model
        final DotPgVectorEmbeddingStore embeddingStore = DotPgVectorEmbeddingStore.builder()
                .indexName(indexName)
                .dimension(embeddingModel.dimension())
                .build();
        final ExtractedContent extractedContent = this.contentExtractor.extract(identifier, languageId);
        if (extractedContent == null) {
            return 0;
        }

        final String fullText = normalize(extractedContent);
        if (fullText == null || fullText.isEmpty()) {
            return 0;
        }

        final String textHash = sha256(fullText); // optional idempotency helper
        final List<String> chunks = textChunker.chunk(fullText);
        if (chunks.isEmpty()) {
            return 0;
        }

        int chunksIndexed = 0;
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            final String textChunk = chunks.get(chunkIndex);
            final TextSegment segment = buildSegment(extractedContent, textChunk, textHash, chunkIndex);
            embeddingStore.add(embeddingModel.embed(segment).content(), segment);
            chunksIndexed++;
        }
        return chunksIndexed;
    }

    /** Combine title & body to slightly boost recall for short titles. */
    private static String normalize(final ExtractedContent extractedContent) {
        final String title = safe(extractedContent.getTitle());
        final String body  = safe(extractedContent.getText());

        if (title.isEmpty()) {
            return body;
        }
        if (body.isEmpty()) {
            return title;
        }
        return title + "\n\n" + body;
    }

    private static String sha256(final String text) {

        try {

            final HashBuilder hashBuilder = Encryptor.Hashing.sha256();
            return   hashBuilder.append(text.getBytes(StandardCharsets.UTF_8)).buildUnixHash();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(final String value) {
        return (value == null) ? BLANK : value.trim();
    }

    private static TextSegment buildSegment(final ExtractedContent extractedContent,
                                            final String textChunk,
                                            final String textHash,
                                            final int chunkIndex) {
        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("identifier",  extractedContent.getIdentifier());
        metadata.put("host",        extractedContent.getHost());
        metadata.put("variant",     extractedContent.getVariant());
        metadata.put("contentType", extractedContent.getContentType());
        metadata.put("language",    extractedContent.getLanguage());
        metadata.put("title",       extractedContent.getTitle());
        metadata.put("chunkIndex",  chunkIndex);
        metadata.put("textHash",    textHash);
        metadata.put("modelName",   "all-minilm-l6-v2");

        return TextSegment.from(textChunk, new dev.langchain4j.data.document.Metadata(metadata));
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

    @Override
    public SearchContentResponse searchForContent(final SearchForContentRequest searchForContentRequest) {

        Logger.debug(this, ()-> "Doing search for content request: " + searchForContentRequest);
        final AiModelConfig modelConfig = searchForContentRequest.getChatModelConfig();
        final String vendorName = AIUtil.getVendorFromPath(searchForContentRequest.getVendorModelPath());
        final EmbeddingModel embeddingModel = this.modelProviderFactory.getEmbedding(vendorName, modelConfig);
        final EmbeddingStore<TextSegment> store = DotPgVectorEmbeddingStore.builder()
                .indexName(searchForContentRequest.getSearcher().indexName!=null?searchForContentRequest.getSearcher().indexName:"default")
                .dimension(embeddingModel.dimension())
                .operator(SimilarityOperator.fromString(searchForContentRequest.getSearcher().operator))
                .build();

        final Retriever retriever = EmbeddingStoreRetriever.builder()
                .store(store)
                .embeddingModel(embeddingModel)
                .defaultLimit(8) // check all these values
                .maxLimit(64)
                .overfetchFactor(3) // todo: what is this
                .defaultThreshold(0.75) // todo: what is this
                .build();
        final RetrievalQuery.Builder retrievalQueryBuilder = RetrievalQuery.builder();
        retrievalQueryBuilder.prompt(searchForContentRequest.getPrompt());
        Optional.ofNullable(searchForContentRequest.getSearcher().host).ifPresent(retrievalQueryBuilder::site);
        retrievalQueryBuilder.contentTypes(Objects.nonNull(searchForContentRequest.getSearcher().contentType)?
                List.of(searchForContentRequest.getSearcher().contentType):List.of());
        Optional.ofNullable(searchForContentRequest.getSearcher().language).ifPresent(languageId -> retrievalQueryBuilder.languageId(String.valueOf(languageId)));
        //retrievalQueryBuilder.identifier(ragSearchRequest.getIdentifier()) ;
        retrievalQueryBuilder.limit(searchForContentRequest.getSearcher().limit);
        retrievalQueryBuilder.offset(searchForContentRequest.getSearcher().offset);
        retrievalQueryBuilder.threshold(searchForContentRequest.getSearcher().threshold);

        final RetrievalQuery retrievalQuery = retrievalQueryBuilder.build();
        final long startMillis = System.currentTimeMillis();
        final List<RetrievedChunk> chunks = retriever.search(retrievalQuery);
        final long endMillis = System.currentTimeMillis();

        final List<SearchMatch> matches = chunks.stream().map(retrievedChunk -> {
            return SearchMatch.builder()
                    .withId(retrievedChunk.getDocId())
                    .withScore(retrievedChunk.getScore())
                    .withTitle(retrievedChunk.getTitle())
                    .withSnippet(truncate(retrievedChunk.getText(), 600))
                    .withIdentifier(retrievedChunk.getIdentifier())
                    .withContentType(retrievedChunk.getContentType())
                    .withLanguage(retrievedChunk.getLanguageId())
                    .withHost(retrievalQuery.getSite())
                    .withVariant(retrievedChunk.getFieldVar())
                    .withUrl(retrievedChunk.getUrl()).build();
        }).collect(Collectors.toList());

        return SearchContentResponse.of(matches, matches.size(), Map.of("latencyMs", (endMillis - startMillis)));
    }

    private static String truncate(final String text, final int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 3)) + "...";
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
