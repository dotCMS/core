package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDB;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.db.EmbeddingsDTO.Builder;
import com.dotcms.ai.util.*;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rest.ContentResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
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
import java.util.*;
import java.util.stream.Collectors;


class EmbeddingsAPIImpl implements EmbeddingsAPI {

    private static final Cache<String, Tuple2<Integer, List<Float>>> embeddingCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_CACHE_TTL_SECONDS)))
            .maximumSize(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_CACHE_SIZE))
            .build();
    private static final String CACHE = "cache";
    private static final String MATCHES = "matches";

    final AppConfig config;

    public EmbeddingsAPIImpl(final Host host) {
        this.config = ConfigService.INSTANCE.config(host);
    }

    @Override
    public void shutdown() {
        Try.run(OpenAIThreadPool::shutdown);
    }

    @Override
    public int deleteByQuery(@NotNull final  String deleteQuery, final Optional<String> indexName, final User user) {
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
                                "moddate",
                                user,
                                false);
                if (searchResults.isEmpty()) {
                    break;
                }
                newOffset += limit;

                for(final ContentletSearch row : searchResults){
                    final String esId = row.getId();
                    final Builder dto = new EmbeddingsDTO.Builder().withIdentifier(row.getIdentifier());

                    final long languageId = Try.of(()->esId.split("_")[1]).map(Long::parseLong).getOrElse(-1L);
                    if (languageId > 0) {
                        dto.withLanguage((int)languageId);
                    } else {
                        dto.withInode(row.getInode());
                    }

                    indexName.ifPresent(dto::withIndexName);

                    total += (EmbeddingsAPI.impl().deleteEmbedding(dto.build()) > 0) ? 1 : 0;
                }
            }

            return total;
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }


    @Override
    public int deleteEmbedding(@NotNull final EmbeddingsDTO dto) {
        return EmbeddingsDB.impl.get().deleteEmbeddings(dto);
    }

    @Override
    public boolean generateEmbeddingsforContent(@NotNull final Contentlet contentlet,
                                                final List<Field> tryFields,
                                                final String indexName) {

        final List<Field> fields = tryFields.isEmpty()
                ? ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet)
                : tryFields;

        final Optional<String> content = ContentToStringUtil.impl.get().parseFields(contentlet, fields);
        if (content.isEmpty() || UtilMethods.isEmpty(content.get())) {
            Logger.info(
                    EmbeddingsAPIImpl.class,
                    "No valid fields to embed for:"
                            + contentlet.getContentType().variable()
                            + " id:"
                            + contentlet.getIdentifier()
                            + " title:"
                            + contentlet.getTitle());
            return false;
        }

        OpenAIThreadPool.submit(new EmbeddingsRunner(this, contentlet, content.get(), indexName));

        return true;
    }

    @Override
    public boolean generateEmbeddingsforContent(@NotNull final Contentlet contentlet,
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

        OpenAIThreadPool.submit(new EmbeddingsRunner(this, contentlet, parsed.get(), indexName));

        return true;
    }

    @Override
    public  Map<String, List<Field>> parseTypesAndFields(final String typeAndFieldParam) {

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
                            .filter(f -> f.variable().equalsIgnoreCase(typeOptField[1]))
                            .findFirst())
                    .getOrElse(Optional.empty());
            field.ifPresent(fields::add);

            typesAndFields.put(type.get().variable(), fields);
        }

        return typesAndFields;
    }

    @Override
    public JSONObject reduceChunksToContent(EmbeddingsDTO searcher, final List<EmbeddingsDTO> searchResults) {
        long startTime = System.currentTimeMillis();

        Map<String, JSONObject> reducedResults = new LinkedHashMap<>();

        for (EmbeddingsDTO result : searchResults) {
            JSONObject contentObject = reducedResults.getOrDefault(result.inode, dtoToContentJson(result, searcher.user));

            contentObject.getAsMap().computeIfAbsent("title", k -> result.title);

            JSONArray matches = contentObject.optJSONArray(MATCHES) == null ? new JSONArray() : contentObject.optJSONArray(MATCHES);
            JSONObject match = new JSONObject();
            match.put("distance", result.threshold);
            match.put("extractedText", UtilMethods.truncatify(result.extractedText, 255));
            matches.add(match);
            contentObject.put(MATCHES, matches);
            reducedResults.putIfAbsent(result.inode,contentObject);


        }


        long count = EmbeddingsAPI.impl().countEmbeddings(searcher);

        JSONObject map = new JSONObject();
        map.put("timeToEmbeddings", System.currentTimeMillis() - startTime + "ms");
        map.put("total", searchResults.size());
        map.put("query", searcher.query);
        map.put("threshold", searcher.threshold);
        map.put("dotCMSResults", reducedResults.values());
        map.put("operator", searcher.operator);
        map.put("offset", searcher.offset);
        map.put("limit", searcher.limit);
        map.put("count", count);

        return map;


    }

    @Override
    public JSONObject searchForContent(EmbeddingsDTO searcher) {

        long startTime = System.currentTimeMillis();

        List<EmbeddingsDTO> searchResults = getEmbeddingResults(searcher);
        JSONObject reducedResults = reduceChunksToContent(searcher, searchResults);

        long totalTime = System.currentTimeMillis() - startTime;

        reducedResults.put("timeToEmbeddings", totalTime + "ms");
        return reducedResults;


    }

    private JSONObject dtoToContentJson(EmbeddingsDTO dto, User user) {
        return Try.of(() ->
                ContentResource.contentletToJSON(
                        APILocator.getContentletAPI().find(dto.inode, user, true),
                        HttpServletRequestThreadLocal.INSTANCE.getRequest(),
                        HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                        "false",
                        user,
                        false)
        ).andThenTry(() ->
                new JSONObject(APILocator.getContentletAPI().find(dto.inode, user, true).getMap())
        ).andThenTry(() ->
                new JSONObject(Map.of("inode", dto.inode,
                        "identifier", dto.identifier,
                        "title", dto.title,
                        "language", dto.language,
                        "index", dto.indexName,
                        "contentType", new JSONArray(dto.contentType)))

        ).getOrElse(JSONObject::new);


    }

    private String hashText(@NotNull String text) {

        return EmbeddingsDB.impl.get().hashText(text);
    }

    private void saveEmbeddingsForCache(String content, Tuple2<Integer, List<Float>> embeddings) {

        EmbeddingsDTO embeddingsDTO = new EmbeddingsDTO.Builder()
                .withContentType(CACHE)
                .withTokenCount(embeddings._1)
                .withInode(CACHE)
                .withLanguage(0)
                .withTitle(CACHE)
                .withIdentifier(CACHE)
                .withHost(CACHE)
                .withExtractedText(content)
                .withIndexName(CACHE)
                .withEmbeddings(embeddings._2).build();


        EmbeddingsDB.impl.get().saveEmbeddings(embeddingsDTO);


    }

    private List<Float> sendTokensToOpenAI(@NotNull List<Integer> tokens) {

        JSONObject json = new JSONObject();

        json.put("model", config.getConfig(AppKeys.EMBEDDINGS_MODEL));
        json.put("input", tokens);


        String responseString = OpenAIRequest.doRequest("https://api.openai.com/v1/embeddings", "post", getAPIKey(), json);
        JSONObject response = new JSONObject(responseString);

        JSONObject data = (JSONObject) response.getJSONArray("data").get(0);


        return (List<Float>) data.getJSONArray("embedding").stream().map(val -> {
            double x = (double) val;
            return (float) x;
        }).collect(Collectors.toList());
    }

    private String getAPIKey() {
        return config.getApiKey();

    }

    @Override
    public List<EmbeddingsDTO> getEmbeddingResults(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = pullOrGenerateEmbeddings(searcher.query)._2;

        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().searchEmbeddings(newSearcher);

    }

    @Override
    public long countEmbeddings(EmbeddingsDTO searcher) {

        List<Float> queryEmbeddings = pullOrGenerateEmbeddings(searcher.query)._2;


        EmbeddingsDTO newSearcher = EmbeddingsDTO.copy(searcher).withEmbeddings(queryEmbeddings)
                .build();

        return EmbeddingsDB.impl.get().countEmbeddings(newSearcher);

    }

    @Override
    public Map<String, Map<String, Object>> countEmbeddingsByIndex() {
        return EmbeddingsDB.impl.get().countEmbeddingsByIndex();

    }

    @Override
    public void dropEmbeddingsTable() {


        EmbeddingsDB.impl.get().dropVectorDbTable();


    }

    @Override
    public void initEmbeddingsTable() {

        EmbeddingsDB.impl.get().initVectorExtension();
        EmbeddingsDB.impl.get().initVectorDbTable();


    }

    /**
     * this method takes a snippet of content and will try to see if we have already generated embeddings for it.
     * It checks the cache first, and returns if it finds it there.  Then it checks the db to see if we have already
     * saved this chunk of content before.  If we have, we reuse those same embeddings rather than making a
     * remote request $$$ to OpenAI for new Embeddings
     *
     * @param content
     * @return Tuple(Count of Tokens Input, List of Embeddings Output)
     */
    @Override
    public Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(@NotNull String content) {
        if (UtilMethods.isEmpty(content)) {
            return Tuple.of(0, List.of());
        }
        String hashed = hashText(content);
        final Tuple2<Integer, List<Float>> cachedEmbeddings = embeddingCache.getIfPresent(hashed);
        if (cachedEmbeddings != null && !cachedEmbeddings._2.isEmpty()) {
            return cachedEmbeddings;
        }

        List<Integer> tokens = EncodingUtil.encoding.get().encode(content);
        if (tokens.isEmpty()) {
            Logger.debug(this.getClass(), "NO TOKENS for " + content);
            return Tuple.of(0, List.of());
        }

        Tuple3<String, Integer, List<Float>> dbEmbeddings = EmbeddingsDB.impl.get().searchExistingEmbeddings(content);
        if (dbEmbeddings != null && !dbEmbeddings._3.isEmpty()) {
            if (!CACHE.equalsIgnoreCase(dbEmbeddings._1)) {
                saveEmbeddingsForCache(content, Tuple.of(dbEmbeddings._2, dbEmbeddings._3));
            }
            embeddingCache.put(hashed, Tuple.of(dbEmbeddings._2, dbEmbeddings._3));
            return Tuple.of(dbEmbeddings._2, dbEmbeddings._3);
        }


        Tuple2<Integer, List<Float>> openAiEmbeddings = Tuple.of(tokens.size(), sendTokensToOpenAI(tokens));
        saveEmbeddingsForCache(content, openAiEmbeddings);
        embeddingCache.put(hashed, openAiEmbeddings);
        return openAiEmbeddings;


    }


}
