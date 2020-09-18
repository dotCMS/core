package com.dotmarketing.business;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Template instances
 */
public class ContentletVersionInfoTransformer implements DBTransformer<ContentletVersionInfo> {
    final List<ContentletVersionInfo> list;


    public ContentletVersionInfoTransformer(List<Map<String, Object>> initList){
        List<ContentletVersionInfo> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<ContentletVersionInfo> asList() {
        return this.list;
    }

    @NotNull
    private static ContentletVersionInfo transform(Map<String, Object> map)  {
        final ContentletVersionInfo versionInfo = new ContentletVersionInfo();
        versionInfo.setIdentifier(String.valueOf(map.get("identifier")));
        versionInfo.setLang(ConversionUtils.toLong(map.get("lang"), 0L));
        versionInfo.setWorkingInode(String.valueOf(map.get("working_inode")));
        versionInfo.setLiveInode(String.valueOf(map.get("live_inode")));
        versionInfo.setDeleted(ConversionUtils.toBooleanFromDb(map.get("deleted")));
        versionInfo.setLockedBy((String) map.get("locked_by"));
        versionInfo.setLockedOn((Date) map.get("locked_on"));
        versionInfo.setVersionTs((Date) map.get("version_ts"));
        return versionInfo;
    }
}
