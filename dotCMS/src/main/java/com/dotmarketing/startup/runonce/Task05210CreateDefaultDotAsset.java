package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
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
 * This upgrade task create the default DotAsset existing dotCMS installs.
 * The tables affected by this task are:
 * <ul>
 * <li>{@code field}: Contains the 3 Fields for the DotAsset.</li>
 * <li>{@code structure}: Contains the new DotAsset Content Type.</li>
 * </ul>
 * 
 * @author jsanca
 */
public class Task05210CreateDefaultDotAsset implements StartupTask {

    public  static final String DOTASSET_VARIABLE_INODE       = "f2d8a1c7-2b77-2081-bcf1-b5348988c08d";
    private static final String DOTASSET_VARIABLE_VARNAME     = "DotAsset";
    private static final String DOTASSET_VARIABLE_NAME        = "DotAsset";
    private static final String DOTASSET_VARIABLE_DESCRIPTION = "Default Content Type for DotAsset";
    private static final String INSERT_INODE_QUERY = "INSERT INTO inode (inode, owner, idate, type) VALUES (?, ?, ?, ?)";
    private static final String INSERT_FIELD_QUERY = "INSERT INTO field "
                    + "(inode, structure_inode, field_name, field_type, field_relation_type, field_contentlet, "
                    + "required, indexed, listed, velocity_var_name, sort_order, field_values, regex_check, "
                    + "hint, default_value, fixed, read_only, searchable, unique_, mod_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_STRUCTURE_QUERY =
                    "INSERT INTO structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, "
                                    + "folder, mod_date, default_structure) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String[] DOTASSET_VARIABLE_FIELD_INODES = {
            "06d5ecb5-5334-3cb7-a883-f029d12e6a0b", // Asset
            "d8719d24-df37-3a20-9442-75fb423cef7e", // hostFolder
            "d8720d13-df48-4b29-9331-75fa314def7e" // tags
    };

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        // Inserts into Inode table the reference to the Language Variable Content Type
        this.addInodeContentType();

        // Inserts into Structure table the Language Variable Content Type
        this.addDotAssetContentType();
        // Gets the list of required fields for the Language Variable Content Type
        this.addDotAssetFields();
    }

    private void addDotAssetFields() throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final List<Map<String, Object>> requiredFields = getDotAssetVariableFields();
        int counter = 0;
        for (final Map<String, Object> field : requiredFields) {
            // Insert into Inode table the reference to the field
            dotConnect.setSQL(INSERT_INODE_QUERY);
            dotConnect.addParam(DOTASSET_VARIABLE_FIELD_INODES[counter]);
            dotConnect.addParam(StringPool.BLANK);
            dotConnect.addParam(new Date());
            dotConnect.addParam("field");
            dotConnect.loadResult();
            // Insert into Field table
            dotConnect.setSQL(INSERT_FIELD_QUERY);
            dotConnect.addParam(DOTASSET_VARIABLE_FIELD_INODES[counter]);
            dotConnect.addParam(field.get("structureInode"));
            dotConnect.addParam(field.get("fieldName"));
            dotConnect.addParam(field.get("fieldType"));
            dotConnect.addParam(field.get("fieldRelationType"));
            dotConnect.addParam(field.get("fieldContentlet"));
            dotConnect.addParam(field.get("isRequired"));
            dotConnect.addParam(field.get("isIndexed"));
            dotConnect.addParam(field.get("isListed"));
            dotConnect.addParam(field.get("velocityVarName"));
            dotConnect.addParam(field.get("sortOrder"));
            dotConnect.addParam(field.get("values"));
            dotConnect.addParam(field.get("regexCheck"));
            dotConnect.addParam(field.get("hint"));
            dotConnect.addParam(field.get("defaultValue"));
            dotConnect.addParam(field.get("isFixed"));
            dotConnect.addParam(field.get("isReadOnly"));
            dotConnect.addParam(field.get("isSearchable"));
            dotConnect.addParam(field.get("isUnique"));
            dotConnect.addParam(new Date());
            dotConnect.loadResult();
            counter++;
        }
    }

    private void addDotAssetContentType() throws DotDataException {
        final boolean isSystem = Boolean.FALSE;
        final boolean isFixed  = Boolean.FALSE;
        final boolean isDefaultStructure = Boolean.FALSE;
        new DotConnect().setSQL(INSERT_STRUCTURE_QUERY)
            .addParam(DOTASSET_VARIABLE_INODE)
            .addParam(DOTASSET_VARIABLE_NAME)
            .addParam(DOTASSET_VARIABLE_DESCRIPTION)
            .addParam(BaseContentType.DOTASSET.getType())
            .addParam(isSystem).addParam(isFixed)
            .addParam(DOTASSET_VARIABLE_VARNAME)
            .addParam("SYSTEM_HOST").addParam("SYSTEM_FOLDER")
            .addParam(new Date()).addParam(isDefaultStructure)
            .loadResult();
    }

    private void addInodeContentType() throws DotDataException {
        new DotConnect().setSQL(INSERT_INODE_QUERY)
                .addParam(DOTASSET_VARIABLE_INODE).addParam("system")
                .addParam(new Date()).addParam("structure")
                .loadResult();
    }

    /**
     * Creates each field that is required by the Language Variable Content Type and returns a list
     * with them.
     * 
     * @return The List of DotAsset Variable fields.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDotAssetVariableFields() {

        final boolean isRequired   = Boolean.TRUE;
        final boolean noRequired   = Boolean.FALSE;
        final boolean listed       = Boolean.TRUE;
        final boolean noListed     = Boolean.FALSE;
        final boolean isIndexed    = Boolean.TRUE;
        final boolean isFixed      = Boolean.TRUE;
        final boolean noFixed      = Boolean.FALSE;
        final boolean noReadOnly   = Boolean.FALSE;
        final boolean noUnique     = Boolean.FALSE;
        final boolean isSearchable = Boolean.TRUE;

        // hostFolder
        final Map<String, Object> hostFolderField = setFieldElements("Site or Folder",
                HostFolderField.class.getName(), StringPool.BLANK, "system_field", noRequired, noListed, isIndexed,
                DotAssetContentType.SITE_OR_FOLDER_FIELD_VAR, 0, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
                StringPool.BLANK, noFixed, noReadOnly, isSearchable, noUnique);

        // Asset
        final Map<String, Object> assetField = setFieldElements("Asset",
                BinaryField.class.getName(), StringPool.BLANK, "system_field", isRequired, listed, isIndexed,
                DotAssetContentType.ASSET_FIELD_VAR, 1, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
                StringPool.BLANK, isFixed, noReadOnly, isSearchable, noUnique);

        // tags
        final Map<String, Object> tagsField = setFieldElements("Tags",
                TagField.class.getName(), StringPool.BLANK, "system_field", noRequired, noListed, isIndexed,
                DotAssetContentType.TAGS_FIELD_VAR, 2, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
                StringPool.BLANK, noFixed, noReadOnly, isSearchable, noUnique);

        return list(assetField, hostFolderField, tagsField);
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
    private Map<String, Object> setFieldElements(final String fieldName, final String fieldType, final String fieldRelationType,
                                                 final String fieldContentlet, final boolean required, final boolean listed,
                                                 final boolean indexed, final String velocityVarName,
                                                 final int sortOrder, final String values, final String checkRegex, final String hint,
                                                 final String defaultValue, final boolean fixed, final boolean readOnly, final boolean searchable, final boolean unique) {

        final Map<String, Object> fieldElementsMap = new HashMap<>();

        fieldElementsMap.put("structureInode", DOTASSET_VARIABLE_INODE);
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
