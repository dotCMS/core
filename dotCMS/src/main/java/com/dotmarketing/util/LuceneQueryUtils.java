package com.dotmarketing.util;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import java.util.List;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;


public class LuceneQueryUtils {


    /**
     *
     * @param luceneQuery
     * @return
     */
    private static String removeQueryPrefix(final String luceneQuery){
        final String cleanedUpQuery;
        if(luceneQuery.startsWith("query_")){
            cleanedUpQuery = luceneQuery.replace( "query_", StringPool.BLANK);
        } else {
            cleanedUpQuery = luceneQuery;
        }
        return cleanedUpQuery;
    }

    private static List<BooleanClause> filterQueryTerms = ImmutableList.of(
            new BooleanClause(new TermQuery(new Term("contentType","Host")),Occur.MUST_NOT)
    );

    /**
     * This method basically does two things. Gets rid of the 'query_' prefix and also adds an additional condition to ensure we exclude all content of type host
     * Since acces to ContentType Host is limited.
     * @param luceneQuery
     * @return
     * @throws ParseException
     */
    public static String prepareBulkActionsQuery(final String luceneQuery) throws ParseException {

        final String cleanedUpQuery = removeQueryPrefix(luceneQuery);
        final QueryParser parser = new QueryParser(null, new WhitespaceAnalyzer());
        final BooleanQuery query = (BooleanQuery) parser.parse(cleanedUpQuery);
        final List<BooleanClause> clauses = query.clauses();

        final BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for(final BooleanClause clause:clauses){
           builder.add(clause);
        }

        for (final BooleanClause clause:filterQueryTerms) {
            builder.add(clause);
        }

        return builder.build().toString();
    }

}
