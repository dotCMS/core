package com.dotcms.content.elasticsearch.business;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

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

}
