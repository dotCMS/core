package com.dotcms.rest.api.v1.portlet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotmarketing.util.UUIDGenerator;
import javax.annotation.Nonnull;

@JsonDeserialize(builder = CustomPortletForm.Builder.class)
public class CustomPortletForm {

  final public String portletId,portletName, baseTypes, contentTypes, dataViewMode;

  
  private CustomPortletForm(Builder builder) {
    this.portletId = (builder.portletId==null) ? UUIDGenerator.shorty() : builder.portletId;
    this.portletName = builder.portletName;
    this.baseTypes = builder.baseTypes;
    this.contentTypes = builder.contentTypes;
    this.dataViewMode = builder.dataViewMode;
  }

  /**
   * Creates builder to build {@link CustomPortletForm}.
   * @return created builder
   */
  
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder to build {@link CustomPortletForm} and initialize it with the given object.
   * @param customPortletForm to initialize the builder with
   * @return created builder
   */
  
  public static Builder from(CustomPortletForm customPortletForm) {
    return new Builder(customPortletForm);
  }

  /**
   * Builder to build {@link CustomPortletForm}.
   */
  
  public static final class Builder {
    @JsonProperty
    private String portletId;
    @JsonProperty
    private String portletName;
    @JsonProperty
    private String baseTypes;
    @JsonProperty
    private String contentTypes;
    @JsonProperty
    private String dataViewMode;

    private Builder() {}

    private Builder(CustomPortletForm customPortletForm) {
      this.portletId = customPortletForm.portletId;
      this.portletName = customPortletForm.portletName;
      this.baseTypes = customPortletForm.baseTypes;
      this.contentTypes = customPortletForm.contentTypes;
      this.dataViewMode = customPortletForm.dataViewMode;
    }

    public Builder withPortletId(@Nonnull String portletId) {
      this.portletId = portletId;
      return this;
    }

    public Builder withPortletName(@Nonnull String portletName) {
      this.portletName = portletName;
      return this;
    }

    public Builder withBaseTypes(@Nonnull String baseTypes) {
      this.baseTypes = baseTypes;
      return this;
    }

    public Builder withContentTypes(@Nonnull String contentTypes) {
      this.contentTypes = contentTypes;
      return this;
    }

      public Builder withDataViewMode(@Nonnull String viewMode) {
          this.dataViewMode = viewMode;
          return this;
      }

    public CustomPortletForm build() {
      return new CustomPortletForm(this);
    }
  }
  
}
