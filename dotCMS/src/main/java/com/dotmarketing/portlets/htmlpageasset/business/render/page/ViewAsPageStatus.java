package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

/**
 * View as rendered page status
 */
@JsonSerialize(using = ViewAsPageStatusSerializer.class)
public class ViewAsPageStatus {
    private IPersona persona;
    private Language language;
    private Contentlet device;
    private PageMode pageMode;

    ViewAsPageStatus(){}

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
}
