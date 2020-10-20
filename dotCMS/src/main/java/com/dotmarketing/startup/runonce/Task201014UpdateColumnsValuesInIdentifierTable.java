package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Task used to populate fields: owner, create_date, asset_subtype of the identifier table
 */
public class Task201014UpdateColumnsValuesInIdentifierTable implements StartupTask {

    protected static final String UPDATE_CONTENTLETS =
            "update identifier set owner=q.i_owner, create_date=q.create_date, asset_subtype=q.asset_subtype from"
            + "(select cont.identifier, cont.mod_user i_owner, inode.idate create_date, "
            + "struc.velocity_var_name asset_subtype from contentlet cont, "
            + "inode inode, structure struc "
            + "where inode.inode=cont.inode "
            + "and cont.structure_inode = struc.inode) q where id=q.identifier";

    private static final String UPDATE_FOLDER = "update identifier set owner=q.i_owner, create_date=q.create_date from "
            + "(select identifier, owner i_owner, idate create_date from folder, inode "
            + "where inode.inode=folder.inode) q where id=q.identifier";

    private static final String UPDATE_CONTAINERS="update identifier set owner=q.i_owner, create_date=q.create_date from "
            + "(select identifier, owner i_owner, idate create_date from dot_containers, inode "
            + "where inode.inode=dot_containers.inode) q where id=q.identifier";

    protected static final String UPDATE_TEMPLATES = "update identifier set owner=q.i_owner, create_date=q.create_date from "
            + "(select identifier, owner i_owner, idate create_date from template, inode "
            + "where inode.inode=template.inode) q where id=q.identifier";

    protected final static String UPDATE_LINKS="update identifier set owner=q.i_owner, create_date=q.create_date from "
            + "(select identifier, owner i_owner, idate create_date from links, inode "
            + "where inode.inode=links.inode) q where id=q.identifier";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final DotConnect dotConnect = new DotConnect().setSQL(UPDATE_CONTENTLETS);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for folders
        dotConnect.setSQL(UPDATE_FOLDER);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for containers
        dotConnect.setSQL(UPDATE_CONTAINERS);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for templates
        dotConnect.setSQL(UPDATE_TEMPLATES);
        dotConnect.loadResult();

        // update owner, create_date and asset_subtype for links
        dotConnect.setSQL(UPDATE_LINKS);
        dotConnect.loadResult();

    }
}
