package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(builder = DeleteFieldsForm.Builder.class)
class DeleteFieldsForm {
    private List<String> fieldsID;

    public DeleteFieldsForm(final List<String> fieldsID) {
        this.fieldsID = fieldsID;
    }

    public List<String> getFieldsID() {
        return fieldsID;
    }

    public static final class Builder {
        private List<String> fieldsID;

        Builder(@JsonProperty List<String> fieldsID) {
            this.fieldsID = fieldsID;
        }

        public DeleteFieldsForm build(){
            return new DeleteFieldsForm(fieldsID);
        }
    }
}
