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
    private final String backgroundColor;
    private final String backgroundPicture;
    private final String logo;
    private final String authorizationType;
    private final LanguageView currentLanguage;
    private final String companyEmail;

    private LoginFormResultView(LoginFormResultView.Builder builder) {
        serverId           = builder.serverId;
        levelName          = builder.levelName;
        version            = builder.version;
        buildDateString    = builder.buildDateString;
        languages          = builder.languages;
        backgroundColor    = builder.backgroundColor;
        backgroundPicture  = builder.backgroundPicture;
        logo               = builder.logo;
        authorizationType  = builder.authorizationType;
        currentLanguage    = builder.currentLanguage;
        companyEmail       = builder.companyEmail;
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

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getBackgroundPicture() {
        return backgroundPicture;
    }

    public String getLogo() {
        return logo;
    }

    public LanguageView getCurrentLanguage() {
        return currentLanguage;
    }

    public String getAuthorizationType() {
        return authorizationType;
    }
    
    public String getCompanyEmail() {
        return companyEmail;
    }

    public static final class Builder {

        private String serverId;
        private String levelName;
        private String version;
        private String buildDateString;
        private List<LanguageView> languages;
        private String backgroundColor;
        private String backgroundPicture;
        private String logo;
        private String authorizationType;
        private LanguageView currentLanguage;
        private String companyEmail;

        public Builder currentLanguage(LanguageView currentLanguage) {
            this.currentLanguage = currentLanguage;
            return this;
        }

        public Builder logo(String logo) {
            this.logo = logo;
            return this;
        }

        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder levelName(String levelName) {
            this.levelName = levelName;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder buildDateString(String buildDateString) {
            this.buildDateString = buildDateString;
            return this;
        }

        public Builder languages(List<LanguageView> languages) {
            this.languages = languages;
            return this;
        }

        public Builder backgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder backgroundPicture(String backgroundPicture) {
            this.backgroundPicture = backgroundPicture;
            return this;
        }

        public Builder authorizationType(String authorizationType) {
            this.authorizationType = authorizationType;
            return this;
        }
        
        public Builder companyEmail(String companyEmail) {
            this.companyEmail = companyEmail;
            return this;
        }

        public LoginFormResultView build() {
            return new LoginFormResultView(this);
        }
    }

} // E:O:F:LoginFormResultView.
