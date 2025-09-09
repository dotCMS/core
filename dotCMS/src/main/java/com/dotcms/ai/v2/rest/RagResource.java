package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.embeddings.ContentTypeRagIndexRequest;
import com.dotcms.ai.v2.api.embeddings.RagIngestAPI;
import com.dotcms.ai.v2.api.embeddings.retrieval.RetrievalQuery;
import com.dotcms.ai.v2.api.embeddings.retrieval.RetrievedChunk;
import com.dotcms.ai.v2.api.embeddings.retrieval.Retriever;
import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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

    private final Retriever retriever;
    private final RagIngestAPI ragIngestAPI;

    @Inject
    public RagResource(final Retriever retriever,
                       final RagIngestAPI ragIngestAPI) {
        this.retriever = retriever;
        this.ragIngestAPI = ragIngestAPI;
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

        final int pageSize  = request.getPageSize()  == null && request.getPageSize().isPresent()?
                50  : Math.max(1, request.getPageSize().get());
        final int batchSize = request.getBatchSize() == null && request.getBatchSize().isPresent()?
                128 : Math.max(1, request.getBatchSize().get());
        final ModelConfig modelConfig = null; // todo: get here the configuration model

        DotConcurrentFactory.getInstance().getSubmitter() // todo: see if want a special one
            .submit(() -> {
            try {
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
