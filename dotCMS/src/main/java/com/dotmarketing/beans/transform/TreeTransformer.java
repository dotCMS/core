package com.dotmarketing.beans.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.Tree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;


public class TreeTransformer implements DBTransformer<Tree> {
    
    private final List<Tree> trees;
    
    public TreeTransformer(List<Map<String, Object>>list ){
        this.trees =  list
        .stream()
        .map(this::toTree)
        .collect(Collectors.toList());
    }

    public TreeTransformer(Map<String, Object> map){
        this.trees = Lists.newArrayList(toTree(map));
    }

    @Override
    public List<Tree> asList() {
        return trees;
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