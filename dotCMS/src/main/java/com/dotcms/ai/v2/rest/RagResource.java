package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.SimilarityOperator;
import com.dotcms.ai.v2.api.embeddings.ContentTypeRagIndexRequest;
import com.dotcms.ai.v2.api.embeddings.DotPgVectorEmbeddingStore;
import com.dotcms.ai.v2.api.embeddings.RagIngestAPI;
import com.dotcms.ai.v2.api.embeddings.retrieval.EmbeddingStoreRetriever;
import com.dotcms.ai.v2.api.embeddings.retrieval.RetrievalQuery;
import com.dotcms.ai.v2.api.embeddings.retrieval.RetrievedChunk;
import com.dotcms.ai.v2.api.embeddings.retrieval.Retriever;
import com.dotcms.ai.v2.api.provider.Model;
import com.dotcms.ai.v2.api.provider.ModelProviderFactory;
import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v2/ai/rag")
@Tag(name = "AI", description = "AI-powered rag")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RagResource {

    private final RagIngestAPI ragIngestAPI;
    private final ModelProviderFactory modelProviderFactory;

    @Inject
    public RagResource(final RagIngestAPI ragIngestAPI,
                       final ModelProviderFactory modelProviderFactory) {
        this.ragIngestAPI = ragIngestAPI;
        this.modelProviderFactory = modelProviderFactory;
    }



    @POST
    @Path("/content-type")
    public void indexContentType(final IndexContentTypeRequest request,
                                 @Suspended final AsyncResponse asyncResponse) {

        if (request == null || request.getContentType() == null || request.getContentType().trim().isEmpty()) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                    .entity("contentType is required").build());
            return;
        }

        final int pageSize  = request.getPageSize()  == null || request.getPageSize().isEmpty()?
                50  : Math.max(1, request.getPageSize().get());
        final int batchSize = request.getBatchSize() == null || request.getBatchSize().isEmpty()?
                128 : Math.max(1, request.getBatchSize().get());
        // todo: this should reach into a configuration and retrieve the one, by now we are using the open ai
        // eventually if do not send any provider key we can try onnix, but it is ok to hardcode by now
        final ModelConfig modelConfig = Model.OPEN_AI_TEXT_EMBEDDING_3_SMALL.toConfig(Config.getStringProperty("OPEN_AI_API_KEY"));

        DotConcurrentFactory.getInstance().getSubmitter() // todo: see if want a special one submitter
            .submit(() -> {
            try {
                // todo: later we should create an observer/listener to stream the progress, but it is ok by now
                final int chunks = ragIngestAPI.indexContentType(
                        ContentTypeRagIndexRequest.builder()
                                .withHost(request.getHost().orElse(Host.SYSTEM_HOST))
                                .withContentType(request.getContentType())
                                .withLanguageId(request.getLanguageId().orElse(APILocator.getLanguageAPI().getDefaultLanguage().getId()))
                                .withIndexName(request.getIndexName().orElse("default"))
                                .withModelConfig(modelConfig)
                                .withPageSize(pageSize)
                                .withBatchSize(batchSize)
                                .withEmbeddingProviderKey(request.getEmbeddingProviderKey().orElse("onnix"))
                                .build()

                );
                asyncResponse.resume(Response.ok(IndexResponse.of(chunks)).build());
            } catch (Exception e) {
                asyncResponse.resume(Response.serverError()
                        .entity("Ingestion failed: " + e.getMessage())
                        .build());
            }
        });
    }

    /**
     * Stateless RAG search:
     * - Embeds the query
     * - Searches embeddings
     * - Applies filters & threshold
     * - Returns top-K with metadata
     */
    @POST
    @Path("/search")
    public RagSearchResponse search(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final RagSearchRequest ragSearchRequest) {

        if (ragSearchRequest == null || ragSearchRequest.getQuery() == null
                || ragSearchRequest.getQuery().trim().isEmpty()) {

            throw new BadRequestException("query is required");
        }

        // todo: this should reach into a configuration and retrieve the one, by now we are using the open ai
        // eventually if do not send any provider key we can try onnix, but it is ok to hardcode by now
        final ModelConfig modelConfig = Model.OPEN_AI_TEXT_EMBEDDING_3_SMALL.toConfig(Config.getStringProperty("OPEN_AI_API_KEY"));


        final EmbeddingModel embeddingModel = this.modelProviderFactory.getEmbedding(/*ragSearchRequest.getEmbeddinModelProviderKey()*/
                Model.OPEN_AI_TEXT_EMBEDDING_3_SMALL.getProviderName(), modelConfig);
        final EmbeddingStore<TextSegment> store = DotPgVectorEmbeddingStore.builder()
                .indexName(/*request.getIndexName()!=null?request.getIndexName():"default"*/"default")
                .dimension(embeddingModel.dimension())
                .operator(SimilarityOperator.COSINE) // todo: by now cosine by the user should have the ability to send it  // o INNER_PRODUCT/EUCLIDEAN
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
        retrievalQueryBuilder.prompt(ragSearchRequest.getQuery());
        ragSearchRequest.getSite().ifPresent(retrievalQueryBuilder::site);
        retrievalQueryBuilder.contentTypes(ragSearchRequest.getContentTypes());
        ragSearchRequest.getLanguageId().ifPresent(retrievalQueryBuilder::languageId);
        //retrievalQueryBuilder.identifier(ragSearchRequest.getIdentifier()) ;
        retrievalQueryBuilder.limit(ragSearchRequest.getLimit() == null || ragSearchRequest.getLimit() <= 0? 8 : ragSearchRequest.getLimit());
        retrievalQueryBuilder.offset((ragSearchRequest.getOffset() == null || ragSearchRequest.getOffset() < 0) ? 0 : ragSearchRequest.getOffset());
        retrievalQueryBuilder.threshold((ragSearchRequest.getThreshold() == null) ? 0.0 : ragSearchRequest.getThreshold());

        final RetrievalQuery retrievalQuery = retrievalQueryBuilder.build();
        final long startMillis = System.currentTimeMillis();
        final List<RetrievedChunk> chunks = retriever.search(retrievalQuery);
        final long endMillis = System.currentTimeMillis();

        final List<RagSearchMatch> matches = chunks.stream().map(retrievedChunk -> {
            return RagSearchMatch.builder()
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

        return RagSearchResponse.of(matches, matches.size(), Map.of("latencyMs", (endMillis - startMillis)));
    }

    private static String truncate(final String text, final int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 3)) + "...";
    }

}
