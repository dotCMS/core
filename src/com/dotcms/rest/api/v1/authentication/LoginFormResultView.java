package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.rest.api.LanguageView;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the Login Form Result View for the login page
 * @author jsanca
 */
public class LoginFormResultView implements Serializable {

    private final String serverId;
    private final String levelName;
    private final String version;
    private final String buildDateString;
    private final List<LanguageView> languages;
    private final String color; // todo check this with Jhon
    private final String backgroundColor;
    private final String backgroundPicture;
    private final String logo;

    private LoginFormResultView(LoginFormResultView.Builder builder) {
        serverId = builder.serverId;
        levelName = builder.levelName;
        version = builder.version;
        buildDateString = builder.buildDateString;
        languages  = builder.languages;
        color  = builder.color;
        backgroundColor  = builder.backgroundColor;
        backgroundPicture  = builder.backgroundPicture;
        logo  = builder.logo;
    }

    public String getServerId() {
        return serverId;
    }

    public String getLevelName() {
        return levelName;
    }

    public String getVersion() {
        return version;
    }

    public String getBuildDateString() {
        return buildDateString;
    }

    public List<LanguageView> getLanguages() {
        return languages;
    }

    public String getColor() {
        return color;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getBackgroundPicture() {
        return backgroundPicture;
    }

    public String getLogo() {
        return logo;
    }

    public static final class Builder {

        private String serverId;
        private String levelName;
        private String version;
        private String buildDateString;
        private List<LanguageView> languages;
        private String color; // todo check this with Jhon
        private String backgroundColor;
        private String backgroundPicture;
        private String logo;

        public Builder setLogo(String logo) {
            this.logo = logo;
            return this;
        }

        public Builder setServerId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder setLevelName(String levelName) {
            this.levelName = levelName;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setBuildDateString(String buildDateString) {
            this.buildDateString = buildDateString;
            return this;
        }

        public Builder setLanguages(List<LanguageView> languages) {
            this.languages = languages;
            return this;
        }

        public Builder setColor(String color) {
            this.color = color;
            return this;
        }

        public Builder setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setBackgroundPicture(String backgroundPicture) {
            this.backgroundPicture = backgroundPicture;
            return this;
        }

        public LoginFormResultView build() {
            return new LoginFormResultView(this);
        }
    }

} // E:O:F:LoginFormResultView.
