/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.util;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;

import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.util.FileUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Time;
import com.liferay.util.lucene.Indexer;

/**
 * <a href="LuceneIndexer.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class LuceneIndexer implements Runnable {

	public LuceneIndexer(String companyId) {
		_companyId = companyId;
	}

	public void halt() {
		_halt = true;
	}

	public boolean isFinished() {
		return _finished;
	}

	public void run() {
		reIndex();
	}

	public void reIndex() {
		_log.info("Re-indexing Lucene started");

		long start = System.currentTimeMillis();

		String luceneDir = LuceneUtil.getLuceneDir(_companyId);

		FileUtil.deltree(luceneDir);

		try {
			IndexWriter writer = LuceneUtil.getWriter(_companyId, true);

			writer.close();
		}
		catch (IOException ioe) {
			Logger.error(LuceneIndexer.class,ioe.getMessage(),ioe);
		}

		String[] indexIds = new String[] {_companyId};

		try {
			Iterator itr =
				PortletManagerUtil.getPortlets(_companyId).iterator();

			while (itr.hasNext()) {
				Portlet portlet = (Portlet)itr.next();

				String className = portlet.getIndexerClass();

				if (portlet.isActive() && className != null) {
					_log.debug("Re-indexing with " + className + " started");

					Indexer indexer = (Indexer)InstancePool.get(className);

					indexer.reIndex(indexIds);

					_log.debug("Re-indexing with " + className + " completed");
				}
			}
		}
		catch (Exception e) {
			Logger.error(LuceneIndexer.class,e.getMessage(),e);
		}

		long end = System.currentTimeMillis();

		_log.info(
			"Re-indexing Lucene completed in " + ((end - start) / Time.SECOND) +
			" seconds");

		_finished = true;
	}

	private static final Log _log = LogFactory.getLog(LuceneIndexer.class);

	private String _companyId;
	private boolean _halt;
	private boolean _finished;

}