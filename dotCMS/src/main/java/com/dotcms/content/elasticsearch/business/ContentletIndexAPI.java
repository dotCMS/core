package com.dotcms.content.elasticsearch.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.common.reindex.BulkProcessorListener;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;

public interface ContentletIndexAPI {
    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    public void getRidOfOldIndex() throws DotDataException;

    /**
     * Inits the indexs
     */
    public void checkAndInitialiazeIndex();

    public boolean createContentIndexLegacy(String indexName) throws DotIndexException, IOException;

    public boolean createContentIndexLegacy(String indexName, int shards) throws DotIndexException, IOException;

    /**
     * creates new working and live indexes with reading aliases pointing to old index and write aliases
     * pointing to both old and new indexes
     *
     * @return the timestamp string used as suffix for indices
     * @throws DotDataException
     * @throws DotIndexException
     */
    public String fullReindexStart() throws DotIndexException, DotDataException;

    /**
     * returns if the system is in a full reindex
     * 
     * @return
     * @throws DotDataException
     */
    public boolean isInFullReindex() throws DotDataException;

    /**
     * This will drop old index and will point read aliases to new index. If you pass forceSwitch=true
     * then this method will force a switch, otherwise, it will check to make sure that we are in a 
     * reindex and that it is the lucky server in the cluster to switch
     * after call to {@link #setUpFullReindex()}
     *
     * @return
     */
    public boolean fullReindexSwitchover(final boolean forceSwitch);

    public boolean fullReindexSwitchover(Connection conn, final boolean forceSwitch);

    /**
     * deletes an elasticsearch index by name
     * 
     * @param indexName
     * @return
     */
    boolean delete(String indexName);

    /**
     * optimizes shards for a list of elasticsearch indicies
     * 
     * @param indexNames
     * @return
     */
    boolean optimize(List<String> indexNames);

    public void removeContentFromIndex(final Contentlet content) throws DotDataException;

    public void removeContentFromIndex(final Contentlet content, final boolean onlyLive) throws DotDataException;

    public void removeContentFromLiveIndex(final Contentlet content) throws DotDataException;

    public void removeContentFromIndexByStructureInode(String structureInode)
            throws DotDataException, DotSecurityException;

    public void removeContentFromIndexByContentType(final ContentType contentType)
            throws DotDataException;

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

    /**
     * Gets the document count of a given index. In case the index does not exist, a runtime exception
     * is thrown
     * @param indexName
     * @return Documents count - long
     */
    long getIndexDocumentCount(String indexName);

    public List<String> getCurrentIndex() throws DotDataException;

    public List<String> getNewIndex() throws DotDataException;

    public List<String> listDotCMSClosedIndices();

    public String getActiveIndexName(String type) throws DotDataException;

    void putToIndex(BulkRequest bulkRequest, ActionListener<BulkResponse> listener);

    void putToIndex(BulkRequest bulkRequest);

    void addContentToIndex(List<Contentlet> contentToIndex) throws DotDataException;

    void addContentToIndex(Contentlet content) throws DotDataException;

    void addContentToIndex(Contentlet content, boolean deps) throws DotDataException;

    BulkRequest createBulkRequestLegacy(List<Contentlet> contentToIndex) throws DotDataException;

    BulkRequest createBulkRequestLegacy();

    BulkRequest appendBulkRequestLegacy(BulkRequest bulkRequest, Collection<ReindexEntry> idxs) throws DotDataException;

    BulkRequest appendBulkRequestLegacy(BulkRequest bulkRequest, ReindexEntry idx) throws DotDataException;

    Optional<String> reindexTimeElapsed();

    void stopFullReindexationAndSwitchover() throws DotDataException;

    boolean reindexSwitchover(boolean forceSwitch) throws DotDataException;

    void stopFullReindexation() throws DotDataException;

    BulkRequest appendBulkRemoveRequest(BulkRequest bulkRequest, final ReindexEntry entry) throws DotDataException;

    BulkProcessor createBulkProcessorLegacy(BulkProcessorListener bulkListener);

    void appendToBulkProcessorLegacy(final BulkProcessor bulk, final Collection<ReindexEntry> idxs) throws DotDataException;
}
