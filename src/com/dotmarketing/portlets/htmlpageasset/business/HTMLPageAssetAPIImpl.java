package com.dotmarketing.portlets.htmlpageasset.business;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;

public class HTMLPageAssetAPIImpl implements HTMLPageAssetAPI {

    @Override
    public void createHTMLPageAssetBaseFields(Structure structure) throws DotDataException, DotStateException {
        if (structure == null || !InodeUtils.isSet(structure.getInode())) {
            throw new DotStateException("Cannot create base htmlpage asset fields on a structure that doesn't exist");
        }
        if (structure.getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE) {
            throw new DotStateException("Cannot create base htmlpage asset fields on a structure that is not of htmlpage asset type");
        }
        Field field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, 1, "", "", "", true, false, true);

        field.setVelocityVarName(HOST_FOLDER_FIELD);
        FieldFactory.saveField(field);

        field = new Field(TITLE_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 2, "", "", "", true, false, true);
        field.setVelocityVarName(TITLE_FIELD);
        field.setListed(false);
        FieldFactory.saveField(field);
        
        field = new Field(URL_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 3, "", "", "", true, false, true);
        field.setVelocityVarName(URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(FRIENDLY_NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, false, true, 4, "", "", "", true, false, true);
        field.setVelocityVarName(FRIENDLY_NAME_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SORT_ORDER_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, true, false, true, 5, "", "0", "", true, false, true);
        field.setVelocityVarName(SORT_ORDER_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SHOW_ON_MENU_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 6, "|true", "false", "", true, false, false);
        field.setVelocityVarName(SHOW_ON_MENU_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(REDIRECT_URL_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, false, true, 7, "", "", "", true, false, true);
        field.setVelocityVarName(REDIRECT_URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(HTTPS_REQUIRED_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 8, "|true", "false", "", true, false, false);
        field.setVelocityVarName(HTTPS_REQUIRED_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(CACHE_TTL_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, false, false, true, 9, "", "0", "", true, false, true);
        field.setVelocityVarName(CACHE_TTL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SEO_DESCRIPTION_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 10, "", "", "", true, false, true);
        field.setVelocityVarName(SEO_DESCRIPTION_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SEO_KEYWORDS_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 11, "", "", "", true, false, true);
        field.setVelocityVarName(SEO_KEYWORDS_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(PAGE_METADATA_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 12, "", "", "", true, false, true);
        field.setVelocityVarName(PAGE_METADATA_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(TEMPLATE_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, false, true, 13, "", "", "", true, false, true);
        field.setVelocityVarName(TEMPLATE_FIELD);
        FieldFactory.saveField(field);
                
    }

}
