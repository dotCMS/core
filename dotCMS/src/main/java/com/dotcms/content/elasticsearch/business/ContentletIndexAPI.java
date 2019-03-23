package com.dotcms.content.elasticsearch.business;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.List;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public interface ContentletIndexAPI {
    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final String ES_WORKING_INDEX_NAME = "working";
    public static final String ES_LIVE_INDEX_NAME = "live";

    public void getRidOfOldIndex() throws DotDataException;

    /**
     * Inits the indexs
     */
    public void checkAndInitialiazeIndex();

    public boolean createContentIndex(String indexName) throws DotIndexException, IOException;

    public boolean createContentIndex(String indexName, int shards) throws DotIndexException, IOException;

    /**
     * creates new working and live indexes with reading aliases pointing to old index and write aliases
     * pointing to both old and new indexes
     *
     * @return the timestamp string used as suffix for indices
     * @throws DotDataException
     * @throws DotIndexException
     */
    public String fullReindexStart() throws DotIndexException, DotDataException;

    public boolean isInFullReindex() throws DotDataException;

    /**
     * This will drop old index and will point read aliases to new index. This method should be called
     * after call to {@link #setUpFullReindex()}
     *
     * @return
     */
    public void fullReindexSwitchover();

    public void fullReindexSwitchover(Connection conn);

    boolean delete(String indexName);

    boolean optimize(List<String> indexNames);

    public void removeContentFromIndex(final Contentlet content) throws DotDataException;

    public void removeContentFromIndex(final Contentlet content, final boolean onlyLive) throws DotDataException;

    public void removeContentFromLiveIndex(final Contentlet content) throws DotDataException;

    public void removeContentFromIndexByStructureInode(String structureInode) throws DotDataException;

    void fullReindexAbort();

    public boolean isDotCMSIndexName(String indexName);

    /**
     * Returns a list of dotcms working and live indices.
     *
     * @return
     */
    public List<String> listDotCMSIndices();

    void activateIndex(String indexName) throws DotDataException;

    void deactivateIndex(String indexName) throws DotDataException, IOException;

    public List<String> getCurrentIndex() throws DotDataException;

    public List<String> getNewIndex() throws DotDataException;

    public List<String> listDotCMSClosedIndices();

    public String getActiveIndexName(String type) throws DotDataException;
    
    BulkRequestBuilder appendBulkRequest(BulkRequestBuilder bulk, List<Contentlet> contentToIndex);

    BulkRequestBuilder appendReindexRequest(BulkRequestBuilder bulk, List<Contentlet> contentToIndex);


    BulkRequestBuilder createBulkRequest(List<Contentlet> contentToIndex) throws DotDataException, DotSecurityException, DotMappingException;

    void putToIndex(BulkRequestBuilder bulk, ActionListener<BulkResponse> listener);

    void putToIndex(BulkRequestBuilder bulk);

    void addContentToIndex(List<Contentlet> contentToIndex) throws DotDataException;

    void addContentToIndex(Contentlet content) throws DotDataException;

    void addContentToIndex(Contentlet content, boolean deps) throws DotDataException;

    void addContentToIndex(Contentlet parentContenlet, boolean includeDependencies, boolean indexBeforeCommit) throws DotDataException;

    void indexContentListDefer(List<Contentlet> contentToIndex);

    BulkRequestBuilder createBulkRequest();



}
