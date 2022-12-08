package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.Inode.Type;

public class ContainerSQL {

    private static ContainerSQL _instance;

    private ContainerSQL(){
    }
    public static ContainerSQL getInstance() {
        if (_instance == null) {
            _instance = new ContainerSQL();
        }

        return _instance;
    }

    public static final String FIND_BY_INODE = "select container.* from dot_containers container "
            + "where inode = ?";

    public static final String INSERT_INODE = "INSERT INTO public.inode (inode, idate, owner, type) VALUES (?,?,?,'containers')";
    public static final String INSERT_CONTAINER = "INSERT INTO public.dot_containers(inode, code, pre_loop, post_loop, show_on_menu, " +
            "title, mod_date, mod_user, sort_order, friendly_name, max_contentlets, use_div, staticify, " +
            "sort_contentlets_by, lucene_query, notes, identifier) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    public static final String UPDATE_INODE = "UPDATE public.inode SET idate = ?, owner = ? WHERE inode = ? AND type='containers'";

    public static final String UPDATE_CONTAINER = "UPDATE public.dot_containers SET code = ?, pre_loop = ?, " +
            "post_loop = ?, show_on_menu = ?, title = ?, mod_date = ?, mod_user = ?, sort_order = ?, friendly_name = ?, " +
            "max_contentlets = ?, use_div = ?, staticify = ?, sort_contentlets_by = ?, lucene_query = ?, notes = ?, identifier = ? " +
            "WHERE inode = ?";
}
