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
        query.append("UPDATE identifier SET owner=mod_user, create_date=idate, asset_subtype=velocity_var_name from\n")
                .append("(SELECT DISTINCT temp.identifier myID, temp.mod_user, inode.idate, \n")
                .append("struc.velocity_var_name  FROM contentlet temp, inode, structure struc,\n")
                .append("(SELECT identifier, MIN(idate) idate FROM contentlet, inode \n")
                .append("WHERE inode.inode=contentlet.inode GROUP BY identifier) custom_select  \n")
                .append("WHERE temp.identifier=custom_select.identifier\n")
                .append("AND inode.inode=temp.inode AND inode.idate=custom_select.idate \n")
                .append("AND temp.structure_inode = struc.inode) my_query\n")
                .append(" WHERE  id=myID;\n");


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
        query.append("UPDATE identifier ident,\n")
                .append("(SELECT DISTINCT temp.identifier myID, temp.mod_user, inode.idate, ")
                .append("struc.velocity_var_name FROM contentlet temp, inode, structure struc,\n")
                .append("(SELECT identifier, MIN(idate) AS idate FROM contentlet, inode WHERE ")
                .append("inode.inode=contentlet.inode GROUP BY identifier) custom_select\n")
                .append("WHERE temp.identifier=custom_select.identifier\n")
                .append("AND inode.inode=temp.inode AND inode.idate=custom_select.idate \n")
                .append("AND temp.structure_inode = struc.inode) my_query\n")
                .append("SET ident.owner=my_query.mod_user, ident.create_date=my_query.idate,\n")
                .append("ident.asset_subtype=my_query.velocity_var_name\n")
                .append("WHERE ident.id=my_query.myID;");

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
        query.append("UPDATE identifier tt SET (tt.owner, tt.asset_subtype, tt.create_date) = ")
                .append("(SELECT st.mod_user, st.velocity_var_name, st.idate \n")
                .append("FROM (SELECT * FROM \n")
                .append("(SELECT DISTINCT temp.identifier myID, temp.mod_user, inode.idate, struc.velocity_var_name,\n")
                .append("row_number() OVER ( PARTITION BY temp.identifier ORDER BY inode.idate desc ) AS r\n")
                .append("FROM contentlet temp, inode, structure struc,\n")
                .append("(SELECT identifier, MIN(idate) AS idate FROM contentlet, inode ")
                .append("WHERE inode.inode=contentlet.inode GROUP BY identifier) custom_select\n")
                .append("WHERE temp.identifier=custom_select.identifier\n")
                .append("AND inode.inode=temp.inode AND inode.idate=custom_select.idate \n")
                .append("AND temp.structure_inode = struc.inode) WHERE r = 1) st WHERE st.myid = tt.id)\n")
                .append("WHERE  EXISTS (SELECT 1/0\n")
                .append("               FROM   (SELECT * FROM \n")
                .append("(SELECT DISTINCT temp.identifier myID, temp.mod_user, inode.idate, struc.velocity_var_name,\n")
                .append("row_number() OVER ( PARTITION BY temp.identifier ORDER BY inode.idate desc ) AS r\n")
                .append("FROM contentlet temp, inode, structure struc,\n")
                .append("(SELECT identifier, MIN(idate) as idate FROM contentlet, inode ")
                .append("WHERE inode.inode=contentlet.inode GROUP BY identifier) custom_select\n")
                .append("WHERE temp.identifier=custom_select.identifier\n")
                .append("AND inode.inode=temp.inode and inode.idate=custom_select.idate \n")
                .append("AND temp.structure_inode = struc.inode) WHERE r = 1)\n")
                .append("               WHERE  myID = tt.id)");

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

        return query.append("UPDATE identifier tt SET (tt.owner, tt.create_date) = (SELECT st.iowner, st.idate \n")
                .append("FROM (SELECT * FROM \n")
                .append("(SELECT DISTINCT temp.identifier myID, owner iowner, inode.idate idate, \n")
                .append("row_number() OVER ( PARTITION BY temp.identifier ORDER BY inode.idate desc ) as r \n")
                .append("FROM ").append(tableName).append(" temp, inode, \n")
                .append("(SELECT identifier, MIN(idate) idate FROM ")
                .append(tableName).append(", inode \n")
                .append("WHERE inode.inode=").append(tableName).append(".inode GROUP BY identifier) custom_select \n")
                .append("WHERE temp.identifier=custom_select.identifier \n")
                .append("AND inode.inode=temp.inode AND inode.idate=custom_select.idate) WHERE r = 1) st WHERE st.myid = tt.id) \n")
                .append("WHERE  EXISTS (SELECT 1/0 \n")
                .append("FROM (SELECT * FROM \n")
                .append("(SELECT DISTINCT temp.identifier myID, owner iowner, inode.idate idate, \n")
                .append("row_number() OVER ( PARTITION BY temp.identifier ORDER BY inode.idate DESC ) AS r \n")
                .append("FROM ").append(tableName).append(" temp, inode, \n")
                .append("(SELECT identifier, MIN(idate) idate FROM ")
                .append(tableName).append(", inode \n")
                .append("WHERE inode.inode=").append(tableName).append(".inode GROUP BY identifier) custom_select \n")
                .append("WHERE temp.identifier=custom_select.identifier \n")
                .append("AND inode.inode=temp.inode AND inode.idate=custom_select.idate) WHERE r = 1) ")
                .append("WHERE  myID = tt.id);").toString();


    }

    private String getQueryToUpdateNonContentletsPostgres(final String tableName){
        final StringBuilder query = new StringBuilder();
        return query.append("UPDATE identifier SET owner=iowner, create_date=idate FROM\n")
                .append("(SELECT DISTINCT temp.identifier myID, owner iowner, inode.idate idate from ")
                .append(tableName).append(" temp, inode,\n")
                .append("(SELECT identifier, MIN(idate) idate from ")
                .append(tableName).append(", inode \n")
                .append("WHERE inode.inode=").append(tableName).append(".inode GROUP BY identifier) custom_select  \n")
                .append(" WHERE temp.identifier=custom_select.identifier\n")
                .append(" AND inode.inode=temp.inode AND inode.idate=custom_select.idate) my_query\n")
                .append(" WHERE id=myID;\n").toString();
    }

    private String getQueryToUpdateNonContentletsMySQL(final String tableName){
        final StringBuilder query = new StringBuilder();
        return query.append("UPDATE identifier ident,\n")
                .append("(SELECT DISTINCT temp.identifier myID, owner iowner, inode.idate idate  FROM ")
                .append(tableName).append(" temp, inode,\n")
                .append("(SELECT identifier, MIN(idate) idate FROM ").append(tableName)
                .append(", inode \n").append("WHERE inode.inode=")
                .append(tableName).append(".inode GROUP BY identifier) custom_select  \n")
                .append("WHERE temp.identifier=custom_select.identifier\n")
                .append("AND inode.inode=temp.inode AND inode.idate=custom_select.idate) my_query\n")
                .append("SET ident.owner=my_query.iowner, ident.create_date=my_query.idate\n")
                .append("WHERE ident.id=my_query.myID;\n").toString();
    }
}
