package com.dotmarketing.portlets.categories.business;

class MSSQLCategorySQL extends CategorySQL{
	
	public String getCreateSortTopLevel() {
		return " SELECT category.inode, row_number() over (order by sort_order) rnum " +
				" into category_reorder from category left join tree tree on category.inode = tree.child, " + 
				" inode category_1_ where tree.child is null and category_1_.inode = category.inode and category_1_.type = 'category' " +
				" order by sort_order; ";
	}
	
	public String getUpdateSort() {
		return "update category set sort_order = ( " + 
				" select rnum from category_reorder innerr where innerr.inode = category.inode) " +
				" where  exists(select 1 from category_reorder cat where cat.inode = category.inode) ";
	}
	
	public String getDropSort() {
		return "drop table category_reorder";
	}
	
	public String getCreateSortChildren(String inode) {
		return " SELECT category.inode, row_number() over (order by sort_order) rnum " +
				" into category_reorder from inode category_1_, category, tree where " +
				"category.inode = tree.child and tree.parent = '" + inode + "' and category_1_.inode = category.inode " +
				" and category_1_.type = 'category' order by sort_order; ";
	}

}
