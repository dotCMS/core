package com.dotmarketing.beans.transform;

import com.dotmarketing.beans.Tree;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OracleTreeTransformer extends TreeTransformer {

    public OracleTreeTransformer(List<Map<String, Object>> list) {
        super(list);
    }

    @Override
    Tree toTree(Map<String, Object> map) {
        Tree tree=new Tree();
        tree.setParent((String)map.getOrDefault("parent", null));
        tree.setChild((String)map.getOrDefault("child", null));
        tree.setRelationType((String)map.getOrDefault("relation_type", null));
        tree.setTreeOrder(((BigDecimal)map.getOrDefault("tree_order", 0)).intValue());

        return tree;
    }
}
