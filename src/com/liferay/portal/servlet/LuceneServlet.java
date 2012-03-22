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

package com.liferay.portal.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;

import com.dotmarketing.util.Logger;
import com.liferay.portal.util.LuceneIndexer;
import com.liferay.portal.util.LuceneUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.FileUtil;
import com.liferay.util.GetterUtil;

/**
 * <a href="LuceneServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Jorge Ferrer
 * @version $Revision: 1.4 $
 *
 */
public class LuceneServlet extends HttpServlet {

	public void init(ServletConfig sc) throws ServletException {
		synchronized (LuceneServlet.class) {
			super.init(sc);

			ServletContext ctx = getServletContext();

			_companyId = ctx.getInitParameter("company_id");

			if (GetterUtil.getBoolean(
					PropsUtil.get(PropsUtil.INDEX_ON_STARTUP))) {

				_indexer = new LuceneIndexer(_companyId);

				if (GetterUtil.getBoolean(
						PropsUtil.get(PropsUtil.INDEX_WITH_THREAD))) {

					_indexerThread = new Thread(
						_indexer, THREAD_NAME + "." + _companyId);

					_indexerThread.setPriority(THREAD_PRIORITY);
					_indexerThread.start();
				}
				else {
					_indexer.reIndex();
				}
			}
			else {
				String luceneDir = LuceneUtil.getLuceneDir(_companyId);

				FileUtil.mkdirs(luceneDir);

				IndexWriter writer = null;

				try {
					writer = new IndexWriter(
							FSDirectory.open(new File(luceneDir)), new WhitespaceAnalyzer(), false, MaxFieldLength.LIMITED);
				}
				catch (IOException ioe1) {
					try {
						writer = new IndexWriter(
								FSDirectory.open(new File(luceneDir)), new WhitespaceAnalyzer(), true, MaxFieldLength.LIMITED);
					}
					catch (IOException ioe2) {
						Logger.error(this,ioe2.getMessage(),ioe2);
					}
				}
			}
		}
	}

	public void destroy() {

		// Wait for indexer to be gracefully interrupted

		if ((_indexer != null) && (!_indexer.isFinished()) &&
			(_indexerThread != null)) {

			_log.warn("Waiting for Lucene indexer to shutdown");

			_indexer.halt();

			try {
				_indexerThread.join(THREAD_TIMEOUT);
			}
			catch (InterruptedException e) {
				_log.error("Lucene indexer shutdown interrupted", e);

				Logger.error(this,e.getMessage(),e);
			}
		}

		// Parent

		super.destroy();
	}

	private static final Log _log = LogFactory.getLog(LuceneServlet.class);

	private static final String THREAD_NAME = LuceneIndexer.class.getName();
	private static final int THREAD_PRIORITY = Thread.MIN_PRIORITY;
	private static final int THREAD_TIMEOUT = 60000;

	private String _companyId;
	private LuceneIndexer _indexer;
	private Thread _indexerThread;

}