package com.dotcms.contenttype.business.sql;

class ContentTypeOracle extends ContentTypeSql{

    public ContentTypeOracle(){
        SELECT_BY_VAR = SELECT_ALL_STRUCTURE_FIELDS + " and LOWER(structure.velocity_var_name) like ?";
    }
}
