package com.dotmarketing.beans.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TreeTransformer implements DBTransformer<Tree> {
    
    private final List<Tree> trees;
    
    public TreeTransformer(final List<Map<String, Object>>list ){
        this.trees =  list
        .stream()
        .map(this::toTree)
        .collect(Collectors.toList());
    }

    @Override
    public List<Tree> asList() {
        return new ArrayList<>(trees);
    }

    @Override
    public Tree findFirst() {
        return this.asList().stream().findFirst().orElse(new Tree());
    }
    
    private Tree toTree (Map<String, Object> map){
        
        Tree tree=new Tree();
        tree.setParent((String)map.getOrDefault("parent", null));
        tree.setChild((String)map.getOrDefault("child", null));
        tree.setRelationType((String)map.getOrDefault("relation_type", null));
        tree.setTreeOrder((Integer)map.getOrDefault("tree_order", 0));
        
        return tree;
    }
    
}