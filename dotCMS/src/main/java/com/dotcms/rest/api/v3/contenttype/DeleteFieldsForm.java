package com.dotcms.rest.api.v3.contenttype;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Form to {@link FieldResource#deleteFields(String, DeleteFieldsForm, HttpServletRequest)} (List, User)}
 */
@JsonDeserialize(builder = DeleteFieldsForm.Builder.class)
class DeleteFieldsForm {
    private final List<String> fieldsID;

    public DeleteFieldsForm(final List<String> fieldsID) {
        this.fieldsID = fieldsID;
    }

    public List<String> getFieldsID() {
        return fieldsID;
    }

    public static final class Builder {
        @JsonProperty
        private List<String> fieldsID;

        Builder(){}

        public Builder fieldsID(final List<String> fieldsID) {
            this.fieldsID = fieldsID;
            return this;
        }

        public DeleteFieldsForm build(){
            return new DeleteFieldsForm(fieldsID);
        }
    }
}
