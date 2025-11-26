package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.liferay.util.StringPool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * This upgrade task create the default Language Variable Content Type for existing dotCMS installs.
 * The tables affected by this task are:
 * <ul>
 * <li>{@code inode}: Contains the Inodes for the Language Variable and its associated Fields.</li>
 * <li>{@code field}: Contains the 2 Fields for the Language Variable.</li>
 * <li>{@code structure}: Contains the new Language Variable Content Type.</li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 30, 2017
 *
 */
public class Task04210CreateDefaultLanguageVariable implements StartupTask {

    private static final String LANGUAGE_VARIABLE_INODE = "f4d7c1b8-2c88-4071-abf1-a5328977b07d";
    private static final String LANGUAGE_VARIABLE_VARNAME = "Languagevariable";
    private static final String LANGUAGE_VARIABLE_NAME = "Language Variable";
    private static final String LANGUAGE_VARIABLE_DESCRIPTION = "Default Content Type for Language Variables";
    private static final String INSERT_INODE_QUERY = "INSERT INTO inode (inode, owner, idate, type) VALUES (?, ?, ?, ?)";
    private static final String INSERT_FIELD_QUERY = "INSERT INTO field "
                    + "(inode, structure_inode, field_name, field_type, field_relation_type, field_contentlet, "
                    + "required, indexed, listed, velocity_var_name, sort_order, field_values, regex_check, "
                    + "hint, default_value, fixed, read_only, searchable, unique_, mod_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_STRUCTURE_QUERY =
                    "INSERT INTO structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, "
                                    + "folder, mod_date, default_structure) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String[] LANGUAGE_VARIABLE_FIELD_INODES = {"05b6edc6-6443-4dc7-a884-f029b12e5a0d", // Key
            "c7829c13-cf47-4a20-9331-85fb314cef8e" // Value
    };

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        DotConnect dc = new DotConnect();
        // Inserts into Inode table the reference to the Language Variable Content Type
        dc.setSQL(INSERT_INODE_QUERY);
        dc.addParam(LANGUAGE_VARIABLE_INODE);
        dc.addParam("system");
        dc.addParam(new Date());
        dc.addParam("structure");
        dc.loadResult();
        // Inserts into Structure table the Language Variable Content Type
        final boolean isSystem = Boolean.FALSE;
        final boolean isFixed = Boolean.TRUE;
        final boolean isDefaultStructure = Boolean.FALSE;
        dc = new DotConnect();
        dc.setSQL(INSERT_STRUCTURE_QUERY);
        dc.addParam(LANGUAGE_VARIABLE_INODE);
        dc.addParam(LANGUAGE_VARIABLE_NAME);
        dc.addParam(LANGUAGE_VARIABLE_DESCRIPTION);
        dc.addParam(BaseContentType.KEY_VALUE.getType());
        dc.addParam(isSystem);
        dc.addParam(isFixed);
        dc.addParam(LANGUAGE_VARIABLE_VARNAME);
        dc.addParam("SYSTEM_HOST");
        dc.addParam("SYSTEM_FOLDER");
        dc.addParam(new Date());
        dc.addParam(isDefaultStructure);
        dc.loadResult();
        // Gets the list of required fields for the Language Variable Content Type
        List<Map<String, Object>> requiredFields = getLanguageVariableFields();
        int i = 0;
        for (Map<String, Object> field : requiredFields) {
            // Insert into Inode table the reference to the field
            dc.setSQL(INSERT_INODE_QUERY);
            dc.addParam(LANGUAGE_VARIABLE_FIELD_INODES[i]);
            dc.addParam(StringPool.BLANK);
            dc.addParam(new Date());
            dc.addParam("field");
            dc.loadResult();
            // Insert into Field table
            dc.setSQL(INSERT_FIELD_QUERY);
            dc.addParam(LANGUAGE_VARIABLE_FIELD_INODES[i]);
            dc.addParam(field.get("structureInode"));
            dc.addParam(field.get("fieldName"));
            dc.addParam(field.get("fieldType"));
            dc.addParam(field.get("fieldRelationType"));
            dc.addParam(field.get("fieldContentlet"));
            dc.addParam(field.get("isRequired"));
            dc.addParam(field.get("isIndexed"));
            dc.addParam(field.get("isListed"));
            dc.addParam(field.get("velocityVarName"));
            dc.addParam(field.get("sortOrder"));
            dc.addParam(field.get("values"));
            dc.addParam(field.get("regexCheck"));
            dc.addParam(field.get("hint"));
            dc.addParam(field.get("defaultValue"));
            dc.addParam(field.get("isFixed"));
            dc.addParam(field.get("isReadOnly"));
            dc.addParam(field.get("isSearchable"));
            dc.addParam(field.get("isUnique"));
            dc.addParam(new Date());
            dc.loadResult();
            i++;
        }
    }

    /**
     * Creates each field that is required by the Language Variable Content Type and returns a list
     * with them.
     * 
     * @return The List of Language Variable fields.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getLanguageVariableFields() {
        final boolean isRequired = Boolean.TRUE;
        final boolean isListed = Boolean.TRUE;
        final boolean isIndexed = Boolean.TRUE;
        final boolean isFixed = Boolean.TRUE;
        final boolean isReadOnly = Boolean.TRUE;
        final boolean isSearchable = Boolean.TRUE;
        final boolean isUnique = Boolean.TRUE;
        Map<String, Object> keyField = setFieldElements(KeyValueContentType.KEY_VALUE_KEY_FIELD_NAME,
                        LegacyFieldTypes.TEXT.implClass().getName(), StringPool.BLANK, "text1", isRequired, isListed, isIndexed,
                        KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, 1, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
                        StringPool.BLANK, isFixed, !isReadOnly, isSearchable, isUnique);
        Map<String, Object> valuField = setFieldElements(KeyValueContentType.KEY_VALUE_VALUE_FIELD_NAME,
                        LegacyFieldTypes.TEXT_AREA.implClass().getName(), StringPool.BLANK, "text_area1", isRequired, isListed,
                        !isIndexed, KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, 2, StringPool.BLANK, StringPool.BLANK,
                        StringPool.BLANK, StringPool.BLANK, isFixed, !isReadOnly, isSearchable, !isUnique);
        return list(keyField, valuField);
    }

    /**
     * This method sets the value of each element that is needed when creating a Field.
     * 
     * @param fieldName
     * @param fieldType
     * @param fieldRelationType
     * @param fieldContentlet
     * @param required
     * @param listed
     * @param indexed
     * @param velocityVarName
     * @param sortOrder
     * @param values
     * @param checkRegex
     * @param hint
     * @param defaultValue
     * @param fixed
     * @param readOnly
     * @param searchable
     * @param unique
     * @return The data required by a field.
     */
    private Map<String, Object> setFieldElements(String fieldName, String fieldType, String fieldRelationType,
                    String fieldContentlet, boolean required, boolean listed, boolean indexed, String velocityVarName,
                    int sortOrder, String values, String checkRegex, String hint, String defaultValue, boolean fixed,
                    boolean readOnly, boolean searchable, boolean unique) {

        final Map<String, Object> fieldElementsMap = new HashMap<>();
        fieldElementsMap.put("structureInode", LANGUAGE_VARIABLE_INODE);
        fieldElementsMap.put("fieldName", fieldName);
        fieldElementsMap.put("fieldType", fieldType);
        fieldElementsMap.put("fieldRelationType", fieldRelationType);
        fieldElementsMap.put("fieldContentlet", fieldContentlet);
        fieldElementsMap.put("isRequired", required);
        fieldElementsMap.put("isIndexed", indexed);
        fieldElementsMap.put("isListed", listed);
        fieldElementsMap.put("velocityVarName", velocityVarName);
        fieldElementsMap.put("sortOrder", sortOrder);
        fieldElementsMap.put("values", values);
        fieldElementsMap.put("regexCheck", checkRegex);
        fieldElementsMap.put("hint", hint);
        fieldElementsMap.put("defaultValue", defaultValue);
        fieldElementsMap.put("isFixed", fixed);
        fieldElementsMap.put("isReadOnly", readOnly);
        fieldElementsMap.put("isSearchable", searchable);
        fieldElementsMap.put("isUnique", unique);

        return fieldElementsMap;
    }

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

}
