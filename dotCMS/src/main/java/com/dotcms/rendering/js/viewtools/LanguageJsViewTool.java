package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.proxy.JsLanguage;
import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.velocity.viewtools.LanguageViewtool;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.language.LanguageException;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps the LanguageViewtool (languagewebapi) into the JS context.
 * @author jsanca
 */
public class LanguageJsViewTool implements JsViewTool, JsViewContextAware {

    private  LanguageViewtool languageViewtool = new LanguageViewtool();

    @Override
    public void setViewContext(final ViewContext viewContext) {
        this.languageViewtool.init(viewContext);
    }

    @Override
    public String getName() {
        return "languagewebapi";
    }

    @HostAccess.Export
    /**
     * Get Language by language code and country code
     * @param languageCode
     * @param countryCode
     * @return JsLanguage
     */
    public JsLanguage getLanguage(final String languageCode, final String countryCode) {
        return new JsLanguage(LanguageViewtool.getLanguage(languageCode, countryCode));
    }

    @HostAccess.Export
    /**
     * Get Language by language code
     * @param langId
     * @return JsLanguage
     */
    public  JsLanguage getLanguage(final String langId) {
        return new JsLanguage(LanguageViewtool.getLanguage(langId));
    }

    @HostAccess.Export
    /**
     * Get the default language
     * @return JsLanguage
     */
    public JsLanguage getDefaultLanguage() {
        return new JsLanguage(LanguageViewtool.getDefaultLanguage());
    }

    @HostAccess.Export
    /**
     * Get the list of available languages
     * @return List<JsLanguage>
     */
    public  List<JsLanguage> getLanguages() {
        return LanguageViewtool.getLanguages().stream().map(JsLanguage::new).collect(Collectors.toList());
    }


    @HostAccess.Export
    /**
     * Return if the content can be use as a default to all languages.
     * It is a conjuntion of canDefaultContentToDefaultLanguage(),
     * wherever it is a form widget and canDefaultWidgetToDefaultLanguage()
     * if the content is a widget.
     * This method is intended to keep this rules in one place so it can
     * be used by DotResourceLoader and content service cache invalidator.
     * Also the contentlet must live in the default language.
     * @param cc
     * @return
     */
    // this should be proxied to work
    public boolean canApplyToAllLanguages(final Contentlet cc) {
        return LanguageViewtool.canApplyToAllLanguages(cc);
    }

    @HostAccess.Export
    /**
     * Update frontend language
     * @param langId
     */
    public void setLanguage(final String langId) {
        this.languageViewtool.setLanguage(langId);
    }

    @HostAccess.Export
    /**
     * Glosssary webapi
     */
    public String get(final String key) {

       return this.languageViewtool.get(key);
    }

    @HostAccess.Export
    public String get(final String key, final List args) {

        return this.languageViewtool.get(key, args);
    }

    @HostAccess.Export
    public String get(final String key, final String languageId) {

        return this.languageViewtool.get(key, languageId);
    }

    @HostAccess.Export
    public int getInt(final String key) {
        return this.languageViewtool.getInt(key);
    }

    @HostAccess.Export
    public int getInt(final String key, final String languageId) {

        return this.languageViewtool.getInt(key, languageId);
    }

    @HostAccess.Export
    public float getFloat(final String key) {
        return this.languageViewtool.getFloat(key);
    }

    @HostAccess.Export
    public float getFloat(final String key, final String languageId) {

        return this.languageViewtool.getFloat(key, languageId);
    }

    @HostAccess.Export
    public boolean getBoolean(final String key) {
        return this.languageViewtool.getBoolean(key);
    }

    @HostAccess.Export
    public boolean getBoolean(final String key, final String languageId) {

        return this.languageViewtool.getBoolean(key, languageId);
    }

    @HostAccess.Export
    public String getFromUserLanguage(final String key) {

        return this.languageViewtool.getFromUserLanguage(key);
    }

    @HostAccess.Export
    public String getFromSessionLanguage(final String key) throws LanguageException {

        return this.languageViewtool.getFromSessionLanguage(key);
    }
}
