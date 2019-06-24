package com.dotcms.rest.api.v1.personalization;

import com.dotmarketing.portlets.personas.model.Persona;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the personalization for personas per page, if the persona
 * @author jsanca
 */
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
}
