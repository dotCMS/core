package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.beans.Inode.Type;

public class TemplateSQL {

    private static TemplateSQL instance;

    private TemplateSQL(){
    }
    public static TemplateSQL getInstance() {
        if (instance == null) {
            instance = new TemplateSQL();
        }

        return instance;
    }

    public static final String FIND_TEMPLATES_BY_HOST_INODE =
            "select template.*, template_identifier.*  from " + Type.TEMPLATE.getTableName() + " template, inode template_1_, " +
                    "identifier template_identifier, " + Type.TEMPLATE.getVersionTableName() + " vi where " +
                    "template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
                    "template.inode = template_1_.inode and vi.identifier=template.identifier and " +
                    "template.inode=vi.working_inode ";

    public static final String FIND_WORKING_TEMPLATE_BY_HOST_INODE_AND_TITLE =
            "select template.*, template_identifier.* from " + Type.TEMPLATE.getTableName() + " template, inode template_1_, " +
                    "identifier template_identifier, " + Type.TEMPLATE.getVersionTableName() + " vi where " +
                    "template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
                    "vi.identifier=template_identifier.id and template.title = ? and " +
                    "template.inode = template_1_.inode and " +
                    "template.inode=vi.working_inode ";

    public static final String FIND_BY_INODE = "select template.*, template_identifier.* from template template, identifier template_identifier "
            + "where inode = ? and template_identifier.id = template.identifier";

    public static final String INSERT_INODE = "insert into inode (inode, idate, owner, type) values (?,?,?,'template')";

    public static final String INSERT_TEMPLATE = "insert into template (inode, show_on_menu, title, mod_date, mod_user, " +
            "sort_order, friendly_name, body, header, footer, image, identifier, drawed, drawed_body, " +
            "add_container_links, containers_added, head_code, theme) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String UPDATE_INODE = "update inode set idate = ?, owner = ? where inode = ? and type='template'";

    public static final String UPDATE_TEMPLATE = "update template set show_on_menu = ?, title = ?, mod_date = ?, " +
            "mod_user = ?, sort_order = ?, friendly_name = ?, body = ?, header = ?, footer = ?, image = ?, identifier = ?, " +
            "drawed = ?, drawed_body = ?, add_container_links = ?, containers_added = ?, head_code = ?, theme = ? where inode = ?";

    public static final String DELETE_INODE = "delete from inode where inode = ? and type='template'";

    public static final String DELETE_TEMPLATE_BY_INODE = "delete from template where inode = ?";

    public static final String FIND_TEMPLATES_BY_CONTAINER_INODE = "SELECT template.*, template_identifier.* from template template, tree tree, inode inode, identifier template_identifier "
            + "where tree.parent = ? and tree.child = template.inode and inode.inode = template.inode and inode.type = 'template'";

    public static final String FIND_ALL_VERSIONS_BY_IDENTIFIER = "SELECT inode FROM template WHERE identifier=? order by mod_date desc";

    public static final String FIND_WORKING_LIVE_VERSION_BY_IDENTIFIER = "SELECT inode FROM template t INNER JOIN template_version_info tvi "
            + "ON (t.inode = tvi.working_inode OR t.inode = tvi.live_inode) "
            + "WHERE t.identifier=? order by t.mod_date desc";

    public static final String FIND_TEMPLATES_BY_MOD_USER="select inode from template where mod_user = ?";

    public static final String UPDATE_MOD_USER_BY_MOD_USER = "UPDATE template set mod_user = ? where mod_user = ?";

    public static final String UPDATE_LOCKED_BY = "update " + Type.TEMPLATE.getVersionTableName() + " set locked_by=? where locked_by  = ?";

    public static final String GET_PAGES_BY_TEMPLATE_ID = " SELECT c.identifier, c.variant_id as variant FROM contentlet c WHERE EXISTS " +
            "(SELECT 1 FROM structure s WHERE s.inode = c.structure_inode AND s.structuretype = '5') AND c.contentlet_as_json->'fields'->'template'->>'value' = ? ";

}
