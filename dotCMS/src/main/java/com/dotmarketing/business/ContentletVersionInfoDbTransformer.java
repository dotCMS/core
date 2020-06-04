package com.dotmarketing.business;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.google.common.collect.ImmutableList;

public class ContentletVersionInfoDbTransformer implements DBTransformer<ContentletVersionInfo> {

    final List<ContentletVersionInfo> transformed;

    ContentletVersionInfoDbTransformer(Map<String, Object> map) {
        transformed = ImmutableList.of(transform(map));


    }

    ContentletVersionInfoDbTransformer(List<Map<String, Object>> list) {
        transformed = list.stream().map(m->transform(m)).collect(Collectors.toList());


    }

    @Override
    public List<ContentletVersionInfo> asList() {

        return transformed;
    }



    public ContentletVersionInfo from() {

        return asList().get(0);
    }
    /*
     *  
     *  
     identifier    | character varying(36)       |           | not null |
     lang          | bigint                      |           | not null |
     working_inode | character varying(36)       |           | not null |
     live_inode    | character varying(36)       |           |          |
     deleted       | boolean                     |           | not null |
     locked_by     | character varying(100)      |           |          |
     locked_on     | timestamp without time zone |           |          |
     version_ts    | timestamp without time zone |           | not null |
     
     * 
     */

    private ContentletVersionInfo transform(Map<String, Object> map) {
        ContentletVersionInfo cvi = new ContentletVersionInfo();
        cvi.setDeleted((Boolean) map.get("deleted"));
        cvi.setLockedOn((Date) map.get("locked_on"));
        cvi.setIdentifier((String) map.get("identifier"));
        cvi.setLang((long) map.get("lang"));
        cvi.setWorkingInode((String) map.get("working_inode"));
        cvi.setLiveInode((String) map.get("live_inode"));
        cvi.setVersionTs((Date) map.get("version_ts"));
        return cvi;
    }



}
