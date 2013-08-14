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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.liferay.util.StringPool;
import com.liferay.util.Validator;
import com.liferay.util.lucene.KeywordsUtil;

/**
 * <a href="LuceneUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class LuceneUtil {

	public static void addTerm( BooleanQuery booleanQuery, String field, String text) throws ParseException {

		if (Validator.isNotNull(text)) {
			if (text.indexOf(StringPool.SPACE) == -1) {
				text = KeywordsUtil.toWildcard(text);
			}

			QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, field, new SimpleAnalyzer(Version.LUCENE_CURRENT));
			Query query = parser.parse(text);

			booleanQuery.add(query, BooleanClause.Occur.SHOULD);
		}
	}

	public static void addRequiredTerm(
		BooleanQuery booleanQuery, String field, String text) {

		text = KeywordsUtil.escape(text);

		Term term = new Term(field, text);
		TermQuery termQuery = new TermQuery(term);

		booleanQuery.add(termQuery, BooleanClause.Occur.MUST);
	}

	public static String getLuceneDir(String companyId) {
		return PropsUtil.get(PropsUtil.LUCENE_DIR) + companyId +
			StringPool.SLASH;
	}

	public static IndexReader getReader(String companyId) throws IOException {
		return IndexReader.open(FSDirectory.open(new File(getLuceneDir(companyId))));
	}

    public static IndexSearcher getSearcher ( String companyId ) throws IOException {

        Directory directory = FSDirectory.open( new File( getLuceneDir( companyId ) ) );
        DirectoryReader ireader = DirectoryReader.open( directory );

        return new IndexSearcher( ireader );
    }

	public static IndexWriter getWriter(String companyId) throws IOException {
		return getWriter(companyId, false);
	}

    public static IndexWriter getWriter ( String companyId, boolean create ) throws IOException {

        Directory directory = FSDirectory.open( new File( getLuceneDir( companyId ) ) );
        IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_CURRENT, new SimpleAnalyzer( Version.LUCENE_CURRENT ) );

        return new IndexWriter( directory, config );
    }

}