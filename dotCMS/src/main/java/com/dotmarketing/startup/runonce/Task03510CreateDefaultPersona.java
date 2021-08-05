package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to add the default persona content type
 *
 * @author
 * @version 1.0
 * @since 12-04-2015
 */
public class Task03510CreateDefaultPersona extends AbstractJDBCStartupTask {


    private final String INSERT_FIELD = "INSERT INTO field " +
        "(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet," +
        "required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check," +
        "hint,default_value,fixed,read_only,searchable,unique_,mod_date) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private final String INSERT_DEFAULT_STRUC_QUERY = "insert into structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, folder, mod_date, default_structure) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? ,?,?)";
    private final String INSERT_DEFAULT_STRUC_INODE_QUERY = "insert into inode (inode, owner, idate, type) values (?, ?, ?, ?)";

    // make sure we have inode and structure entries
    private final String SELECT_DEFAULT_STRUC_QUERY = "select inode.inode from inode,structure where inode.inode = ? and structure.inode = inode.inode";

    // these need to be fixed and identical in all installations
    private final String[] DEFAUTL_PERSONA_FIELD_INODES = { "606ac3af-63e5-4bd4-bfa1-c4c672bb8eb8", "0ea2bd92-4b2d-48a2-a394-77fd560b1fce",
        "6b25d960-034d-4030-b785-89cc01baaa3d", "07cfbc2c-47de-4c78-a411-176fe8bb24a5", "2dab7223-ebb5-411b-922f-611a30bc2a2b",
        "65e4e742-d87a-47ff-84ef-fde44e889e27", "f9fdd242-6fac-4d03-9fa3-b346d6995779" };

    private final String DEFAULT_PERSONAS_STRUCTURE_INODE = "c938b15f-bcb6-49ef-8651-14d455a97045";
    private final String DEFAULT_PERSONAS_STRUCTURE_VARNAME = "persona";

    private final String DEFAULT_PERSONAS_STRUCTURE_NAME = "Persona";
    private final String DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION = "Default Structure for Personas";


    @Override
    public boolean forceRun() {
        return true;
    }


    /**
     * ALL of this logic and SQL is duped in the PersonaFactoryImpl
     * and could be called by:
     * APILocator.getPersonaAPI().createDefaultPersonaStructure();
     * But I did not want to "CALL an API in a startup task"
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        DotConnect dc = new DotConnect();

        dc.setSQL(SELECT_DEFAULT_STRUC_QUERY);
        dc.addParam(DEFAULT_PERSONAS_STRUCTURE_INODE);
        if (dc.loadResults().size() > 0) {
            return;
        }

        /**
         * insert inode
         */
        dc.setSQL(INSERT_DEFAULT_STRUC_INODE_QUERY);
        dc.addParam(DEFAULT_PERSONAS_STRUCTURE_INODE);
        dc.addParam("system");
        dc.addParam(new Date());
        dc.addParam("structure");
        dc.loadResult();

        /**
         * insert structure inode
         */
        dc = new DotConnect();
        dc.setSQL(INSERT_DEFAULT_STRUC_QUERY);
        dc.addParam(DEFAULT_PERSONAS_STRUCTURE_INODE);
        dc.addParam(DEFAULT_PERSONAS_STRUCTURE_NAME);
        dc.addParam(DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION);
        dc.addParam(6);
        dc.addParam(false);
        dc.addParam(false);
        dc.addParam(DEFAULT_PERSONAS_STRUCTURE_VARNAME);
        dc.addParam("SYSTEM_HOST");
        dc.addParam("SYSTEM_FOLDER");
        dc.addParam(new Date());
        dc.addParam(false);
        dc.loadResult();

        List<Map<String, Object>> fields = getBasePersonaFields();
        int i = 0;
        String currentInode;
        for (Map<String,Object> f : fields) {
            currentInode = DEFAUTL_PERSONA_FIELD_INODES[i++];
            //Insert inode
            dc.setSQL(INSERT_DEFAULT_STRUC_INODE_QUERY);
            dc.addParam(currentInode);
            dc.addParam("");
            dc.addParam(new Date());
            dc.addParam("field");
            dc.loadResult();


            //Insert field
            insertField(dc, currentInode, f);
        }
    }

    private void insertField(DotConnect dc, String currentInode, Map<String, Object> f) throws DotDataException {
        dc.setSQL(INSERT_FIELD);
        dc.addParam(currentInode);
        dc.addParam(f.get("structureInode"));
        dc.addParam(f.get("fieldName"));
        dc.addParam(f.get("fieldType"));
        dc.addParam(f.get("fieldRelationType"));
        dc.addParam(f.get("fieldContentlet"));
        dc.addParam(f.get("isRequired"));
        dc.addParam(f.get("isIndexed"));
        dc.addParam(f.get("isListed"));
        dc.addParam(f.get("velocityVarName"));
        dc.addParam(f.get("sortOrder"));
        dc.addParam(f.get("values"));
        dc.addParam(f.get("regexCheck"));
        dc.addParam(f.get("hint"));
        dc.addParam(f.get("defaultValue"));
        dc.addParam(f.get("isFixed"));
        dc.addParam(f.get("isReadOnly"));
        dc.addParam(f.get("isSearchable"));
        dc.addParam(f.get("isUnique"));
        dc.addParam(new Date());
        dc.loadResult();
    }

    private List<Map<String, Object>> getBasePersonaFields() {
        List fields = new ArrayList();

        fields.add(getFieldMap("Site/Folder", "host or folder", "null:sitefolder", "system_field1", true, false, true,
            "hostFolder", 1, "", "", "", true, false, true));

        fields.add(getFieldMap("Name", "text", "null:name", "text1", true, true, true,
            "name", 2, "", "", "", true, false, true));

        fields.add(getFieldMap("Key Tag", "custom_field", "null:keyTag", "text2", true, true, true,
            "keyTag", 3, "$velutil.mergeTemplate(\'/static/personas/keytag_custom_field.vtl\')", "", "[a-zA-Z0-9]+", true, false, true));

        fields.add(getFieldMap("Photo", "binary", "null:photo", "binary1", false, false, false,
            "photo", 4, "", "", "", true, false, false));

        fields.add(getFieldMap("Other Tags", "tag", "null:otherTags", "text_area1", false, false, true,
            "tags", 5, "", "", "", true, false, true));

        fields.add(getFieldMap("Description", "textarea", "null:description", "text_area2", false, false, true,
            "description", 6, "", "", "", true, false, true));

        return fields;
    }

    private Map<String, Object> getFieldMap(String fieldName, String fieldType, String fieldRelationType,
                                            String fieldContentlet, boolean required, boolean listed, boolean indexed,
                                            String velocityVarName, int sortOrder, String values, String defaultValue,
                                            String checkRegex, boolean fixed, boolean readOnly, boolean searchable) {

        Map<String, Object> elements = new HashMap<>();
        elements.put("structureInode", DEFAULT_PERSONAS_STRUCTURE_INODE);
        elements.put("fieldName", fieldName);
        elements.put("fieldType", fieldType);
        elements.put("fieldRelationType", fieldRelationType);
        elements.put("fieldContentlet",fieldContentlet);
        elements.put("isRequired", required);
        elements.put("isIndexed", indexed);
        elements.put("isListed", listed);
        elements.put("velocityVarName", velocityVarName);
        elements.put("sortOrder", sortOrder);
        elements.put("values", values);
        elements.put("regexCheck", checkRegex);
        elements.put("hint", "");
        elements.put("defaultValue", defaultValue);
        elements.put("isFixed", fixed);
        elements.put("isReadOnly", readOnly);
        elements.put("isSearchable", searchable);
        elements.put("isUnique", false);
        return elements;
    }




    @Override
    public String getPostgresScript() {
        // TODO Auto-generated method stub
        return null;
    }




    @Override
    public String getMySQLScript() {
        // TODO Auto-generated method stub
        return null;
    }




    @Override
    public String getOracleScript() {
        // TODO Auto-generated method stub
        return null;
    }




    @Override
    public String getMSSQLScript() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    protected List<String> getTablesToDropConstraints() {
        // TODO Auto-generated method stub
        return null;
    }




}