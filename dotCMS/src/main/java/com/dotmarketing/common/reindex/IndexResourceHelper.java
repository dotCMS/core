package com.dotmarketing.common.reindex;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotmarketing.business.APILocator;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;


public class IndexResourceHelper {

    private static class IndexResourceHelperHolder{
        private static IndexResourceHelper helper = new IndexResourceHelper();
    }
    ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
    ESIndexAPI esapi = APILocator.getESIndexAPI();
    
    private IndexResourceHelper() {
        

    }

    
    static IndexResourceHelper getInstance() {
        return IndexResourceHelperHolder.helper;
    }
    


    public List<Map<String,Object>> indexStatsList()  {


        Map<String,ClusterIndexHealth> clusterHealth = esapi.getClusterHealth();
        List<String> openIndicies=idxApi.listDotCMSIndices();
        List<String> closedIndices=idxApi.listDotCMSClosedIndices();
        List<String> currentIdx = Try.of(()->idxApi.getCurrentIndex()).getOrElse(ImmutableList.of());
        List<String> newIdx =Try.of(()->idxApi.getNewIndex()).getOrElse(ImmutableList.of());
        Map<String, IndexStats> indexInfo = esapi.getIndicesStats();
        List<Map<String,Object>> indexList = new ArrayList<>();
        
        
        
        openIndicies.stream().forEach(index->{
            Map<String, Object> indexStats = new HashMap<>();
            indexStats.put("active", currentIdx.contains(index));
            indexStats.put("building", newIdx.contains(index));
            indexStats.put("closed", false);
            indexStats.put("indexName", index);
            indexStats.put("status", indexInfo.get(index));
            indexStats.put("health", clusterHealth.get(index));
            indexStats.put("created", indexDate(index));
            indexList.add(indexStats);
        });
        
        closedIndices.stream().forEach(index->{
            Map<String, Object> indexStats = new HashMap<>();
            indexStats.put("active", false);
            indexStats.put("building", false);
            indexStats.put("closed", true);
            indexStats.put("indexName", index);
            indexStats.put("created", indexDate(index));
            indexStats.put("health", clusterHealth.get(index));
            
            indexList.add(indexStats);
        });
        
        
        

        return indexList;

    }
    

    private Date indexDate(String indexName) {
        
        return Try.of(()->new SimpleDateFormat("yyyyMMddHHmmss").parse(indexName.split("_")[1])).getOrElse(new Date());
        
        
        
    }
    
    
    
    
    
    

}
