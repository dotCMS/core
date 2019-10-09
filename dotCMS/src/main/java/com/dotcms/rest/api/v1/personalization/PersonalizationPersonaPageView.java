package com.dotcms.rest.api.v1.personalization;

import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageViewSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the personalization for personas per page, if the persona
 * @author jsanca
 */
@JsonSerialize(using = PersonalizationPersonaPageViewSerializer.class)
public class PersonalizationPersonaPageView implements Serializable {

    private final String pageId;
    private final Map<String, Object> persona;

    public PersonalizationPersonaPageView(final String pageId,
                                          final Map<String, Object> persona) {
        this.pageId       = pageId;
        this.persona      = persona;
    }

    public String getPageId() {
        return pageId;
    }

    public Map<String, Object> getPersona() {
        return persona;
    }

    @Override
    public String toString() {
        return "PersonalizationPersonaPageView{" +
                "pageId='" + pageId + '\'' +
                ", persona=" + persona +
                '}';
    }
}
