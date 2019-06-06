package com.dotcms.rest.api.v1.personalization;

import com.dotmarketing.portlets.personas.model.Persona;

import java.io.Serializable;

/**
 * Encapsulates the personalization for personas per page, if the persona
 * @author jsanca
 */
public class PersonalizationPersonaPageView implements Serializable {

    private final String pageId;
    private final boolean personalized;
    private final Persona persona;

    public PersonalizationPersonaPageView(final String pageId, final boolean personalized,
                                          final Persona persona) {
        this.pageId       = pageId;
        this.personalized = personalized;
        this.persona      = persona;
    }


    public String getPageId() {
        return pageId;
    }

    public boolean isPersonalized() {
        return personalized;
    }

    public Persona getPersona() {
        return persona;
    }
}
