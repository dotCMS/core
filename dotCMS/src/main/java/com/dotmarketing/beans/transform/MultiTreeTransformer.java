package com.dotmarketing.beans.transform;

import com.dotcms.util.ConversionUtils;
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

    private final ArrayList<MultiTree> list = new ArrayList<>();

    @Override
    public List<MultiTree> asList() {
        return (List)this.list.clone();
    }

    public MultiTreeTransformer(final List<Map<String, Object>> initList) {
        if (initList != null) {
            for (final Map<String, Object> map : initList) {
                this.list.add(transform(map));
            }
        }
    }

    @NotNull
    private static MultiTree transform(final Map<String, Object> map) {
        final MultiTree mt = new MultiTree();
        mt.setContentlet((String) map.get(CHILD));
        mt.setHtmlPage((String) map.get(PARENT1));
        mt.setContainer((String) map.get(PARENT2));
        if (UtilMethods.isSet(map.get(RELATION_TYPE))) {
            mt.setRelationType((String) map.get(RELATION_TYPE));
        }
        if (UtilMethods.isSet(map.get(TREE_ORDER))) {
            mt.setTreeOrder(ConversionUtils.toInt(map.get(TREE_ORDER), 0));
        }
        return mt;
    }
}