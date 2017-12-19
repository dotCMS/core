package com.dotmarketing.beans.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class MultiTreeTransformer implements DBTransformer<MultiTree> {

    //MultiTree fields
    private static final String CHILD = "child";
    private static final String PARENT1 = "parent1";
    private static final String PARENT2 = "parent2";
    private static final String RELATION_TYPE = "relation_type";
    private static final String TREE_ORDER = "tree_order";

    private final List<MultiTree> list = new ArrayList<>();

    @Override
    public List<MultiTree> asList() {
        return this.list;
    }

    public MultiTreeTransformer(List<Map<String, Object>> initList){
        if (initList != null){
            for(Map<String, Object> map : initList){
                this.list.add(transform(map));
            }
        }
    }

    @NotNull
    private static MultiTree transform(Map<String, Object> map) {
        MultiTree mt = new MultiTree();
        mt.setChild((String) map.get(CHILD));
        mt.setParent1((String) map.get(PARENT1));
        mt.setParent2((String) map.get(PARENT2));
        if (UtilMethods.isSet(map.get(RELATION_TYPE))) {
            mt.setRelationType((String) map.get(RELATION_TYPE));
        }
        if (UtilMethods.isSet(map.get(TREE_ORDER))) {
            mt.setTreeOrder((Integer) map.get(TREE_ORDER));
        }
        return mt;
    }
}
