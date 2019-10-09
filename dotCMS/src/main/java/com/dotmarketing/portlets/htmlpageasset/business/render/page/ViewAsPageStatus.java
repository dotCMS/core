package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.visitor.domain.Visitor;
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
    private final IPersona   persona;
    private final Language   language;
    private final Contentlet device;
    private final PageMode   pageMode;
    private final boolean    personalized;
    private final Visitor   visitor;



    public ViewAsPageStatus(
        final Visitor   visitor,
        Language language, 
        Contentlet device, 
        PageMode pageMode, 
        boolean personalized
        ) {
      super();
      this.visitor=visitor;
      this.persona=visitor!=null ? visitor.getPersona():null;
      this.language = language;
      this.device = device;
      this.pageMode = pageMode;
      this.personalized = personalized;
      
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


    public PageMode getPageMode() {
        return pageMode;
    }

    public boolean isPersonalized() {
        return personalized;
    }
    
    public Visitor getVisitor() {
      return visitor;
  }
}
