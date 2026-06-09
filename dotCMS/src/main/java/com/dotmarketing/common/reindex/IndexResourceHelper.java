package com.dotmarketing.common.reindex;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.MigrationIndexVisibility;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.IndexStats;
import com.dotmarketing.business.APILocator;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;


public class IndexResourceHelper {

    private static class IndexResourceHelperHolder{
        private static final IndexResourceHelper helper = new IndexResourceHelper();
    }
    ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
    IndexAPI esapi = APILocator.getESIndexAPI();
    
    private IndexResourceHelper() {
        

    }

    
    public static IndexResourceHelper getInstance() {
        return IndexResourceHelperHolder.helper;
    }
    


    public List<Map<String,Object>> indexStatsList(final User user)  {


        Map<String,ClusterIndexHealth> clusterHealth = esapi.getClusterHealth();
        // Hide OS-tagged (.os) migration indices from the maintenance dashboard outside Phase 3,
        // unless the acting user holds the configured QA/preview role. Operational paths keep the
        // full set; only this display sink filters — see MigrationIndexVisibility.
        List<String> openIndices=MigrationIndexVisibility.filter(idxApi.listDotCMSIndices(), user);
        List<String> closedIndices=MigrationIndexVisibility.filter(idxApi.listDotCMSClosedIndices(), user);
        List<String> currentIdx = Try.of(()->idxApi.getCurrentIndex()).getOrElse(ImmutableList.of());
        List<String> newIdx =Try.of(()->idxApi.getNewIndex()).getOrElse(ImmutableList.of());
        Map<String, IndexStats> indexInfo = esapi.getIndicesStats();
        List<Map<String,Object>> indexList = new ArrayList<>();
        
        
        
        openIndices.forEach(index->{
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
        
        closedIndices.forEach(index->{
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
