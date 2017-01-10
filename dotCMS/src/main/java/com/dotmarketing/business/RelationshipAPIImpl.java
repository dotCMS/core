package com.dotmarketing.business;

import com.dotmarketing.beans.Tree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.util.InodeUtils;

public class RelationshipAPIImpl implements RelationshipAPI {

	public void addRelationship(String parent,String child, String relationType)throws DotDataException {		
		Tree tree = TreeFactory.getTree(parent, child,relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setParent(parent);
			tree.setChild(child);
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		}
	}

}
