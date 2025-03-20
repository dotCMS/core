package com.dotcms.content.elasticsearch;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.google.common.collect.ImmutableSet;

public class ESQueryCacheTest extends UnitTestBase {


    static ESQueryCache cache;


    @BeforeClass
    public static void setup() {

        cache = new  ESQueryCache(admin);
        
        
        
    }

    @Test
    public void test_hash() {

        String testQuery =RandomStringUtils.randomAscii(200);

        SearchRequest request1 = getSearchRquest(testQuery, true) ;
        SearchRequest request2 = getSearchRquest(testQuery, true) ;
        

        assertEquals(cache.hash(request1), cache.hash(request2));
        
        
        SearchRequest request3 = getSearchRquest(testQuery, false) ;
        

        assertNotEquals(cache.hash(request1), cache.hash(request3));
    }

    
    @Test
    public void test_get_hits_by_query() {

        SearchRequest request = getSearchRequest();
        SearchHits hits= hits();
        cache.put(request, hits);
        
        assertTrue(cache.get(request).isPresent());
        
        assertEquals(hits, cache.get(request).get());
        
        SearchRequest request2 = getSearchRequest();
        assertTrue(cache.get(request2).isEmpty());

    }
    
    
    @Test
    public void test_get_hits_by_request() {

        final String testQuery =RandomStringUtils.randomAscii(200);
        SearchHits hits= hits();
        
        SearchRequest req1 = getSearchRquest(testQuery, true);
        SearchRequest req2 = getSearchRquest(testQuery, true);
        cache.put(req1, hits);
        

        assert(cache.get(req1).isPresent());
        
        assert(cache.get(req2).isPresent());
        
        assertEquals(cache.get(req1).get(), cache.get(req2).get());

    }
    
    
    @Test
    public void test_clear_cache() {

        final String testQuery =RandomStringUtils.randomAscii(200);
        SearchHits hits= hits();
        
        SearchRequest req1 = getSearchRquest(testQuery, true);
        SearchRequest req2 = getSearchRquest(testQuery, true);
        cache.put(req1, hits);
        

        
        assert(cache.get(req1).isPresent());
        assert(cache.get(req2).isPresent());
        
        cache.clearCache();
        
        assert(cache.get(req1).isEmpty());
        assert(cache.get(req2).isEmpty());


    }
    
    
    
    
    SearchHits hits() {
        Random rand = new Random(); 
        List<SearchHit> hitList = new ArrayList<>();
        for(int i=0;i< rand.nextInt(10);i++) {
            Text text = new Text(RandomStringUtils.randomAscii(36));
            SearchHit hit = new SearchHit(i,RandomStringUtils.randomAscii(36),text,new HashMap<>(),new HashMap<>());
            hitList.add(hit);
        }
        
        return new SearchHits(hitList.toArray(new SearchHit[0]), new TotalHits(2, Relation.EQUAL_TO), .7f);
        
        
    }
    
    
    
    private SearchRequest getSearchRequest() {

        return getSearchRquest(RandomStringUtils.randomAscii(200), true) ;
        
    }
    
    @NotNull
    private SearchRequest getSearchRquest(String query, boolean live) {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(query));
        searchSourceBuilder.size(0);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices((live ? "live" : "working"));
        return searchRequest;
    }

    static DotCacheAdministrator admin = new DotCacheAdministrator() {

        private Map<String, SearchHits> hitMap = new HashMap<>();

        @Override
        public void shutdown() {


        }



        @Override
        public void removeLocalOnly(String key, String group, boolean ignoreDistributed) {
            hitMap.remove(key + group);

        }

        @Override
        public void remove(String key, String group) {
            hitMap.remove(key + group);

        }

        @Override
        public void put(String key, Object content, String group) {
            hitMap.put(key + group, (SearchHits) content);

        }

        @Override
        public void invalidateCacheMesageFromCluster(String message) {


        }

        @Override
        public void initProviders() {


        }

        @Override
        public CacheTransport getTransport() {

            return null;
        }

        @Override
        public DotCacheAdministrator getImplementationObject() {

            return null;
        }

        @Override
        public Class getImplementationClass() {

            return null;
        }

        @Override
        public Set<String> getGroups() {

            return ImmutableSet.of(cache.getPrimaryGroup());
        }

        @Override
        public List<CacheProviderStats> getCacheStatsList() {

            return null;
        }

        @Override
        public Object get(String key, String group) throws DotCacheException {

            return hitMap.get(key + group);
        }

        @Override
        public void flushGroupLocalOnly(String group, boolean ignoreDistributed) {
            hitMap.clear();

        }

        @Override
        public void flushGroup(String group) {

            hitMap.clear();
        }

        @Override
        public void flushAll() {
            hitMap.clear();

        }

        @Override
        public void flushAlLocalOnly(boolean ignoreDistributed) {
            hitMap.clear();

        }
    };



}
