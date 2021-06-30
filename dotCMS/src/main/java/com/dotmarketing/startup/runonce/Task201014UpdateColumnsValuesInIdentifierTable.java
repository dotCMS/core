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

        query.append("ALTER TABLE identifier DISABLE TRIGGER ALL;\n");

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


        query.append("ALTER TABLE identifier ENABLE TRIGGER ALL;\n");
        return query.toString();
    }

    @Override
    public String getMySQLScript() {
        final StringBuilder query = new StringBuilder();
        query.append("DROP TRIGGER IF EXISTS check_parent_path_when_insert;\n");
        query.append("DROP TRIGGER IF EXISTS check_parent_path_when_update;\n");
        query.append("DROP TRIGGER IF EXISTS check_child_assets;\n");

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

        addTriggersBack(query);

        return query.toString();
    }

    private void addTriggersBack(final StringBuilder query) {
        query.append("CREATE TRIGGER check_parent_path_when_insert  BEFORE INSERT\n")
                .append("on identifier\n")
                .append("FOR EACH ROW\n")
                .append("BEGIN\n")
                .append("DECLARE idCount INT;\n")
                .append("DECLARE canInsert boolean default false;\n")
                .append(" select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n")
                .append(" IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n")
                .append("   SET canInsert := TRUE;\n")
                .append(" END IF;\n")
                .append(" IF(canInsert = FALSE) THEN\n")
                .append("  delete from Cannot_insert_for_this_path_does_not_exist_for_the_given_host;\n")
                .append(" END IF;\n")
                .append("END\n")
                .append("#\n");

        query.append("CREATE TRIGGER check_parent_path_when_update  BEFORE UPDATE\n")
                .append("on identifier\n")
                .append("FOR EACH ROW\n")
                .append("BEGIN\n")
                .append("DECLARE idCount INT;\n")
                .append("DECLARE canUpdate boolean default false;\n")
                .append(" IF @disable_trigger IS NULL THEN\n")
                .append("   select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n")
                .append("   IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n")
                .append("     SET canUpdate := TRUE;\n")
                .append("   END IF;\n")
                .append("   IF(canUpdate = FALSE) THEN\n")
                .append("     delete from Cannot_update_for_this_path_does_not_exist_for_the_given_host;\n")
                .append("   END IF;\n")
                .append(" END IF;\n")
                .append("END\n")
                .append("#\n");

        query.append("CREATE TRIGGER check_child_assets BEFORE DELETE\n")
                .append("ON identifier\n")
                .append("FOR EACH ROW\n")
                .append("BEGIN\n")
                .append("  DECLARE pathCount INT;\n")
                .append("    IF(OLD.asset_type ='folder') THEN\n")
                .append("      select count(*) into pathCount from identifier where parent_path = CONCAT(OLD.parent_path,OLD.asset_name,'/') and host_inode = OLD.host_inode;\n")
                .append("    END IF;\n")
                .append("    IF(OLD.asset_type ='contentlet') THEN\n")
                .append("\t select count(*) into pathCount from identifier where host_inode = OLD.id;\n")
                .append("    END IF;\n")
                .append(" IF(pathCount > 0) THEN\n")
                .append("   delete from Cannot_delete_as_this_path_has_children;\n")
                .append(" END IF;\n")
                .append("END\n")
                .append("#");
    }

    @Override
    public String getOracleScript() {

        final StringBuilder query = new StringBuilder();

        query.append("ALTER TABLE identifier DISABLE ALL TRIGGERS;\n");

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
                .append("               WHERE  myID = tt.id);\n");

        query.append("ALTER TABLE identifier ENABLE ALL TRIGGERS\n");

        return query.toString();
    }

    @Override
    public String getMSSQLScript() {
        return getPostgresScript();
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
