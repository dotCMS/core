package com.dotmarketing.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Tree;
import com.google.common.collect.ImmutableList;

public class DBTreeTransformer {
	private final List<Tree> _trees;
	
	public DBTreeTransformer(final List<Map<String, Object>> trees){
		this._trees = listToTreeList(trees);
	}
	public DBTreeTransformer(final Map<String, Object> tree){
		this._trees = ImmutableList.of(mapToTree(tree));
	}
	
	public List<Tree> trees(){
		return _trees;
	}
	public Tree tree(){
		return _trees.size()>0 ? _trees.get(0) : new Tree();
	}
	
	private List<Tree> listToTreeList(final List<Map<String, Object>> mapList){
		List<Tree> trees = new ArrayList<>();
		if(mapList!=null) {
			for(Map<String, Object> m : mapList){
				trees.add(mapToTree(m));
			}
		}
		return ImmutableList.copyOf(trees);
	}
	
	private Tree mapToTree(final Map<String, Object> resultMap){
		final Tree tree  = new Tree();
		tree.setChild(resultMap.get("child").toString());
		tree.setParent(resultMap.get("parent").toString());
		tree.setRelationType(resultMap.get("relation_type").toString());
		tree.setTreeOrder(Integer.parseInt(resultMap.get("tree_order").toString()));
		return tree;
	}
	
}
