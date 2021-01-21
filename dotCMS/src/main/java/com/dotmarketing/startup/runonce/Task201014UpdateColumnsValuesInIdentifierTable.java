package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

/**
 * Task used to populate fields: owner, create_date, asset_subtype of the identifier table
 */
public class Task201014UpdateColumnsValuesInIdentifierTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {

        final StringBuilder query = new StringBuilder();

        //update templates
        query.append(getQueryToUpdateNonContentletsPostgres("template"));

        //update containers
        query.append(getQueryToUpdateNonContentletsPostgres("dot_containers"));

        //update links
        query.append(getQueryToUpdateNonContentletsPostgres("links"));

        //update folders
        query.append(getQueryToUpdateNonContentletsPostgres("folder"));

        //update contentlets
        query.append("update identifier set owner=mod_user, create_date=idate, asset_subtype=velocity_var_name from\n")
                .append("(select distinct temp.identifier myID, temp.mod_user, inode.idate, struc.velocity_var_name  from contentlet temp, inode, structure struc,\n")
                .append("(select identifier, MIN(idate) idate from contentlet, inode \n")
                .append("where inode.inode=contentlet.inode group by identifier) custom_select  \n")
                .append("where temp.identifier=custom_select.identifier\n")
                .append("and inode.inode=temp.inode and inode.idate=custom_select.idate \n")
                .append("and temp.structure_inode = struc.inode) my_query\n")
                .append(" where  id=myID;\n");


        return query.toString();
    }

    @Override
    public String getMySQLScript() {
        final StringBuilder query = new StringBuilder();

        //update templates
        query.append(getQueryToUpdateNonContentletsMySQL("template"));

        //update containers
        query.append(getQueryToUpdateNonContentletsMySQL("dot_containers"));

        //update links
        query.append(getQueryToUpdateNonContentletsMySQL("links"));

        //update folders
        query.append(getQueryToUpdateNonContentletsMySQL("folder"));

        //update contentlets
        query.append("update identifier ident,\n")
                .append("(select distinct temp.identifier myID, temp.mod_user, inode.idate, ")
                .append("struc.velocity_var_name  from contentlet temp, inode, structure struc,\n")
                .append("(select identifier, MIN(idate) as idate from contentlet, inode where ")
                .append("inode.inode=contentlet.inode group by identifier) custom_select\n")
                .append("where temp.identifier=custom_select.identifier\n")
                .append("and inode.inode=temp.inode and inode.idate=custom_select.idate \n")
                .append("and temp.structure_inode = struc.inode) my_query\n")
                .append("set ident.owner=my_query.mod_user, ident.create_date=my_query.idate,\n")
                .append("ident.asset_subtype=my_query.velocity_var_name\n")
                .append("where ident.id=my_query.myID;");

        return query.toString();
    }

    @Override
    public String getOracleScript() {

        final StringBuilder query = new StringBuilder();

        //update templates
        query.append(getQueryToUpdateNonContentletsOracle("template"));

        //update containers
        query.append(getQueryToUpdateNonContentletsOracle("dot_containers"));

        //update links
        query.append(getQueryToUpdateNonContentletsOracle("links"));

        //update folders
        query.append(getQueryToUpdateNonContentletsOracle("folder"));

        //update contentlets
        query.append("MERGE INTO identifier ident\n")
                .append("    USING\n")
                .append("(select distinct temp.identifier myID, temp.mod_user, inode.idate, struc.velocity_var_name  from contentlet temp, inode, structure struc,\n")
                .append("(select identifier, MIN(idate) as idate from contentlet, inode where inode.inode=contentlet.inode group by identifier) custom_select\n")
                .append("where temp.identifier=custom_select.identifier\n")
                .append("and inode.inode=temp.inode and inode.idate=custom_select.idate \n")
                .append("and temp.structure_inode = struc.inode)\n")
                .append("st ON (ident.id = st.myID) \n")
                .append("WHEN MATCHED THEN\n")
                .append("    UPDATE SET ident.owner = st.mod_user,\n")
                .append("               ident.asset_subtype = st.velocity_var_name,\n")
                .append("               ident.create_date = st.idate\n");

        return query.toString();
    }

    @Override
    public String getMSSQLScript() {
        return getPostgresScript();
    }

    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    private String getQueryToUpdateNonContentletsOracle(final String tableName){
        final StringBuilder query = new StringBuilder();
        return query.append("MERGE INTO identifier ident\n")
                .append("    USING\n")
                .append("(select distinct temp.identifier myID, owner iowner, inode.idate idate from ")
                .append(tableName).append(" temp, inode, \n")
                .append("(select identifier, MIN(idate) as idate from ")
                .append(tableName).append(", inode where inode.inode=")
                .append(tableName).append(".inode group by identifier) custom_select\n")
                .append("where temp.identifier=custom_select.identifier\n")
                .append("and inode.inode=temp.inode and inode.idate=custom_select.idate)\n")
                .append("st ON (ident.id = st.myID) \n")
                .append("WHEN MATCHED THEN\n")
                .append("    UPDATE SET ident.owner = st.iowner,\n")
                .append("               ident.create_date = st.idate;\n").toString();
    }

    private String getQueryToUpdateNonContentletsPostgres(final String tableName){
        final StringBuilder query = new StringBuilder();
        return query.append("update identifier set owner=iowner, create_date=idate from\n")
                .append("(select distinct temp.identifier myID, owner iowner, inode.idate idate from ")
                .append(tableName).append(" temp, inode,\n")
                .append("(select identifier, MIN(idate) idate from ")
                .append(tableName).append(", inode \n")
                .append("where inode.inode=").append(tableName).append(".inode group by identifier) custom_select  \n")
                .append(" where temp.identifier=custom_select.identifier\n")
                .append(" and inode.inode=temp.inode and inode.idate=custom_select.idate) my_query\n")
                .append(" where  id=myID;\n").toString();
    }

    private String getQueryToUpdateNonContentletsMySQL(final String tableName){
        final StringBuilder query = new StringBuilder();
        return query.append("update identifier ident,\n")
                .append("(select distinct temp.identifier myID, owner iowner, inode.idate idate  from ")
                .append(tableName).append(" temp, inode,\n")
                .append("(select identifier, MIN(idate) idate from ").append(tableName)
                .append(", inode \n").append("where inode.inode=")
                .append(tableName).append(".inode group by identifier) custom_select  \n")
                .append("where temp.identifier=custom_select.identifier\n")
                .append("and inode.inode=temp.inode and inode.idate=custom_select.idate) my_query\n")
                .append("set ident.owner=my_query.iowner, ident.create_date=my_query.idate\n")
                .append("where  ident.id=my_query.myID;\n").toString();
    }
}
