package com.dotcms.rendering.js.proxy;

import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Locale;

/**
 * This class is used to expose the Language object to the javascript engine.
 * @author jsanca
 */
public class JsLanguage implements Serializable, JsProxyObject<Language> {

    private final Language language;

    /**
     * Returns the actual language object, but can not be retrieved on the JS context.
     * @return
     */
    public Language getLanguageObject () {
        return language;
    }

    @Override
    public Language  getWrappedObject() {
        return this.getLanguageObject();
    }

    public JsLanguage(final Language language) {
        this.language = language;
    }

    @HostAccess.Export
    public String getCountry() {
        return this.language.getCountry();
    }

    @HostAccess.Export
    @JsonIgnore
    public Locale asLocale() {
        return this.language.asLocale();
    }

    @HostAccess.Export
    /**
     * @return Returns the countryCode.
     */
    public String getCountryCode() {
        return this.language.getCountryCode();
    }

    @HostAccess.Export
    /**
     * @return Returns the language.
     */
    public String getLanguage() {
        return this.language.getLanguage();
    }

    @HostAccess.Export
    /**
     * @return Returns the languageCode.
     */
    public String getLanguageCode() {
        return this.language.getLanguageCode();
    }

    @HostAccess.Export
    public int hashCode() {
        return this.language.hashCode();
    }

    @HostAccess.Export
    /**
     * @return Returns the id.
     */
    public long getId() {
        return this.language.getId();
    }

    @HostAccess.Export
    public boolean equals(Object other) {
        if (!(other instanceof JsLanguage)) {
            return false;
        }

        JsLanguage castOther = (JsLanguage) other;

        return this.language.equals(castOther.language);
    }

    @HostAccess.Export
    public String getIsoCode() {
        return this.language.getIsoCode();
    }

    @HostAccess.Export
    @Override
    public String toString() {
        return this.language.toString();
    }

    @JsonIgnore
    protected ManifestItem.ManifestInfo getManifestInfoInternal(){
        return this.language.getManifestInfo();
    }

    @HostAccess.Export
    @JsonIgnore
    public Object getManifestInfo(){
        return JsProxyFactory.createProxy(this.getManifestInfoInternal());
    }
}
