package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * View as rendered page status
 */
@JsonSerialize(using = ViewAsPageStatusSerializer.class)
public class ViewAsPageStatus {
    private IPersona   persona;
    private Language   language;
    private Contentlet device;
    private PageMode   pageMode;
    private boolean    personalized;

    ViewAsPageStatus(){}

    ViewAsPageStatus setPersonalized(final boolean personalized) {
        this.personalized = personalized;
        return this;
    }

    ViewAsPageStatus setPersona(IPersona persona) {
        this.persona = persona;
        return this;
    }

    ViewAsPageStatus setLanguage(Language language) {
        this.language = language;
        return this;
    }

    ViewAsPageStatus setDevice(Contentlet device) {
        this.device = device;
        return this;
    }

    public IPersona getPersona() {
        return persona;
    }

    public Language getLanguage() {
        return language;
    }

    public Contentlet getDevice() {
        return device;
    }

    ViewAsPageStatus setPageMode(PageMode pageMode) {
        this.pageMode = pageMode;
        return this;
    }

    public PageMode getPageMode() {
        return pageMode;
    }

    public boolean isPersonalized() {
        return personalized;
    }
}
