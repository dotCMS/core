package com.dotcms.rendering.js.proxy;

import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * This class is used to expose the Locale object to the javascript engine.
 * @author jsanca
 */
public class JsLocale implements Serializable, JsProxyObject<Locale> {

    private final Locale locale;

    public JsLocale(final Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getWrappedObject() {
        return locale;
    }

    @HostAccess.Export
    public String getLanguage() {
        return locale.getLanguage();
    }

    @HostAccess.Export
    public String getScript() {
        return locale.getScript();
    }

    @HostAccess.Export
    public String getCountry() {
        return locale.getCountry();
    }

    @HostAccess.Export
    public String getVariant() {
        return locale.getVariant();
    }

    @HostAccess.Export
    public boolean hasExtensions() {
        return locale.hasExtensions();
    }

    @HostAccess.Export
    public Object stripExtensions() {
        return JsProxyFactory.createProxy(locale.stripExtensions());
    }

    @HostAccess.Export
    public String getExtension(final char key) {
        return locale.getExtension(key);
    }

    @HostAccess.Export
    public Object getExtensionKeys() {
        return JsProxyFactory.createProxy(locale.getExtensionKeys());
    }

    @HostAccess.Export
    public Object getUnicodeLocaleAttributes() {
        return JsProxyFactory.createProxy(locale.getUnicodeLocaleAttributes());
    }

    @HostAccess.Export
    public String getUnicodeLocaleType(final String key) {
        return locale.getUnicodeLocaleType(key);
    }

    @HostAccess.Export
    public Object getUnicodeLocaleKeys() {
        return JsProxyFactory.createProxy(locale.getUnicodeLocaleKeys());
    }

    @HostAccess.Export
    public String toLanguageTag() {
        return locale.toLanguageTag();
    }

    @HostAccess.Export
    public String getISO3Language() throws MissingResourceException {
        return locale.getISO3Language();
    }

    @HostAccess.Export
    public String getISO3Country() throws MissingResourceException {
        return locale.getISO3Country();
    }

    @HostAccess.Export
    public final String getDisplayLanguage() {
        return locale.getDisplayLanguage();
    }

    @HostAccess.Export
    public String getDisplayLanguage(final JsLocale inLocale) {
        return locale.getDisplayLanguage(inLocale.getWrappedObject());
    }

    @HostAccess.Export
    public String getDisplayScript() {
        return locale.getDisplayScript();
    }

    @HostAccess.Export
    public String getDisplayScript(final JsLocale inLocale) {
        return locale.getDisplayScript(inLocale.getWrappedObject());
    }

    @HostAccess.Export
    public final String getDisplayCountry() {
        return locale.getDisplayCountry();
    }

    @HostAccess.Export
    public String getDisplayCountry(final JsLocale inLocale) {
        return locale.getDisplayCountry(inLocale.getWrappedObject());
    }

    @HostAccess.Export
    public final String getDisplayVariant() {
        return locale.getDisplayVariant();
    }

    @HostAccess.Export
    public String getDisplayVariant(final JsLocale inLocale) {
        return locale.getDisplayVariant(inLocale.getWrappedObject());
    }

    @HostAccess.Export
    public final String getDisplayName() {
        return locale.getDisplayName();
    }

    @HostAccess.Export
    public String getDisplayName(JsLocale inLocale) {
        return locale.getDisplayName(inLocale.getWrappedObject());
    }
}
