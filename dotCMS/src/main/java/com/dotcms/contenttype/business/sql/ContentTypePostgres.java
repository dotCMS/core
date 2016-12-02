package com.dotcms.contenttype.business.sql;

class ContentTypePostgres extends ContentTypeSql{

    public ContentTypePostgres(){
        SELECT_BY_VAR = SELECT_ALL_STRUCTURE_FIELDS + " and LOWER(structure.velocity_var_name) like ?";
    }

}
