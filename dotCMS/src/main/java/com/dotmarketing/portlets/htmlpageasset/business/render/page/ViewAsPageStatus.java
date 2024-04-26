package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.api.APIProvider;
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
    private String variantId;


    private ViewAsPageStatus(final Builder builder) {
      super();
      this.visitor = builder.visitor;
      this.persona= builder.visitor != null ? builder.visitor.getPersona():null;
      this.language = builder.language;
      this.device = builder.device;
      this.pageMode = builder.pageMode;
      this.personalized = builder.personalized;
      this.variantId = builder.variantId;
      
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

    public String getVariantId() {
        return variantId;
    }

    public static class Builder{
      private Language   language;
      private Contentlet device;
      private PageMode   pageMode;
      private  boolean    personalized;
      private Visitor   visitor;
      private String variantId;

      public Builder setLanguage(Language language) {
          this.language = language;
          return this;
      }

      public Builder setDevice(Contentlet device) {
          this.device = device;
          return this;
      }

      public Builder setPageMode(PageMode pageMode) {
          this.pageMode = pageMode;
          return this;
      }

      public Builder setPersonalized(boolean personalized) {
          this.personalized = personalized;
          return this;
      }

      public Builder setVariant(final String variantId) {
          this.variantId = variantId;
          return this;
      }
      public Builder setVisitor(Visitor visitor) {
          this.visitor = visitor;
          return this;
      }

      public ViewAsPageStatus build(){
          return new ViewAsPageStatus(this);
      }


  }
}
