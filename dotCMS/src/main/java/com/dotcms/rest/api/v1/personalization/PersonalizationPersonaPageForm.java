package com.dotcms.rest.api.v1.personalization;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = PersonalizationPersonaPageForm.Builder.class)
public class PersonalizationPersonaPageForm extends Validated {

    @NotNull
    private final String        pageId;

    @NotNull
    private final String        personaTag;


    public String getPageId() {
        return pageId;
    }

    public String getPersonaTag() {
        return personaTag;
    }

    public PersonalizationPersonaPageForm(final PersonalizationPersonaPageForm.Builder builder) {
        super();
        this.pageId                     = builder.pageId;
        this.personaTag                 = builder.personaTag;
        this.checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String        pageId;

        @JsonProperty(required = true)
        private String        personaTag;


        public PersonalizationPersonaPageForm.Builder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public PersonalizationPersonaPageForm.Builder personaTag(String personaTag) {
            this.personaTag = personaTag;
            return this;
        }

        public PersonalizationPersonaPageForm build() {
            return new PersonalizationPersonaPageForm(this);
        }
    }
}
