package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Task used to populate fields: owner, create_date, asset_subtype of the identifier table
 */
public class Task201014UpdateColumnsValuesInIdentifierTable implements StartupTask {

    protected static final String UPDATE_CONTENTLETS =
            "update identifier as ident, "
                    + "(select cont.identifier, cont.mod_user i_owner, inode.idate create_date, struc.velocity_var_name asset_subtype "
                    + "from contentlet cont, inode inode, structure struc where inode.inode=cont.inode and cont.structure_inode = struc.inode) "
                    + "as q set ident.owner=q.i_owner, ident.create_date=q.create_date, ident.asset_subtype=q.asset_subtype  where ident.id=q.identifier";

    protected static final String UPDATE_CONTENTLETS_POSTGRESQL =
            "update identifier set owner=q.i_owner, create_date=q.create_date, asset_subtype=q.asset_subtype from "
                    + "(select cont.identifier, cont.mod_user i_owner, inode.idate create_date, "
                    + "struc.velocity_var_name asset_subtype from contentlet cont, "
                    + "inode inode, structure struc "
                    + "where inode.inode=cont.inode "
                    + "and cont.structure_inode = struc.inode) q where id=q.identifier";

    private static final String UPDATE_FOLDER = "update identifier as ident, "
            + "(select identifier, owner i_owner, idate create_date from folder, inode where inode.inode=folder.inode) as q "
            + "set ident.owner=q.i_owner, ident.create_date=q.create_date "
            + "where ident.id=q.identifier";

    private static final String UPDATE_FOLDER_POSTGRESQL =
            "update identifier set owner=q.i_owner, create_date=q.create_date from "
                    + "(select identifier, owner i_owner, idate create_date from folder, inode "
                    + "where inode.inode=folder.inode) q where id=q.identifier";

    private static final String UPDATE_CONTAINERS = "update identifier as ident, "
            + "(select identifier, owner i_owner, idate create_date from dot_containers, inode where inode.inode=dot_containers.inode) as q "
            + "set ident.owner=q.i_owner, ident.create_date=q.create_date "
            + "where ident.id=q.identifier";

    private static final String UPDATE_CONTAINERS_POSTGRESQL =
            "update identifier set owner=q.i_owner, create_date=q.create_date from "
                    + "(select identifier, owner i_owner, idate create_date from dot_containers, inode "
                    + "where inode.inode=dot_containers.inode) q where id=q.identifier";

    private static final String UPDATE_TEMPLATES = "update identifier as ident, "
            + "(select identifier, owner i_owner, idate create_date from template, inode where inode.inode=template.inode) as q "
            + "set ident.owner=q.i_owner, ident.create_date=q.create_date "
            + "where ident.id=q.identifier";

    protected static final String UPDATE_TEMPLATES_POSTGRESQL =
            "update identifier set owner=q.i_owner, create_date=q.create_date from "
                    + "(select identifier, owner i_owner, idate create_date from template, inode "
                    + "where inode.inode=template.inode) q where id=q.identifier";

    private static final String UPDATE_LINKS = "update identifier as ident, "
            + "(select identifier, owner i_owner, idate create_date from links, inode where inode.inode=links.inode) as q "
            + "set ident.owner=q.i_owner, ident.create_date=q.create_date "
            + "where ident.id=q.identifier";

    protected final static String UPDATE_LINKS_POSTGRESQL =
            "update identifier set owner=q.i_owner, create_date=q.create_date from "
                    + "(select identifier, owner i_owner, idate create_date from links, inode "
                    + "where inode.inode=links.inode) q where id=q.identifier";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final DotConnect dotConnect = new DotConnect()
                .setSQL(DbConnectionFactory.isPostgres() ? UPDATE_CONTENTLETS_POSTGRESQL
                        : UPDATE_CONTENTLETS);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for folders
        dotConnect.setSQL(DbConnectionFactory.isPostgres() ? UPDATE_FOLDER_POSTGRESQL
                : UPDATE_FOLDER);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for containers
        dotConnect.setSQL(DbConnectionFactory.isPostgres() ? UPDATE_CONTAINERS_POSTGRESQL
                : UPDATE_CONTAINERS);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for templates
        dotConnect.setSQL(DbConnectionFactory.isPostgres() ? UPDATE_TEMPLATES_POSTGRESQL
                : UPDATE_TEMPLATES);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for links
        dotConnect
                .setSQL(DbConnectionFactory.isPostgres() ? UPDATE_LINKS_POSTGRESQL : UPDATE_LINKS);
        dotConnect.loadResult();

    }
}
