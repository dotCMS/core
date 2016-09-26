package com.dotmarketing.portlets.categories.business;

class MySQLCategorySQL extends CategorySQL{

	public String getCreateSortTopLevel() {
		return " create table category_reorder as " +
				" SELECT category.inode, @rownum:=@rownum+1 rnum from (SELECT @rownum:=0) r, category left join tree tree on category.inode = tree.child, " +
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

	public String getCreateSortChildren(String inode) {
		return " create table category_reorder as " +
				" SELECT inode , @rownum:=@rownum+1 rnum from (SELECT @rownum:=0) r,  (  " +
				" SELECT category.inode from inode category_1_, category, tree where " +
				"category.inode = tree.child and tree.parent = '" + inode + "' and category_1_.inode = category.inode " +
				" and category_1_.type = 'category'  order by sort_order ) t";
	}

}
