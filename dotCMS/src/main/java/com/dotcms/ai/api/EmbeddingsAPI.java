package com.dotcms.ai.api;

import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EmbeddingsAPI {


    static EmbeddingsAPI impl(Host host) {
        return new EmbeddingsAPIImpl(host);
    }

    static EmbeddingsAPI impl() {
        return new EmbeddingsAPIImpl(null);
    }

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
    boolean generateEmbeddingsforContent(Contentlet contentlet, List<Field> fields, String index);

    /**
     * this method takes a contentlet and a velocity template, generates a velocity context that includes the
     * $contentlet in it and indexes the rendered result.
     *
     * @param contentlet
     * @param velocityTemplate
     * @param indexName
     * @return
     */
    boolean generateEmbeddingsforContent(@NotNull Contentlet contentlet, String velocityTemplate, String indexName);

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
     *
     * @param searcher
     * @param searchResults
     * @return
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
     * returns a list of matching content+embeddings from the dot_embeddings table based on the searcher dto
     *
     * @param searcher
     * @return
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

    /**
     * drops the dot_embeddings table
     */
    void dropEmbeddingsTable();

    /**
     * inits pg_vector and builds the dot_embeddings table
     */
    void initEmbeddingsTable();

    /**
     * Takes a string and returns the embeddings value for the string
     *
     * @param content
     * @return
     */
    Tuple2<Integer, List<Float>> pullOrGenerateEmbeddings(String content);


}
