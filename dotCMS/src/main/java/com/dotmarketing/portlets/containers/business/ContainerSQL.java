package com.dotmarketing.portlets.containers.business;

public class ContainerSQL {

    private static ContainerSQL instance;

    private ContainerSQL(){
    }
    public static ContainerSQL getInstance() {
        if (instance == null) {
            instance = new ContainerSQL();
        }

        return instance;
    }

    public static final String FIND_BY_INODE = "select container.*, inode.* from dot_containers container, inode inode "
            + "where container.inode = inode.inode AND container.inode = ?";

    public static final String INSERT_INODE = "INSERT INTO inode (inode, idate, owner, type) VALUES (?,?,?,'containers')";
    public static final String INSERT_CONTAINER = "INSERT INTO dot_containers(inode, code, pre_loop, post_loop, show_on_menu, " +
            "title, mod_date, mod_user, sort_order, friendly_name, max_contentlets, use_div, staticify, " +
            "sort_contentlets_by, lucene_query, notes, identifier) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    public static final String UPDATE_INODE = "UPDATE inode SET idate = ?, owner = ? WHERE inode = ? AND type='containers'";

    public static final String UPDATE_CONTAINER = "UPDATE dot_containers SET code = ?, pre_loop = ?, " +
            "post_loop = ?, show_on_menu = ?, title = ?, mod_date = ?, mod_user = ?, sort_order = ?, friendly_name = ?, " +
            "max_contentlets = ?, use_div = ?, staticify = ?, sort_contentlets_by = ?, lucene_query = ?, notes = ?, identifier = ? " +
            "WHERE inode = ?";
}