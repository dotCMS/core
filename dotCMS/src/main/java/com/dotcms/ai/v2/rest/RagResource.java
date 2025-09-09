package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.embeddings.RetrievalQuery;
import com.dotcms.ai.v2.api.embeddings.RetrievedChunk;
import com.dotcms.ai.v2.api.embeddings.Retriever;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v2/ai/rag")
@Tag(name = "AI", description = "AI-powered rag")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RagResource {

    private final Retriever retriever;

    @Inject
    public RagResource(final Retriever retriever) {
        this.retriever = retriever;
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
