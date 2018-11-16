package com.dotcms.content.elasticsearch.business;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.List;

public interface ContentletIndexAPI {
    public static final SimpleDateFormat timestampFormatter=new SimpleDateFormat("yyyyMMddHHmmss");
    public static final String ES_WORKING_INDEX_NAME = "working";
    public static final String ES_LIVE_INDEX_NAME = "live";
	public void getRidOfOldIndex() throws DotDataException;

	/**
	 * Inits the indexs
	 */
	public void checkAndInitialiazeIndex();

	public boolean createContentIndex(String indexName)throws DotIndexException, IOException;
	public boolean createContentIndex(String indexName, int shards) throws DotIndexException, IOException;

	/**
	 * creates new working and live indexes with reading aliases pointing to old
	 * index and write aliases pointing to both old and new indexes
	 *
	 * @return the timestamp string used as suffix for indices
	 * @throws DotDataException
	 * @throws DotIndexException
	 */
	public String setUpFullReindex() throws DotIndexException, DotDataException;

	public boolean isInFullReindex() throws DotDataException;

	/**
	 * This will drop old index and will point read aliases to new index. This
	 * method should be called after call to {@link #setUpFullReindex()}
	 *
	 * @return
	 */
	public void fullReindexSwitchover();
	public void fullReindexSwitchover(Connection conn);

	public boolean delete(String indexName);

	public boolean optimize(List<String> indexNames);

	/**
	 * Returns true if the {@link Contentlet} has been already indexed on the current thread
	 * @param contentlet {@link Contentlet}
	 * @return true if the content has been already indexed
	 */
	boolean isContentAlreadyIndexed(final Contentlet contentlet);


	/**
	 * Returns true if the {@link Contentlet} id has been already indexed on the current thread
	 * @param contentletIdentifier {@link String}
	 * @return true if the content has been already indexed
	 */
	boolean isContentAlreadyIndexed(final String contentletIdentifier);

	public void addContentToIndex(final Contentlet content) throws DotHibernateException;

	public void addContentToIndex(final Contentlet content, final boolean deps) throws DotHibernateException;

	public void addContentToIndex(final Contentlet content, final boolean deps, boolean indexBeforeCommit) throws DotHibernateException;

	public void addContentToIndex(final Contentlet content, final boolean deps, boolean indexBeforeCommit, final boolean reindexOnly)
			throws DotHibernateException;

	public void addContentToIndex(final Contentlet content, final boolean deps, boolean indexBeforeCommit, final boolean reindexOnly,
			final BulkRequestBuilder bulk) throws DotHibernateException;

	public void removeContentFromIndex(final Contentlet content) throws DotHibernateException;

	public void removeContentFromIndex(final Contentlet content, final boolean onlyLive) throws DotHibernateException;

	public void removeContentFromLiveIndex(final Contentlet content) throws DotHibernateException;

	public void removeContentFromIndexByStructureInode(String structureInode) throws DotDataException;

	public void fullReindexAbort();

	public boolean isDotCMSIndexName(String indexName);

	/**
	 * Returns a list of dotcms working and live indices.
	 *
	 * @return
	 */
	public List<String> listDotCMSIndices();

	public void activateIndex(String indexName) throws DotDataException;

	public void deactivateIndex(String indexName) throws DotDataException, IOException;



	public List<String> getCurrentIndex() throws DotDataException;

	public List<String> getNewIndex() throws DotDataException;

	public List<String> listDotCMSClosedIndices();

	public String getActiveIndexName(String type) throws DotDataException;

	void indexContentList(List<Contentlet> contentToIndex, BulkRequestBuilder bulk, boolean reindexOnly) throws DotDataException;

	/**
	 * This method is similar to the indexContentList, but it make searchable the content immediate
	 * Important node: this is only for testing
	 * @param contentToIndex
	 * @param bulk
	 * @param reindexOnly
	 * @throws DotDataException
	 */
	@VisibleForTesting
	void indexContentListNow(List<Contentlet> contentToIndex, BulkRequestBuilder bulk, boolean reindexOnly) throws DotDataException;

	/**
	 * This method is similar to the indexContentList, but it will wait until the contentlets are indexed to continue
	 *
	 * @param contentToIndex
	 * @param bulk
	 * @param reindexOnly
	 * @throws DotDataException
	 */
	void indexContentListWaitFor(List<Contentlet> contentToIndex, BulkRequestBuilder bulk, boolean reindexOnly) throws DotDataException;


	/**
	 * This method stores the contentlets in a queue in order to be process (indexed) in a separated and deferred mechanism
	 * (see: {@link com.dotmarketing.common.business.journal.DistributedJournalAPI} and {@link com.dotmarketing.common.reindex.ReindexThread})
	 * When you use this method you do not really care about when the content is gonna be index.
	 *
	 * If you need to index a collection of contentlets and wait until they are done, use
	 *
	 * {@link #indexContentListWaitFor(List, BulkRequestBuilder, boolean)}
	 *
	 * If you need to index a collection of contentlets and be notified (listen until) when the indexing is done use
	 *
	 * {@link #indexContentList(List, BulkRequestBuilder, boolean, ActionListener)}
	 *
	 * @param contentToIndex {@link List}
	 * @throws DotHibernateException
	 */
	public void indexContentListDeferred(final List<Contentlet> contentToIndex) throws DotHibernateException;

	/**
	 * Same of indexContentList, just including a listener that will be call when the indexing is done.
	 * @param contentToIndex
	 * @param bulk
	 * @param reindexOnly
	 * @param listener    {@link ActionListener} implement it in order to be notified in async mode when the content indexing is done
	 * @throws DotDataException
	 */
	void indexContentList(final List<Contentlet> contentToIndex,
								 final BulkRequestBuilder bulk,
								 final boolean reindexOnly,
								 ActionListener<BulkResponse> listener) throws  DotDataException;

}
