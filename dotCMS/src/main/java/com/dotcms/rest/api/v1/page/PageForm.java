package com.dotcms.rest.api.v1.page;

import java.io.IOException;
import java.util.Map;

import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * {@link PageResource}'s form
 */
@JsonDeserialize(builder = PageForm.Builder.class)
public class PageForm {

    private String themeId;
    private String title;
    private String hostId;
    private TemplateLayout layout;

    public PageForm(String themeId, String title, String hostId, TemplateLayout layout) {
        this.themeId = themeId;
        this.title = title;
        this.hostId = hostId;
        this.layout = layout;
    }

    /**
     *
     * @return Layout's theme id
     */
    public String getThemeId() {
        return themeId;
    }

    /**
     *
     * @return Layout's title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @return Layout's host
     */
    public String getHostId() {
        return hostId;
    }

    /**
     *
     * @return TemplateLayout linked with the Template
     */
    public TemplateLayout getLayout() {
        return layout;
    }

    public static final class Builder {
        @JsonProperty
        @NotNull
        private String themeId;

        @JsonProperty
        private String title;

        @JsonProperty
        @NotNull
        private String hostId;

        @JsonProperty
        @NotNull
        private Map<String, Object> layout;

        public Builder() {
        }

        public Builder themeId(String themeId) {
            this.themeId = themeId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder hostId(String hostId) {
            this.hostId = hostId;
            return this;
        }

        public Builder layout(Map<String, Object> layout) {
            this.layout = layout;
            return this;
        }

        private TemplateLayout getTemplateLayout() throws BadRequestException {

            try {
                String layoutString = JsonTransformer.mapper.writeValueAsString(layout);
                return JsonTransformer.mapper.readValue(layoutString, TemplateLayout.class);
            } catch (IOException e) {
                throw new BadRequestException(e, "An error occurred when proccessing the JSON request");
            }
        }

        public PageForm build(){
            return new PageForm(themeId, title, hostId, getTemplateLayout());
        }
    }
}
