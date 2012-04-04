package com.dotmarketing.portlets.contentlet.business;

import java.util.List;

import org.apache.lucene.document.Document;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.dotmarketing.beans.Identifier;

/**
 *
 * @author Jason Tesser
 */

public abstract class ReindexContentletFactory {

	public static final String ADD_TO_INDEX = "ADD_TO_INDEX";

	public static final String ADD_TO_INDEX_REINDEXATION = "ADD_TO_INDEX_REINDEXATION";

	/**
	 * returns a list of documents to reindex for a given identifier. 
	 * @param identifier
	 * @return
	 * @throws Exception
	 */
	public abstract List<XContentBuilder> buildDocList(Identifier identifier) throws Exception;
	
	public abstract XContentBuilder buildDocNoDeps(Identifier identifier) throws Exception;
	
}
