package com.dotmarketing.portlets.categories.business;

class OracleCategorySQL extends CategorySQL{
	
	public String getCreateSortTopLevel() {
		return "create table category_reorder as " +
				" SELECT category.inode, row_number() over (order by sort_order) rnum from category left join tree tree on category.inode = tree.child, " +
				" inode category_1_ where tree.child is null and category_1_.inode = category.inode and category_1_.type = 'category' " +
				" order by sort_order ";
	}
	
	public String getUpdateSort() {
		return "update category set sort_order = ( " + 
				" select rnum from category_reorder innerr where innerr.inode = category.inode) " +
				" where  exists(select 1 from category_reorder cat where cat.inode = category.inode) ";
	}
	
	public String getDropSort() {
		return "drop table category_reorder";
	}
	
	public String getCreateSortChildren() {
		return "INSERT INTO category_reorder(inode, rnum) " +
				" SELECT category.inode, row_number() over (order by sort_order) rnum from inode category_1_, category, tree where " +
				"category.inode = tree.child and tree.parent = ? and category_1_.inode = category.inode " +
				" and category_1_.type = 'category' order by sort_order ";
	}

    public String createCategoryReorderTable() {
        return "CREATE TABLE category_reorder(inode VARCHAR2(36), rnum NUMBER)";
    }

}
