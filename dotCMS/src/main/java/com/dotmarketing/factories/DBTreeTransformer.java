package com.dotmarketing.factories;

import com.dotmarketing.beans.Tree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;


public class DBTreeTransformer{
    
    private final List<Tree> trees;
    
    public DBTreeTransformer (List<Map<String, Object>>list ){
        this.trees =  list
        .stream()
        .map(row -> toTree(row))
        .collect(Collectors.toList());

    }
    
    public Tree tree() {
        return trees.stream().findFirst().orElse(null);
       
    }
    public List<Tree> trees() {
        return trees;
        
    }
    
    
    
    public DBTreeTransformer (Map<String, Object> map){
        this.trees = Lists.newArrayList(toTree(map));
        
        
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