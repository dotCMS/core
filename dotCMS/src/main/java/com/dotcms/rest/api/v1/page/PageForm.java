package com.dotcms.rest.api.v1.page;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents the layout of a Template in dotCMS.
 *
 * <p>All Containers that make up the structure of a Template -- along with its rows and columns --
 * are organized and transformed into an instance of this class.</p>
 *
 * @author Freddy Rodriguez
 * @since Nov 22nd, 2017
 */
@JsonDeserialize(builder = PageForm.Builder.class)
class PageForm {

    private final String themeId;
    private final String title;
    private final String siteId;
    private final TemplateLayout layout;


    public PageForm(final String themeId, final String title, final String siteId, final TemplateLayout layout) {

        this.themeId = themeId;
        this.title = title;
        this.siteId = siteId;
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
        return title != null ? title : Template.ANONYMOUS_PREFIX + System.currentTimeMillis();
    }

    /**
     *
     * @return Layout's site
     */
    public String getSiteId() {
        return siteId;
    }

    public boolean isAnonymousLayout() {
        return !UtilMethods.isSet(this.title);
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
        private String themeId;

        @JsonProperty
        private String title;

        @JsonProperty
        private String hostId;

        @JsonProperty(required = true)
        private TemplateLayout layout;

        @JsonIgnore
        private Map<String, ContainerUUIDChanged> changes;

        @JsonIgnore
        private Map<String, String> newlyContainersUUID;

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

        public Builder layout(TemplateLayout layout) {
            this.layout = layout;
            return this;
        }

        PageForm build(){
            return new PageForm(themeId, title, hostId, layout);
        }

    }

}
