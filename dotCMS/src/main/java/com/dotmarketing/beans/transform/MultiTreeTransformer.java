package com.dotmarketing.beans.transform;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultiTreeTransformer implements DBTransformer<MultiTree> {

    //MultiTree fields
    private static final String CHILD = "child";
    private static final String PARENT1 = "parent1";
    private static final String PARENT2 = "parent2";
    private static final String RELATION_TYPE = "relation_type";
    private static final String TREE_ORDER = "tree_order";
    private static final String PERSONALIZATION = "personalization";

    private final ArrayList<MultiTree> list = new ArrayList<>();

    @Override
    public List<MultiTree> asList() {
        return this.list;
    }

    public MultiTreeTransformer(final List<Map<String, Object>> initList) {
        if (initList != null) {
            for (final Map<String, Object> map : initList) {
                this.list.add(transform(map));
            }
        }
    }

    private static MultiTree transform(final Map<String, Object> map) {

        final MultiTree multiTree = new MultiTree();
        multiTree.setContentlet((String) map.get(CHILD));
        multiTree.setHtmlPage((String) map.get(PARENT1));
        multiTree.setContainer((String) map.get(PARENT2));
        multiTree.setPersonalization((String) map.get(PERSONALIZATION));
        if (UtilMethods.isSet(map.get(RELATION_TYPE))) {
            multiTree.setRelationType((String) map.get(RELATION_TYPE));
        }
        if (UtilMethods.isSet(map.get(TREE_ORDER))) {
            multiTree.setTreeOrder(ConversionUtils.toInt(map.get(TREE_ORDER), 0));
        }

        return multiTree;
    }
}