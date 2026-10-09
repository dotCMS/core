package com.dotcms.ai.api;

import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The EmbeddingsAPI interface provides methods for managing and interacting with embeddings.
 * Embeddings are used to convert text into a form that can be processed by machine learning algorithms.
 * This interface provides methods for generating, deleting, and searching embeddings.
 * It also provides methods for managing the embeddings table.
 *
 * Implementations of this interface should provide specific functionality for these operations.
 */
public interface EmbeddingsAPI {

    String AI_THREAD_POOL_KEY = "AIThreadPool";

    void shutdown();

    /**
     * given a contentlet, a list of fields and an index, this method will do its best to turn that content into an
     * index-able string that then gets vectorized and stored in postgres.
     * <p>
     * Important - if you send in an empty list of fields to index, the method will try to intelligently(tm) pick how to
     * index the content.  For example, if you send in a fileAsset or dotAsset, it will try to index the content of the
     * file. If you send a htmlPageAsset, it will render the page and index the rendered page.  If you send a contentlet
     * with a Storyblock or wysiwyg field, it will render those and index the resultant content.
     *
     * @param contentlet
     * @param fields
     * @param index
     * @return
     */
    boolean generateEmbeddingsForContent(Contentlet contentlet, List<Field> fields, String index);

    /**
     * this method takes a contentlet and a velocity template, generates a velocity context that includes the
     * $contentlet in it and indexes the rendered result.
     *
     * @param contentlet
     * @param velocityTemplate
     * @param indexName
     * @return
     */
    boolean generateEmbeddingsForContent(@NotNull Contentlet contentlet, String velocityTemplate, String indexName);

    /**
     * this method takes a lucene query and
     * @param deleteQuery
     * @param indexName
     * @param user
     * @return
     */
    int deleteByQuery(@NotNull String deleteQuery, Optional<String> indexName, User user);

    /**
     * Takes a DTO object and based on its properties deletes from the embeddings index.
     *
     * @param dto
     * @return
     */
    int deleteEmbedding(EmbeddingsDTO dto);

    /**
     * This method takes comma or line separated string of content types and optionally fields and returns
     *
     * @param typeAndFieldParam a map of <contentTypeVar, List<FieldsToIndex>>
     * @return
     */
    Map<String, List<Field>> parseTypesAndFields(String typeAndFieldParam);

    /**
     * This method takes a list of semantic search results, which are just fragements of content and returns a json
     * object of a list of the actual contentlets and fragements that matched the result. The idea is to provide the
     * ability to show exactly which contentlets matched the semantic query and specifically, which fragments in that
     * content matched.
     * <p>The response's {@code count} is the number of contentlets in it, and {@code total} the
     * number of chunks. Pass chunks that came from {@link #getEmbeddingResults(EmbeddingsDTO)}, so
     * they are already filtered by READ permission.</p>
     *
     * @param searcher      the search request
     * @param searchResults the chunks to group, already filtered by READ permission
     * @return the search response JSON
     */
    JSONObject reduceChunksToContent(EmbeddingsDTO searcher, List<EmbeddingsDTO> searchResults);

    /**
     * Takes a searcher DTO and returns a JSON object that is a list of matching contentlets and the fragments that
     * matched.
     *
     * @param searcher
     * @return
     */
    JSONObject searchForContent(EmbeddingsDTO searcher);

    /**
     * Returns the chunks from the dot_embeddings table that match the searcher and that the
     * searcher's user can READ. Every AI search, related-content and completions call gets its
     * chunks here, so every implementation must honour this contract:
     * <ul>
     *     <li>Only chunks whose source contentlet the user can READ are returned (front-end roles
     *     respected, so anonymous and front-end users see live content only). A {@code null} user
     *     is checked as the Anonymous user; pass the system user for unrestricted results.</li>
     *     <li>{@code offset} and {@code limit} count readable chunks, taken from at most
     *     {@code max(limit, cap)} matching chunks per call, where the cap is
     *     {@code embeddingsSearchCandidateCap} (default 1000) for logged-in callers and at most
     *     200 for anonymous ones. A page comes back short when the readable matches rank below
     *     that many restricted ones, or when {@code offset} goes past them.</li>
     *     <li>For callers other than CMS admins and the system user, chunks with no contentlet
     *     behind them (deleted content, cache rows) are never returned.</li>
     *     <li>A failure loading contentlets or checking permissions is thrown, not turned into an
     *     empty result.</li>
     * </ul>
     *
     * @param searcher the search request, including the user
     * @return the readable chunks, in distance order
     */
    List<EmbeddingsDTO> getEmbeddingResults(EmbeddingsDTO searcher);

    /**
     * returns a count of matching content+embeddings based on the searcher dto
     *
     * @param searcher
     * @return
     */
    long countEmbeddings(EmbeddingsDTO searcher);

    /**
     * returns a map of all the available dot_embeddings 'indexes' plus the count of embeddings in them
     *
     * @return
     */
    Map<String, Map<String, Object>> countEmbeddingsByIndex();

    boolean indexExists(String indexName);

    /**
     * drops the dot_embeddings table
     */
    void dropEmbeddingsTable();

    /**
     * inits pg_vector and builds the dot_embeddings table
     */
    void initEmbeddingsTable();

    /**
     * this method takes a snippet of content and will try to see if we have already generated
     * embeddings for it. It checks the cache first, and returns if it finds it there.  Then it
     * checks the db to see if we have already saved this chunk of content before.  If we have, we
     * reuse those same embeddings rather than making a remote request $$$ to OpenAI for new
     * Embeddings
     *
     * @param content The content that will be tokenized and sent to OpenAI.
     * @param userId   The ID of the user making the request.
     *
     * @return Tuple(Count of Tokens Input, List of Embeddings Output)
     */
    Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(String content, String userId);

    /**
     * this method takes a snippet of content and will try to see if we have already generated
     * embeddings for it. It checks the cache first, and returns if it finds it there.  Then it
     * checks the db to see if we have already saved this chunk of content before.  If we have, we
     * reuse those same embeddings rather than making a remote request $$$ to OpenAI for new
     * Embeddings
     *
     * @param contentId The ID of the Contentlet being sent to the OpenAI Endpoint.
     * @param content   The actual indexable data that will be tokenized and sent to OpenAI service.
     * @param userId   The ID of the user making the request.
     *
     * @return Tuple(Count of Tokens Input, List of Embeddings Output)
     */
    Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(String contentId, String content, String userId);

    /**
     * Checks if the embeddings for the given inode, indexName, and extractedText already exist in the database.
     *
     * @param inode the inode of the contentlet.
     * @param indexName the name of the index where the embeddings are stored.
     * @param extractedText the text that was extracted from the contentlet and used to generate the embeddings.
     * @return true if the embeddings exist, false otherwise.
     */
    boolean embeddingExists(final String inode, final String indexName, final String extractedText);

    /**
     * Saves the provided embeddings to the database.
     *
     * @param embeddings the EmbeddingsDTO object containing the embeddings to be saved.
     */
    void saveEmbeddings(final EmbeddingsDTO embeddings);

    /**
     * Deletes the embeddings from the database that match the provided EmbeddingsDTO.
     *
     * @param dto the EmbeddingsDTO object containing the embeddings to be deleted.
     * @return the number of embeddings deleted from the database.
     */
    int deleteEmbeddings(final EmbeddingsDTO dto);

}
