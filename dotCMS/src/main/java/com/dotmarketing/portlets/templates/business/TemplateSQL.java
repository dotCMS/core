package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.beans.Inode.Type;

public class TemplateSQL {

    public static TemplateSQL getInstance(){ return new TemplateSQL(); }

    public static final String FIND_TEMPLATES_BY_HOST_INODE =
            "select template.*, template_1_.*  from " + Type.TEMPLATE.getTableName() + " template, inode template_1_, " +
                    "identifier template_identifier, " + Type.TEMPLATE.getVersionTableName() + " vi where " +
                    "template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
                    "template.inode = template_1_.inode and vi.identifier=template.identifier and " +
                    "template.inode=vi.working_inode ";

    public static final String FIND_WORKING_TEMPLATE_BY_HOST_INODE_AND_TITLE =
            "select template.*, template_1_.* from " + Type.TEMPLATE.getTableName() + " template, inode template_1_, " +
                    "identifier template_identifier, " + Type.TEMPLATE.getVersionTableName() + " vi where " +
                    "template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
                    "vi.identifier=template_identifier.id and template.title = ? and " +
                    "template.inode = template_1_.inode and " +
                    "template.inode=vi.working_inode ";

    public static final String SELECT_ALL_FIELDS = "select inode, show_on_menu, title, mod_date, mod_user, " +
            "sort_order, friendly_name, body, header, footer, image, identifier, drawed, drawed_body, " +
            "add_container_links, containers_added, head_code, theme from " + Type.TEMPLATE.getTableName();

    public static final String FIND_BY_INODE = SELECT_ALL_FIELDS + " where inode = ?";

    public static final String INSERT_INODE = "insert into inode (inode, idate, owner, type) values (?,?,?,'template')";

    public static final String INSERT_TEMPLATE = "insert into template (inode, show_on_menu, title, mod_date, mod_user, " +
            "sort_order, friendly_name, body, header, footer, image, identifier, drawed, drawed_body, " +
            "add_container_links, containers_added, head_code, theme) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String UPDATE_INODE = "update inode set idate = ?, owner = ? where inode = ? and type='template'";

    public static final String UPDATE_TEMPLATE = "update template set show_on_menu = ?, title = ?, mod_date = ?, " +
            "mod_user = ?, sort_order = ?, friendly_name = ?, body = ?, header = ?, footer = ?, image = ?, identifier = ?, " +
            "drawed = ?, drawed_body = ?, add_container_links = ?, containers_added = ?, head_code = ?, theme = ? where inode = ?";


}
